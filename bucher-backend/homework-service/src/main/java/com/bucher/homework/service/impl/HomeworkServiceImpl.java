package com.bucher.homework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import com.bucher.homework.dto.AnswerSaveDTO;
import com.bucher.homework.dto.HomeworkDraftSaveDTO;
import com.bucher.homework.dto.HomeworkListQueryDTO;
import com.bucher.homework.dto.HomeworkPublishDTO;
import com.bucher.homework.dto.HomeworkSubmitDTO;
import com.bucher.homework.entity.Homework;
import com.bucher.homework.entity.HomeworkAnswer;
import com.bucher.homework.entity.HomeworkClass;
import com.bucher.homework.entity.HomeworkQuestion;
import com.bucher.homework.entity.HomeworkSubmission;
import com.bucher.homework.enums.HomeworkFilterEnum;
import com.bucher.homework.enums.HomeworkStatusEnum;
import com.bucher.homework.enums.QuestionTypeEnum;
import com.bucher.homework.event.HomeworkPublishedEvent;
import com.bucher.homework.mapper.HomeworkAnswerMapper;
import com.bucher.homework.mapper.HomeworkClassMapper;
import com.bucher.homework.mapper.HomeworkMapper;
import com.bucher.homework.mapper.HomeworkQuestionMapper;
import com.bucher.homework.mapper.HomeworkSubmissionMapper;
import com.bucher.homework.service.HomeworkService;
import com.bucher.homework.vo.HomeworkDoVO;
import com.bucher.homework.vo.HomeworkListVO;
import com.bucher.homework.vo.HomeworkUnpublishedVO;
import com.bucher.homework.vo.QuestionDetailVO;
import com.bucher.homework.vo.StudentHomeworkVO;
import com.bucher.homework.feign.CourseServiceClient;
import com.bucher.homework.feign.QuestionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
 * 作业 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkServiceImpl implements HomeworkService {

    private static final String REDIS_KEY_PREFIX = "homework:answers:";
    private static final long REDIS_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 天

    private final HomeworkMapper homeworkMapper;
    private final HomeworkQuestionMapper homeworkQuestionMapper;
    private final HomeworkClassMapper homeworkClassMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final HomeworkAnswerMapper homeworkAnswerMapper;
    private final RabbitTemplate rabbitTemplate;
    private final CourseServiceClient courseServiceClient;
    private final QuestionServiceClient questionServiceClient;
    private final RedissonClient redissonClient;

    @Override
    public IPage<HomeworkListVO> getTeacherHomeworkList(HomeworkListQueryDTO dto) {
        HomeworkFilterEnum filterEnum = HomeworkFilterEnum.of(dto.getFilterMode());
        String filter = filterEnum == HomeworkFilterEnum.ALL ? null : filterEnum.getCode();

        Page<HomeworkListVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return homeworkMapper.selectTeacherHomeworkList(page, dto.getTeacherId(), filter);
    }

    @Override
    public IPage<HomeworkUnpublishedVO> getUnpublishedHomeworkList(Long teacherId, Integer pageNum, Integer pageSize) {
        Page<Homework> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Homework> wrapper = Wrappers.lambdaQuery(Homework.class)
                .eq(Homework::getTeacherId, teacherId)
                .eq(Homework::getStatus, 0)
                .eq(Homework::getIsDeleted, 0)
                .orderByDesc(Homework::getCreateTime);

        return homeworkMapper.selectPage(page, wrapper).convert(h -> {
            HomeworkUnpublishedVO vo = new HomeworkUnpublishedVO();
            vo.setId(h.getId());
            vo.setTitle(h.getTitle());
            vo.setCourseName(h.getCourseName());
            vo.setDeadline(h.getDeadline());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(HomeworkDraftSaveDTO dto, Long teacherId, Integer role) {
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        int totalScore = dto.getQuestions().stream()
                .mapToInt(HomeworkDraftSaveDTO.QuestionItem::getScore)
                .sum();

        Homework homework = new Homework();
        homework.setCourseId(dto.getCourseId());
        homework.setTeacherId(teacherId);
        homework.setTitle(dto.getTitle());
        homework.setComposeType(1);
        homework.setGroupId(dto.getSourceBankId());
        homework.setTotalScore(totalScore);
        homework.setStatus(HomeworkStatusEnum.UNPUBLISHED.getCode());
        homeworkMapper.insert(homework);

        List<HomeworkQuestion> questions = new ArrayList<>();
        for (int i = 0; i < dto.getQuestions().size(); i++) {
            HomeworkDraftSaveDTO.QuestionItem item = dto.getQuestions().get(i);
            HomeworkQuestion hq = new HomeworkQuestion();
            hq.setHomeworkId(homework.getId());
            hq.setQuestionId(item.getQuestionId());
            hq.setScore(item.getScore());
            hq.setSortOrder(i);
            questions.add(hq);
        }
        for (HomeworkQuestion hq : questions) {
            homeworkQuestionMapper.insert(hq);
        }
    }

    @Override
    public List<StudentHomeworkVO> getStudentHomeworkList(Long studentId, Long courseId) {
        List<Long> classIds = Optional.ofNullable(courseServiceClient.getStudentCourseClassIds(studentId, courseId))
                .map(Result::getData)
                .orElse(Collections.emptyList());

        if (classIds.isEmpty()) {
            return Collections.emptyList();
        }

        return homeworkMapper.selectStudentHomeworkList(courseId, studentId, classIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(HomeworkPublishDTO dto, Long teacherId, Integer role) {
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        int totalScore = dto.getQuestions().stream()
                .mapToInt(HomeworkDraftSaveDTO.QuestionItem::getScore)
                .sum();

        Homework homework = new Homework();
        homework.setCourseId(dto.getCourseId());
        homework.setTeacherId(teacherId);
        homework.setTitle(dto.getTitle());
        homework.setComposeType(1);
        homework.setGroupId(dto.getSourceBankId());
        homework.setTotalScore(totalScore);
        homework.setDeadline(dto.getDeadline());
        homework.setStatus(HomeworkStatusEnum.IN_PROGRESS.getCode());
        homeworkMapper.insert(homework);

        for (int i = 0; i < dto.getQuestions().size(); i++) {
            HomeworkDraftSaveDTO.QuestionItem item = dto.getQuestions().get(i);
            HomeworkQuestion hq = new HomeworkQuestion();
            hq.setHomeworkId(homework.getId());
            hq.setQuestionId(item.getQuestionId());
            hq.setScore(item.getScore());
            hq.setSortOrder(i);
            homeworkQuestionMapper.insert(hq);
        }

        for (Long classId : dto.getClassIds()) {
            HomeworkClass hc = new HomeworkClass();
            hc.setHomeworkId(homework.getId());
            hc.setCourseClassId(classId);
            homeworkClassMapper.insert(hc);
        }

        // 事务提交后发送 RabbitMQ 通知事件
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    HomeworkPublishedEvent event = new HomeworkPublishedEvent(
                            homework.getId(),
                            homework.getCourseId(),
                            homework.getTeacherId(),
                            dto.getClassIds(),
                            homework.getTitle(),
                            homework.getDeadline()
                    );
                    try {
                        rabbitTemplate.convertAndSend("notification.exchange",
                                "notification.homework", event);
                        log.info("已发送作业发布事件: homeworkId={}", homework.getId());
                    } catch (Exception e) {
                        log.error("发送作业发布通知失败, 不影响作业创建: homeworkId={}", homework.getId(), e);
                    }
                }
            });
        }
    }

    @Override
    public HomeworkDoVO getHomeworkDetail(Long studentId, Long homeworkId) {
        // 1. 查询作业基本信息
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!HomeworkStatusEnum.IN_PROGRESS.getCode().equals(homework.getStatus())
                && !HomeworkStatusEnum.ENDED.getCode().equals(homework.getStatus())) {
            throw new BusinessException("作业暂未发布");
        }

        // 2. 校验学生是否有权限做该作业
        checkStudentAccess(studentId, homework);

        // 3. 查询作业题目关联
        List<HomeworkQuestion> hqList = homeworkQuestionMapper.selectList(
                Wrappers.lambdaQuery(HomeworkQuestion.class)
                        .eq(HomeworkQuestion::getHomeworkId, homeworkId)
                        .orderByAsc(HomeworkQuestion::getSortOrder));

        if (hqList.isEmpty()) {
            HomeworkDoVO vo = new HomeworkDoVO();
            vo.setHomeworkId(homeworkId);
            vo.setTitle(homework.getTitle());
            vo.setDescription(homework.getDescription());
            vo.setCourseName(homework.getCourseName());
            vo.setTotalScore(0);
            vo.setDeadline(homework.getDeadline());
            vo.setStatus(homework.getStatus());
            vo.setSubmitted(false);
            vo.setQuestions(Collections.emptyList());
            return vo;
        }

        // 4. 批量查询题目详情
        List<Long> questionIds = hqList.stream()
                .map(HomeworkQuestion::getQuestionId)
                .collect(Collectors.toList());
        List<QuestionDetailVO> details = fetchQuestionDetails(questionIds);

        // 按 questionId 建立映射
        Map<Long, QuestionDetailVO> detailMap = details.stream()
                .collect(Collectors.toMap(QuestionDetailVO::getId, d -> d));

        // 5. 检查是否已提交
        HomeworkSubmission submission = homeworkSubmissionMapper.selectOne(
                Wrappers.lambdaQuery(HomeworkSubmission.class)
                        .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                        .eq(HomeworkSubmission::getStudentId, studentId));
        boolean submitted = submission != null && submission.getSubmitTime() != null;

        // 6. 获取已作答答案（优先 Redis，其次 MySQL）
        Map<String, String> redisAnswers = getRedisAnswers(homeworkId, studentId);
        List<HomeworkAnswer> submittedAnswers = null;
        if (redisAnswers.isEmpty() && submitted) {
            // Redis 无缓存，从 MySQL 读取已提交答案
            submittedAnswers = homeworkAnswerMapper.selectList(
                    Wrappers.lambdaQuery(HomeworkAnswer.class)
                            .eq(HomeworkAnswer::getHomeworkId, homeworkId)
                            .eq(HomeworkAnswer::getStudentId, studentId));
        }

        // 7. 组装 VO
        List<HomeworkDoVO.QuestionVO> questionVOs = new ArrayList<>();
        for (HomeworkQuestion hq : hqList) {
            QuestionDetailVO detail = detailMap.get(hq.getQuestionId());
            if (detail == null) {
                continue;
            }

            HomeworkDoVO.QuestionVO qvo = new HomeworkDoVO.QuestionVO();
            qvo.setQuestionId(detail.getId());
            qvo.setQuestionType(QuestionTypeEnum.toValue(detail.getType()));
            qvo.setContent(detail.getContent());
            qvo.setScore(hq.getScore());
            qvo.setSortOrder(hq.getSortOrder());

            // 优先取 Redis 暂存，其次 MySQL 已提交
            String cachedAnswer = redisAnswers.get(String.valueOf(detail.getId()));
            if (cachedAnswer != null) {
                qvo.setAnswer(cachedAnswer);
            } else if (submittedAnswers != null) {
                submittedAnswers.stream()
                        .filter(a -> a.getQuestionId().equals(detail.getId()))
                        .findFirst()
                        .ifPresent(a -> qvo.setAnswer(a.getAnswer()));
            }

            // 选择题才提供选项（学生端不返回 isCorrect）
            if (detail.getOptions() != null && !detail.getOptions().isEmpty()) {
                List<HomeworkDoVO.OptionVO> optionVOs = detail.getOptions().stream()
                        .map(opt -> {
                            HomeworkDoVO.OptionVO ovo = new HomeworkDoVO.OptionVO();
                            ovo.setLabel(opt.getLabel());
                            ovo.setContent(opt.getContent());
                            return ovo;
                        })
                        .collect(Collectors.toList());
                qvo.setOptions(optionVOs);
            }

            // 编程题返回模板代码和示例测试用例
            if (Integer.valueOf(5).equals(detail.getType())) {
                if (detail.getProgrammingConfig() != null) {
                    qvo.setTemplateCode(detail.getProgrammingConfig().getTemplateCode());
                }
                if (detail.getTestCases() != null) {
                    List<HomeworkDoVO.TestCaseVO> testCaseVOs = detail.getTestCases().stream()
                            .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                            .map(tc -> {
                                HomeworkDoVO.TestCaseVO tvo = new HomeworkDoVO.TestCaseVO();
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

        // 8. 返回
        HomeworkDoVO vo = new HomeworkDoVO();
        vo.setHomeworkId(homeworkId);
        vo.setTitle(homework.getTitle());
        vo.setDescription(homework.getDescription());
        vo.setCourseName(homework.getCourseName());
        vo.setTotalScore(homework.getTotalScore());
        vo.setDeadline(homework.getDeadline());
        vo.setStatus(homework.getStatus());
        vo.setSubmitted(submitted);
        vo.setQuestions(questionVOs);
        return vo;
    }

    @Override
    public void saveAnswer(Long studentId, AnswerSaveDTO dto) {
        // 校验作业状态
        Homework homework = homeworkMapper.selectById(dto.getHomeworkId());
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!HomeworkStatusEnum.IN_PROGRESS.getCode().equals(homework.getStatus())) {
            throw new BusinessException("作业未在进行中，无法作答");
        }
        if (homework.getDeadline() != null && LocalDateTime.now().isAfter(homework.getDeadline())) {
            throw new BusinessException("作业已截止，无法作答");
        }

        // 检查是否已提交
        HomeworkSubmission submission = homeworkSubmissionMapper.selectOne(
                Wrappers.lambdaQuery(HomeworkSubmission.class)
                        .eq(HomeworkSubmission::getHomeworkId, dto.getHomeworkId())
                        .eq(HomeworkSubmission::getStudentId, studentId));
        if (submission != null && submission.getSubmitTime() != null) {
            throw new BusinessException("作业已提交，无法修改");
        }

        // 写入 Redis Hash
        String redisKey = redisKey(dto.getHomeworkId(), studentId);
        RMap<String, String> map = redissonClient.getMap(redisKey);
        map.put(String.valueOf(dto.getQuestionId()), dto.getAnswer() != null ? dto.getAnswer() : "");
        map.expire(REDIS_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitHomework(Long studentId, HomeworkSubmitDTO dto) {
        Long homeworkId = dto.getHomeworkId();

        // 1. 校验作业
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!HomeworkStatusEnum.IN_PROGRESS.getCode().equals(homework.getStatus())) {
            throw new BusinessException("作业未在进行中，无法提交");
        }
        if (homework.getDeadline() != null && LocalDateTime.now().isAfter(homework.getDeadline())) {
            throw new BusinessException("作业已截止，无法提交");
        }

        // 2. 校验可提交
        HomeworkSubmission existingSubmission = homeworkSubmissionMapper.selectOne(
                Wrappers.lambdaQuery(HomeworkSubmission.class)
                        .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                        .eq(HomeworkSubmission::getStudentId, studentId));
        if (existingSubmission != null && existingSubmission.getSubmitTime() != null) {
            throw new BusinessException("作业已提交，请勿重复提交");
        }

        // 3. 校验学生权限
        checkStudentAccess(studentId, homework);

        // 4. 获取 Redis 中所有暂存答案
        String redisKey = redisKey(homeworkId, studentId);
        RMap<String, String> redisMap = redissonClient.getMap(redisKey);
        Map<String, String> redisEntries = redisMap.readAllMap();

        if (redisEntries.isEmpty()) {
            throw new BusinessException("暂无作答记录，请先作答后再提交");
        }

        // 5. 查询题目详情（用于自动评分）
        List<HomeworkQuestion> hqList = homeworkQuestionMapper.selectList(
                Wrappers.lambdaQuery(HomeworkQuestion.class)
                        .eq(HomeworkQuestion::getHomeworkId, homeworkId));
        List<Long> questionIds = hqList.stream()
                .map(HomeworkQuestion::getQuestionId)
                .collect(Collectors.toList());
        List<QuestionDetailVO> details = fetchQuestionDetails(questionIds);
        Map<Long, QuestionDetailVO> detailMap = details.stream()
                .collect(Collectors.toMap(QuestionDetailVO::getId, d -> d));
        Map<Long, Integer> questionScoreMap = hqList.stream()
                .collect(Collectors.toMap(HomeworkQuestion::getQuestionId, HomeworkQuestion::getScore));

        // 6. 构建 HomeworkAnswer 并自动评分
        int totalScore = 0;
        int autoGradedCount = 0;
        int correctCount = 0;
        List<HomeworkAnswer> answers = new ArrayList<>();

        for (Map.Entry<String, String> entry : redisEntries.entrySet()) {
            Long questionId = Long.valueOf(entry.getKey());
            String studentAnswer = entry.getValue() != null ? entry.getValue() : "";

            HomeworkAnswer answer = new HomeworkAnswer();
            answer.setHomeworkId(homeworkId);
            answer.setStudentId(studentId);
            answer.setQuestionId(questionId);
            answer.setAnswer(studentAnswer);

            QuestionDetailVO detail = detailMap.get(questionId);
            if (detail != null) {
                Integer score = autoGrade(detail.getType(), studentAnswer, detail.getAnswer());
                if (score != null) {
                    int questionScore = questionScoreMap.getOrDefault(questionId, 0);
                    if (Objects.equals(score, 1)) {
                        // 正确：得满分
                        answer.setScore(questionScore);
                        answer.setIsCorrect(1);
                        totalScore += questionScore;
                        correctCount++;
                    } else if (Objects.equals(score, 0)) {
                        // 错误：得 0 分
                        answer.setScore(0);
                        answer.setIsCorrect(0);
                    }
                    // 多选题部分正确暂不处理（剩余得0分）
                    autoGradedCount++;
                }
            }

            answers.add(answer);
        }

        // 7. 批量插入/更新 homework_answer
        // 编程题可能已有作答记录（来自判题 API 的预创建），需要复用已有记录保持关联
        for (HomeworkAnswer answer : answers) {
            HomeworkAnswer existing = homeworkAnswerMapper.selectOne(
                    Wrappers.lambdaQuery(HomeworkAnswer.class)
                            .eq(HomeworkAnswer::getHomeworkId, homeworkId)
                            .eq(HomeworkAnswer::getStudentId, studentId)
                            .eq(HomeworkAnswer::getQuestionId, answer.getQuestionId()));
            if (existing != null) {
                // 已有记录（编程题被判题过），更新内容和分数
                existing.setAnswer(answer.getAnswer());
                if (answer.getScore() != null) {
                    existing.setScore(answer.getScore());
                    existing.setIsCorrect(answer.getIsCorrect());
                }
                homeworkAnswerMapper.updateById(existing);
            } else {
                homeworkAnswerMapper.insert(answer);
            }
        }

        // 8. 创建/更新 homework_submission
        if (existingSubmission != null) {
            existingSubmission.setScore(totalScore);
            existingSubmission.setSubmitTime(LocalDateTime.now());
            existingSubmission.setGradeStatus(determineGradeStatus(answers));
            existingSubmission.setStatus(1);
            homeworkSubmissionMapper.updateById(existingSubmission);
        } else {
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setHomeworkId(homeworkId);
            submission.setStudentId(studentId);
            submission.setScore(totalScore);
            submission.setSubmitTime(LocalDateTime.now());
            submission.setGradeStatus(determineGradeStatus(answers));
            submission.setStatus(1);
            homeworkSubmissionMapper.insert(submission);
        }

        // 9. 更新作业批改状态（如果有主观题需要批改）
        boolean hasSubjective = answers.stream().anyMatch(a -> a.getIsCorrect() == null && a.getScore() == null);
        if (hasSubjective) {
            homework.setGradingStatus(1);
            homeworkMapper.updateById(homework);
        }

        // 10. 清理 Redis 缓存
        redisMap.delete();

        log.info("学生提交作业: studentId={}, homeworkId={}, 总分={}, 自动批改={}题, 正确={}题",
                studentId, homeworkId, totalScore, autoGradedCount, correctCount);
    }

    // ==================== 私有方法 ====================

    /**
     * Redis Key: homework:answers:{homeworkId}:{studentId}
     */
    private String redisKey(Long homeworkId, Long studentId) {
        return REDIS_KEY_PREFIX + homeworkId + ":" + studentId;
    }

    /**
     * 从 Redis 获取学生暂存的全部答案
     */
    private Map<String, String> getRedisAnswers(Long homeworkId, Long studentId) {
        String redisKey = redisKey(homeworkId, studentId);
        RMap<String, String> map = redissonClient.getMap(redisKey);
        return new HashMap<>(map.readAllMap());
    }

    /**
     * 校验学生是否有权限做该作业
     */
    private void checkStudentAccess(Long studentId, Homework homework) {
        List<Long> classIds = Optional.ofNullable(
                courseServiceClient.getStudentCourseClassIds(studentId, homework.getCourseId()))
                .map(Result::getData)
                .orElse(Collections.emptyList());

        if (classIds.isEmpty()) {
            throw new BusinessException("您不在该课程的任何教学班中");
        }

        Long count = homeworkClassMapper.selectCount(
                Wrappers.lambdaQuery(HomeworkClass.class)
                        .eq(HomeworkClass::getHomeworkId, homework.getId())
                        .in(HomeworkClass::getCourseClassId, classIds));

        if (count == null || count == 0) {
            throw new BusinessException("您没有权限做该作业");
        }
    }

    /**
     * 批量从 question-service 获取题目详情
     */
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

    /**
     * 自动评分：返回 1=正确, 0=错误, null=无法自动评分
     */
    private Integer autoGrade(Integer questionType, String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || studentAnswer.isEmpty() || correctAnswer == null || correctAnswer.isEmpty()) {
            return null;
        }

        if (questionType == 1) {
            // 单选：精确匹配
            return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim()) ? 1 : 0;
        } else if (questionType == 2) {
            // 多选：排序后比较（答案如 "ABC" 无序，需排序后比较）
            String sortedStudent = sortAnswer(studentAnswer);
            String sortedCorrect = sortAnswer(correctAnswer);
            return sortedStudent.equals(sortedCorrect) ? 1 : 0;
        }
        // 填空/简答：需人工批改
        return null;
    }

    /**
     * 对多选题答案字符串排序，忽略非字母字符
     */
    private String sortAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        char[] chars = answer.toUpperCase().replaceAll("[^A-Z]", "").toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    /**
     * 判断批改状态：所有题目都自动批改了 → 无需批改(0)，有主观题 → 待批改(1)
     */
    private Integer determineGradeStatus(List<HomeworkAnswer> answers) {
        boolean hasUngraded = answers.stream().anyMatch(a -> a.getIsCorrect() == null);
        return hasUngraded ? 1 : 0;
    }
}
