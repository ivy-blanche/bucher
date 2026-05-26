package com.bucher.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import com.bucher.exam.dto.AnswerSaveDTO;
import com.bucher.exam.dto.ExamDraftSaveDTO;
import com.bucher.exam.dto.ExamListQueryDTO;
import com.bucher.exam.dto.ExamPublishDTO;
import com.bucher.exam.dto.ExamSubmitDTO;
import com.bucher.exam.entity.Exam;
import com.bucher.exam.entity.ExamAnswer;
import com.bucher.exam.entity.ExamClass;
import com.bucher.exam.entity.ExamQuestion;
import com.bucher.exam.entity.ExamSubmission;
import com.bucher.exam.enums.ExamStatusEnum;
import com.bucher.exam.event.ExamPublishedEvent;
import com.bucher.exam.mapper.ExamAnswerMapper;
import com.bucher.exam.mapper.ExamClassMapper;
import com.bucher.exam.mapper.ExamMapper;
import com.bucher.exam.mapper.ExamQuestionMapper;
import com.bucher.exam.mapper.ExamSubmissionMapper;
import com.bucher.exam.service.ExamService;
import com.bucher.exam.vo.ExamDoVO;
import com.bucher.exam.vo.ExamListVO;
import com.bucher.exam.vo.ExamUnpublishedVO;
import com.bucher.exam.vo.QuestionDetailVO;
import com.bucher.exam.vo.StudentExamVO;
import com.bucher.exam.feign.CourseServiceClient;
import com.bucher.exam.feign.QuestionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 考试 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private static final String REDIS_KEY_PREFIX = "exam:answers:";
    private static final long REDIS_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 天

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamClassMapper examClassMapper;
    private final ExamSubmissionMapper examSubmissionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final RabbitTemplate rabbitTemplate;
    private final CourseServiceClient courseServiceClient;
    private final QuestionServiceClient questionServiceClient;
    private final RedissonClient redissonClient;

    @Override
    public IPage<ExamListVO> getTeacherExamList(ExamListQueryDTO dto) {
        Page<ExamListVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ExamListVO> result = examMapper.selectTeacherExamList(page, dto.getTeacherId(), dto.getFilterMode());

        // 填充每个考试的已提交人数
        for (ExamListVO vo : result.getRecords()) {
            Integer count = examMapper.countSubmitByExamId(vo.getId());
            vo.setSubmitCount(count != null ? count : 0);
        }

        return result;
    }

    @Override
    public IPage<ExamUnpublishedVO> getUnpublishedExamList(Long teacherId, Integer pageNum, Integer pageSize) {
        Page<Exam> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Exam> wrapper = Wrappers.lambdaQuery(Exam.class)
                .eq(Exam::getTeacherId, teacherId)
                .eq(Exam::getStatus, 0)
                .eq(Exam::getIsDeleted, 0)
                .orderByDesc(Exam::getCreateTime);

        return examMapper.selectPage(page, wrapper).convert(h -> {
            ExamUnpublishedVO vo = new ExamUnpublishedVO();
            vo.setId(h.getId());
            vo.setTitle(h.getTitle());
            vo.setCourseName(h.getCourseName());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(ExamDraftSaveDTO dto, Long teacherId, Integer role) {
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        int totalScore = dto.getQuestions().stream()
                .mapToInt(ExamDraftSaveDTO.QuestionItem::getScore)
                .sum();

        Exam exam = new Exam();
        exam.setCourseId(dto.getCourseId());
        exam.setTeacherId(teacherId);
        exam.setTitle(dto.getTitle());
        exam.setComposeType(1);
        exam.setGroupId(dto.getSourceBankId());
        exam.setTotalScore(totalScore);
        exam.setStatus(ExamStatusEnum.UNPUBLISHED.getCode());
        exam.setDuration(0);
        examMapper.insert(exam);

        List<ExamQuestion> questions = new ArrayList<>();
        for (int i = 0; i < dto.getQuestions().size(); i++) {
            ExamDraftSaveDTO.QuestionItem item = dto.getQuestions().get(i);
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(exam.getId());
            eq.setQuestionId(item.getQuestionId());
            eq.setScore(item.getScore());
            eq.setSortOrder(i);
            questions.add(eq);
        }
        for (ExamQuestion eq : questions) {
            examQuestionMapper.insert(eq);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(ExamPublishDTO dto, Long teacherId, Integer role) {
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        // 校验开始时间必须在当前时间之后
        if (dto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("开始时间必须在当前时间之后");
        }

        int totalScore = dto.getQuestions().stream()
                .mapToInt(ExamDraftSaveDTO.QuestionItem::getScore)
                .sum();

        LocalDateTime endTime = dto.getStartTime().plusMinutes(dto.getDuration());

        Exam exam = new Exam();
        exam.setCourseId(dto.getCourseId());
        exam.setTeacherId(teacherId);
        exam.setTitle(dto.getTitle());
        exam.setComposeType(1);
        exam.setGroupId(dto.getSourceBankId());
        exam.setTotalScore(totalScore);
        exam.setPassScore(dto.getPassScore());
        exam.setDuration(dto.getDuration());
        exam.setEarlySubmitMinutes(dto.getEarlySubmitMinutes());
        exam.setLateBanMinutes(dto.getLateBanMinutes() != null ? dto.getLateBanMinutes() : 15);
        exam.setAutoSubmit(dto.getAutoSubmit() != null ? dto.getAutoSubmit() : 1);
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(endTime);
        exam.setDescription(dto.getDescription());
        exam.setStatus(ExamStatusEnum.IN_PROGRESS.getCode());
        examMapper.insert(exam);

        for (int i = 0; i < dto.getQuestions().size(); i++) {
            ExamDraftSaveDTO.QuestionItem item = dto.getQuestions().get(i);
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(exam.getId());
            eq.setQuestionId(item.getQuestionId());
            eq.setScore(item.getScore());
            eq.setSortOrder(i);
            examQuestionMapper.insert(eq);
        }

        for (Long classId : dto.getClassIds()) {
            ExamClass ec = new ExamClass();
            ec.setExamId(exam.getId());
            ec.setCourseClassId(classId);
            examClassMapper.insert(ec);
        }

        // 事务提交后发送 RabbitMQ 通知事件
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ExamPublishedEvent event = new ExamPublishedEvent(
                            exam.getId(),
                            exam.getCourseId(),
                            exam.getTeacherId(),
                            dto.getClassIds(),
                            exam.getTitle(),
                            exam.getStartTime(),
                            exam.getEndTime()
                    );
                    try {
                        rabbitTemplate.convertAndSend("notification.exchange",
                                "notification.exam", event);
                        log.info("已发送考试发布事件: examId={}", exam.getId());
                    } catch (Exception e) {
                        log.error("发送考试发布通知失败, 不影响考试创建: examId={}", exam.getId(), e);
                    }
                }
            });
        }
    }

    @Override
    public List<StudentExamVO> getStudentExamList(Long studentId, Long courseId) {
        List<Long> classIds = Optional.ofNullable(courseServiceClient.getStudentCourseClassIds(studentId, courseId))
                .map(Result::getData)
                .orElse(Collections.emptyList());

        if (classIds.isEmpty()) {
            return Collections.emptyList();
        }

        return examMapper.selectStudentExamList(courseId, studentId, classIds);
    }

    @Override
    public ExamDoVO getExamDetail(Long studentId, Long examId) {
        // 1. 查询考试基本信息
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(ResultCodeEnum.EXAM_NOT_FOUND);
        }
        if (!ExamStatusEnum.IN_PROGRESS.getCode().equals(exam.getStatus())) {
            throw new BusinessException("考试暂未发布或已结束");
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 校验考试是否已经开始
        if (now.isBefore(exam.getStartTime())) {
            throw new BusinessException(ResultCodeEnum.EXAM_NOT_STARTED);
        }

        // 3. 校验考试是否已结束
        if (now.isAfter(exam.getEndTime())) {
            throw new BusinessException(ResultCodeEnum.EXAM_FINISHED);
        }

        // 4. 校验是否禁止进入（开考后 lateBanMinutes 分钟禁止进入）
        int banMinutes = exam.getLateBanMinutes() != null ? exam.getLateBanMinutes() : 15;
        LocalDateTime banDeadline = exam.getStartTime().plusMinutes(banMinutes);
        if (now.isAfter(banDeadline)) {
            // 检查是否已经提交过（已提交的仍可查看，不允许修改）
            ExamSubmission submission = examSubmissionMapper.selectOne(
                    Wrappers.lambdaQuery(ExamSubmission.class)
                            .eq(ExamSubmission::getExamId, examId)
                            .eq(ExamSubmission::getStudentId, studentId));
            boolean submitted = submission != null && submission.getSubmitTime() != null;

            if (!submitted) {
                throw new BusinessException("考试已开始" + banMinutes + "分钟，禁止进入");
            }
        }

        // 5. 校验学生是否有权限参加考试
        checkStudentAccess(studentId, exam);

        // 6. 查询考试题目关联
        List<ExamQuestion> eqList = examQuestionMapper.selectList(
                Wrappers.lambdaQuery(ExamQuestion.class)
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSortOrder));

        if (eqList.isEmpty()) {
            ExamDoVO vo = buildEmptyExamDoVO(exam);
            vo.setSubmitted(checkSubmitted(studentId, examId));
            return vo;
        }

        // 7. 批量查询题目详情
        List<Long> questionIds = eqList.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toList());
        List<QuestionDetailVO> details = fetchQuestionDetails(questionIds);
        Map<Long, QuestionDetailVO> detailMap = details.stream()
                .collect(Collectors.toMap(QuestionDetailVO::getId, d -> d));

        // 8. 检查是否已提交
        boolean submitted = checkSubmitted(studentId, examId);

        // 9. 获取已作答答案
        Map<String, String> redisAnswers = getRedisAnswers(examId, studentId);
        List<ExamAnswer> submittedAnswers = null;
        if (redisAnswers.isEmpty() && submitted) {
            submittedAnswers = examAnswerMapper.selectList(
                    Wrappers.lambdaQuery(ExamAnswer.class)
                            .eq(ExamAnswer::getExamId, examId)
                            .eq(ExamAnswer::getStudentId, studentId));
        }

        // 10. 组装 VO
        List<ExamDoVO.QuestionVO> questionVOs = new ArrayList<>();
        for (ExamQuestion eq : eqList) {
            QuestionDetailVO detail = detailMap.get(eq.getQuestionId());
            if (detail == null) {
                continue;
            }

            ExamDoVO.QuestionVO qvo = new ExamDoVO.QuestionVO();
            qvo.setQuestionId(detail.getId());
            qvo.setQuestionType(toQuestionTypeValue(detail.getType()));
            qvo.setContent(detail.getContent());
            qvo.setScore(eq.getScore());
            qvo.setSortOrder(eq.getSortOrder());

            String cachedAnswer = redisAnswers.get(String.valueOf(detail.getId()));
            if (cachedAnswer != null) {
                qvo.setAnswer(cachedAnswer);
            } else if (submittedAnswers != null) {
                submittedAnswers.stream()
                        .filter(a -> a.getQuestionId().equals(detail.getId()))
                        .findFirst()
                        .ifPresent(a -> qvo.setAnswer(a.getAnswer()));
            }

            if (detail.getOptions() != null && !detail.getOptions().isEmpty()) {
                List<ExamDoVO.OptionVO> optionVOs = detail.getOptions().stream()
                        .map(opt -> {
                            ExamDoVO.OptionVO ovo = new ExamDoVO.OptionVO();
                            ovo.setLabel(opt.getLabel());
                            ovo.setContent(opt.getContent());
                            return ovo;
                        })
                        .collect(Collectors.toList());
                qvo.setOptions(optionVOs);
            }

            // 编程题
            if (Integer.valueOf(5).equals(detail.getType())) {
                if (detail.getProgrammingConfig() != null) {
                    qvo.setTemplateCode(detail.getProgrammingConfig().getTemplateCode());
                }
                if (detail.getTestCases() != null) {
                    List<ExamDoVO.TestCaseVO> testCaseVOs = detail.getTestCases().stream()
                            .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                            .map(tc -> {
                                ExamDoVO.TestCaseVO tvo = new ExamDoVO.TestCaseVO();
                                tvo.setInput(tc.getInput());
                                tvo.setExpectedOutput(tc.getExpectedOutput());
                                return tvo;
                            })
                            .collect(Collectors.toList());
                    qvo.setTestCases(testCaseVOs);
                }
            }

            questionVOs.add(qvo);
        }

        // 11. 计算剩余时间
        long remainingSeconds = Duration.between(now, exam.getEndTime()).getSeconds();

        // 12. 返回
        ExamDoVO vo = new ExamDoVO();
        vo.setExamId(examId);
        vo.setTitle(exam.getTitle());
        vo.setDescription(exam.getDescription());
        vo.setCourseName(exam.getCourseName());
        vo.setTotalScore(exam.getTotalScore());
        vo.setDuration(exam.getDuration());
        vo.setEarlySubmitMinutes(exam.getEarlySubmitMinutes());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setStatus(exam.getStatus());
        vo.setSubmitted(submitted);
        vo.setRemainingSeconds(Math.max(0, remainingSeconds));
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    public void saveAnswer(Long studentId, AnswerSaveDTO dto) {
        Exam exam = examMapper.selectById(dto.getExamId());
        if (exam == null) {
            throw new BusinessException(ResultCodeEnum.EXAM_NOT_FOUND);
        }
        if (!ExamStatusEnum.IN_PROGRESS.getCode().equals(exam.getStatus())) {
            throw new BusinessException("考试未在进行中，无法作答");
        }

        LocalDateTime now = LocalDateTime.now();

        // 校验考试是否开始
        if (now.isBefore(exam.getStartTime())) {
            throw new BusinessException(ResultCodeEnum.EXAM_NOT_STARTED);
        }

        // 校验考试是否已结束
        if (now.isAfter(exam.getEndTime())) {
            throw new BusinessException(ResultCodeEnum.EXAM_FINISHED);
        }

        // 检查是否已提交
        ExamSubmission submission = examSubmissionMapper.selectOne(
                Wrappers.lambdaQuery(ExamSubmission.class)
                        .eq(ExamSubmission::getExamId, dto.getExamId())
                        .eq(ExamSubmission::getStudentId, studentId));
        if (submission != null && submission.getSubmitTime() != null) {
            throw new BusinessException("考试已提交，无法修改");
        }

        // 写入 Redis Hash
        String redisKey = redisKey(dto.getExamId(), studentId);
        RMap<String, String> map = redissonClient.getMap(redisKey);
        map.put(String.valueOf(dto.getQuestionId()), dto.getAnswer() != null ? dto.getAnswer() : "");
        map.expire(REDIS_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitExam(Long studentId, ExamSubmitDTO dto) {
        Long examId = dto.getExamId();

        // 1. 校验考试
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(ResultCodeEnum.EXAM_NOT_FOUND);
        }
        if (!ExamStatusEnum.IN_PROGRESS.getCode().equals(exam.getStatus())) {
            throw new BusinessException("考试未在进行中，无法提交");
        }

        LocalDateTime now = LocalDateTime.now();

        // 2. 校验考试是否开始
        if (now.isBefore(exam.getStartTime())) {
            throw new BusinessException(ResultCodeEnum.EXAM_NOT_STARTED);
        }

        // 3. 校验最早交卷时间
        int earlySubmitMin = exam.getEarlySubmitMinutes() != null ? exam.getEarlySubmitMinutes() : 0;
        if (earlySubmitMin > 0) {
            LocalDateTime earliestSubmitTime = exam.getStartTime().plusMinutes(earlySubmitMin);
            if (now.isBefore(earliestSubmitTime)) {
                throw new BusinessException("考试开始" + earlySubmitMin + "分钟后才能交卷");
            }
        }

        // 4. 校验可提交（重复提交）
        ExamSubmission existingSubmission = examSubmissionMapper.selectOne(
                Wrappers.lambdaQuery(ExamSubmission.class)
                        .eq(ExamSubmission::getExamId, examId)
                        .eq(ExamSubmission::getStudentId, studentId));
        if (existingSubmission != null && existingSubmission.getSubmitTime() != null) {
            throw new BusinessException("考试已提交，请勿重复提交");
        }

        // 5. 校验学生权限
        checkStudentAccess(studentId, exam);

        // 6. 获取 Redis 中所有暂存答案
        String redisKey = redisKey(examId, studentId);
        RMap<String, String> redisMap = redissonClient.getMap(redisKey);
        Map<String, String> redisEntries = redisMap.readAllMap();

        if (redisEntries.isEmpty()) {
            throw new BusinessException("暂无作答记录，请先作答后再提交");
        }

        // 7. 查询题目详情（用于自动评分）
        List<ExamQuestion> eqList = examQuestionMapper.selectList(
                Wrappers.lambdaQuery(ExamQuestion.class)
                        .eq(ExamQuestion::getExamId, examId));
        List<Long> questionIds = eqList.stream()
                .map(ExamQuestion::getQuestionId)
                .collect(Collectors.toList());
        List<QuestionDetailVO> details = fetchQuestionDetails(questionIds);
        Map<Long, QuestionDetailVO> detailMap = details.stream()
                .collect(Collectors.toMap(QuestionDetailVO::getId, d -> d));
        Map<Long, Integer> questionScoreMap = eqList.stream()
                .collect(Collectors.toMap(ExamQuestion::getQuestionId, ExamQuestion::getScore));

        // 8. 构建 ExamAnswer 并自动评分
        int totalScore = 0;
        int autoGradedCount = 0;
        int correctCount = 0;
        List<ExamAnswer> answers = new ArrayList<>();

        for (Map.Entry<String, String> entry : redisEntries.entrySet()) {
            Long questionId = Long.valueOf(entry.getKey());
            String studentAnswer = entry.getValue() != null ? entry.getValue() : "";

            ExamAnswer answer = new ExamAnswer();
            answer.setExamId(examId);
            answer.setStudentId(studentId);
            answer.setQuestionId(questionId);
            answer.setAnswer(studentAnswer);

            QuestionDetailVO detail = detailMap.get(questionId);
            if (detail != null) {
                Integer score = autoGrade(detail.getType(), studentAnswer, detail.getAnswer());
                if (score != null) {
                    int questionScore = questionScoreMap.getOrDefault(questionId, 0);
                    if (Objects.equals(score, 1)) {
                        answer.setScore(questionScore);
                        answer.setIsCorrect(1);
                        totalScore += questionScore;
                        correctCount++;
                    } else if (Objects.equals(score, 0)) {
                        answer.setScore(0);
                        answer.setIsCorrect(0);
                    }
                    autoGradedCount++;
                }
            }

            answers.add(answer);
        }

        // 9. 批量插入/更新 exam_answer
        for (ExamAnswer answer : answers) {
            ExamAnswer existing = examAnswerMapper.selectOne(
                    Wrappers.lambdaQuery(ExamAnswer.class)
                            .eq(ExamAnswer::getExamId, examId)
                            .eq(ExamAnswer::getStudentId, studentId)
                            .eq(ExamAnswer::getQuestionId, answer.getQuestionId()));
            if (existing != null) {
                existing.setAnswer(answer.getAnswer());
                if (answer.getScore() != null) {
                    existing.setScore(answer.getScore());
                    existing.setIsCorrect(answer.getIsCorrect());
                }
                examAnswerMapper.updateById(existing);
            } else {
                examAnswerMapper.insert(answer);
            }
        }

        // 10. 创建/更新 exam_submission
        if (existingSubmission != null) {
            existingSubmission.setScore(totalScore);
            existingSubmission.setSubmitTime(LocalDateTime.now());
            existingSubmission.setGradeStatus(determineGradeStatus(answers));
            existingSubmission.setStatus(1);
            examSubmissionMapper.updateById(existingSubmission);
        } else {
            ExamSubmission submission = new ExamSubmission();
            submission.setExamId(examId);
            submission.setStudentId(studentId);
            submission.setScore(totalScore);
            submission.setSubmitTime(LocalDateTime.now());
            submission.setGradeStatus(determineGradeStatus(answers));
            submission.setStatus(1);
            examSubmissionMapper.insert(submission);
        }

        // 11. 如果有主观题需要批改
        boolean hasSubjective = answers.stream().anyMatch(a -> a.getIsCorrect() == null && a.getScore() == null);
        if (hasSubjective) {
            exam.setGradingStatus(1);
            examMapper.updateById(exam);
        }

        // 12. 清理 Redis 缓存
        redisMap.delete();

        log.info("学生提交考试: studentId={}, examId={}, 总分={}, 自动批改={}题, 正确={}题",
                studentId, examId, totalScore, autoGradedCount, correctCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoSubmitExpiredExams() {
        LocalDateTime now = LocalDateTime.now();

        // 查找所有进行中且已过结束时间的考试
        List<Exam> expiredExams = examMapper.selectList(
                Wrappers.lambdaQuery(Exam.class)
                        .eq(Exam::getStatus, ExamStatusEnum.IN_PROGRESS.getCode())
                        .eq(Exam::getAutoSubmit, 1)
                        .eq(Exam::getIsDeleted, 0)
                        .lt(Exam::getEndTime, now));

        for (Exam exam : expiredExams) {
            // 查找该考试下所有未提交的学生
            List<Long> allStudentIds = examSubmissionMapper.selectList(
                    Wrappers.lambdaQuery(ExamSubmission.class)
                            .eq(ExamSubmission::getExamId, exam.getId())
                            .eq(ExamSubmission::getIsDeleted, 0))
                    .stream()
                    .filter(s -> s.getSubmitTime() == null)
                    .map(ExamSubmission::getStudentId)
                    .collect(Collectors.toList());

            // 如果没有 submission 记录但有 redis 缓存的学生
            List<ExamSubmission> submissions = new ArrayList<>();

            for (Long studentId : allStudentIds) {
                String redisKey = redisKey(exam.getId(), studentId);
                RMap<String, String> redisMap = redissonClient.getMap(redisKey);
                Map<String, String> redisEntries = redisMap.readAllMap();

                if (redisEntries.isEmpty()) {
                    continue;
                }

                // 执行强制提交
                try {
                    ExamSubmitDTO submitDTO = new ExamSubmitDTO();
                    submitDTO.setExamId(exam.getId());
                    submitExam(studentId, submitDTO);
                    log.info("超时自动提交: examId={}, studentId={}", exam.getId(), studentId);
                } catch (Exception e) {
                    log.error("超时自动提交失败: examId={}, studentId={}", exam.getId(), studentId, e);
                }
            }

            // 更新考试状态为已结束
            exam.setStatus(ExamStatusEnum.ENDED.getCode());
            examMapper.updateById(exam);
        }
    }

    // ==================== 私有方法 ====================

    private String redisKey(Long examId, Long studentId) {
        return REDIS_KEY_PREFIX + examId + ":" + studentId;
    }

    private Map<String, String> getRedisAnswers(Long examId, Long studentId) {
        String redisKey = redisKey(examId, studentId);
        RMap<String, String> map = redissonClient.getMap(redisKey);
        return new HashMap<>(map.readAllMap());
    }

    private boolean checkSubmitted(Long studentId, Long examId) {
        ExamSubmission submission = examSubmissionMapper.selectOne(
                Wrappers.lambdaQuery(ExamSubmission.class)
                        .eq(ExamSubmission::getExamId, examId)
                        .eq(ExamSubmission::getStudentId, studentId));
        return submission != null && submission.getSubmitTime() != null;
    }

    private void checkStudentAccess(Long studentId, Exam exam) {
        List<Long> classIds = Optional.ofNullable(
                        courseServiceClient.getStudentCourseClassIds(studentId, exam.getCourseId()))
                .map(Result::getData)
                .orElse(Collections.emptyList());

        if (classIds.isEmpty()) {
            throw new BusinessException("您不在该课程的任何教学班中");
        }

        Long count = examClassMapper.selectCount(
                Wrappers.lambdaQuery(ExamClass.class)
                        .eq(ExamClass::getExamId, exam.getId())
                        .in(ExamClass::getCourseClassId, classIds));

        if (count == null || count == 0) {
            throw new BusinessException("您没有权限参加该考试");
        }
    }

    private List<QuestionDetailVO> fetchQuestionDetails(List<Long> questionIds) {
        try {
            Result<List<QuestionDetailVO>> result = questionServiceClient.getBatchDetail(questionIds);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            log.warn("批量查询题目详情失败: {}", result != null ? result.getMessage() : "result is null");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Feign 调用 question-service 批量查询题目详情失败", e);
            throw new BusinessException("获取题目信息失败");
        }
    }

    private Integer autoGrade(Integer questionType, String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || studentAnswer.isEmpty() || correctAnswer == null || correctAnswer.isEmpty()) {
            return null;
        }

        if (questionType == 1) {
            return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim()) ? 1 : 0;
        } else if (questionType == 2) {
            String sortedStudent = sortAnswer(studentAnswer);
            String sortedCorrect = sortAnswer(correctAnswer);
            return sortedStudent.equals(sortedCorrect) ? 1 : 0;
        }
        return null;
    }

    private String sortAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        char[] chars = answer.toUpperCase().replaceAll("[^A-Z]", "").toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    private Integer determineGradeStatus(List<ExamAnswer> answers) {
        boolean hasUngraded = answers.stream().anyMatch(a -> a.getIsCorrect() == null);
        return hasUngraded ? 1 : 0;
    }

    private String toQuestionTypeValue(Integer type) {
        if (type == null) return "unknown";
        return switch (type) {
            case 1 -> "single";
            case 2 -> "multiple";
            case 3 -> "fill";
            case 4 -> "short-answer";
            case 5 -> "programming";
            default -> "unknown";
        };
    }

    private ExamDoVO buildEmptyExamDoVO(Exam exam) {
        ExamDoVO vo = new ExamDoVO();
        vo.setExamId(exam.getId());
        vo.setTitle(exam.getTitle());
        vo.setDescription(exam.getDescription());
        vo.setCourseName(exam.getCourseName());
        vo.setTotalScore(0);
        vo.setDuration(exam.getDuration());
        vo.setEarlySubmitMinutes(exam.getEarlySubmitMinutes());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setStatus(exam.getStatus());
        vo.setRemainingSeconds(0L);
        vo.setQuestions(Collections.emptyList());
        return vo;
    }
}
