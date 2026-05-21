package com.bucher.homework.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.result.Result;
import com.bucher.homework.dto.JudgeSubmitDTO;
import com.bucher.homework.entity.Homework;
import com.bucher.homework.entity.HomeworkAnswer;
import com.bucher.homework.entity.HomeworkAnswerJudge;
import com.bucher.homework.entity.HomeworkClass;
import com.bucher.homework.entity.HomeworkQuestion;
import com.bucher.homework.entity.HomeworkSubmission;
import com.bucher.homework.enums.HomeworkStatusEnum;
import com.bucher.homework.feign.CourseServiceClient;
import com.bucher.homework.feign.QuestionServiceClient;
import com.bucher.homework.mapper.HomeworkAnswerJudgeMapper;
import com.bucher.homework.mapper.HomeworkAnswerMapper;
import com.bucher.homework.mapper.HomeworkClassMapper;
import com.bucher.homework.mapper.HomeworkMapper;
import com.bucher.homework.mapper.HomeworkQuestionMapper;
import com.bucher.homework.mapper.HomeworkSubmissionMapper;
import com.bucher.homework.service.JudgeService;
import com.bucher.homework.vo.CodeRunResultVO;
import com.bucher.homework.vo.JudgeResultVO;
import com.bucher.homework.vo.QuestionDetailVO;
import com.bucher.homework.vo.QuestionProgrammingVO;
import com.bucher.homework.vo.QuestionTestCaseVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 编程题判题服务实现
 *
 * 运行与判题分离：
 * - runCode：做作业时运行，仅针对样例测试用例，不保存
 * - asyncJudgeProgrammingQuestions：提交作业后异步判题，针对所有测试用例，保存结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Judge0 已完成状态 ID（>= 3 表示判题完成） */
    private static final int JUDGE0_COMPLETED = 3;
    /** Judge0 通过状态 ID */
    private static final int JUDGE0_ACCEPTED = 3;

    public static final int JUDGE_STATUS_PENDING = 0;
    public static final int JUDGE_STATUS_PROCESSING = 1;
    public static final int JUDGE_STATUS_COMPLETED = 2;
    public static final int JUDGE_STATUS_FAILED = 3;

    private final HomeworkMapper homeworkMapper;
    private final HomeworkQuestionMapper homeworkQuestionMapper;
    private final HomeworkClassMapper homeworkClassMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final HomeworkAnswerMapper homeworkAnswerMapper;
    private final HomeworkAnswerJudgeMapper homeworkAnswerJudgeMapper;
    private final QuestionServiceClient questionServiceClient;
    private final CourseServiceClient courseServiceClient;
    private final RestTemplate restTemplate;

    @Value("${judge0.base-url:http://localhost:2358}")
    private String judge0BaseUrl;

    @Override
    public CodeRunResultVO runCode(Long studentId, JudgeSubmitDTO dto) {
        // 1. 校验作业
        Homework homework = homeworkMapper.selectById(dto.getHomeworkId());
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!HomeworkStatusEnum.IN_PROGRESS.getCode().equals(homework.getStatus())) {
            throw new BusinessException("作业未在进行中");
        }
        checkStudentAccess(studentId, homework);

        // 2. 获取题目详情
        QuestionDetailVO questionDetail = fetchQuestionDetail(dto.getQuestionId());
        if (questionDetail == null || !Integer.valueOf(5).equals(questionDetail.getType())) {
            throw new BusinessException("题目不存在或不是编程题");
        }

        QuestionProgrammingVO programmingConfig = questionDetail.getProgrammingConfig();
        if (programmingConfig == null) {
            throw new BusinessException("题目编程配置缺失");
        }

        // 3. 仅选取样例测试用例
        List<QuestionTestCaseVO> allTestCases = questionDetail.getTestCases();
        if (allTestCases == null || allTestCases.isEmpty()) {
            throw new BusinessException("该编程题没有测试用例");
        }
        List<QuestionTestCaseVO> sampleCases = allTestCases.stream()
                .filter(tc -> Boolean.TRUE.equals(tc.getIsSample()))
                .collect(Collectors.toList());
        if (sampleCases.isEmpty()) {
            throw new BusinessException("该编程题没有样例测试用例，无法运行");
        }

        // 4. 提交到 Judge0（同步等待结果）
        try {
            return submitAndWait(dto.getCode(), programmingConfig, sampleCases);
        } catch (Exception e) {
            log.error("代码运行失败", e);
            return CodeRunResultVO.builder()
                    .compileSuccess(false)
                    .compileOutput("运行失败: " + e.getMessage())
                    .passedCount(0)
                    .totalCount(sampleCases.size())
                    .build();
        }
    }

    @Override
    public void asyncJudgeProgrammingQuestions(Long studentId, Long homeworkId) {
        CompletableFuture.runAsync(() -> {
            try {
                doJudgeProgrammingQuestions(studentId, homeworkId);
            } catch (Exception e) {
                log.error("异步判题失败: studentId={}, homeworkId={}", studentId, homeworkId, e);
            }
        });
    }

    @Override
    public JudgeResultVO getJudgeStatus(Long answerId) {
        HomeworkAnswerJudge judge = homeworkAnswerJudgeMapper.selectOne(
                Wrappers.lambdaQuery(HomeworkAnswerJudge.class)
                        .eq(HomeworkAnswerJudge::getAnswerId, answerId));
        if (judge == null) {
            throw new BusinessException("判题记录不存在");
        }
        return buildJudgeResult(answerId, judge);
    }

    // ==================== 私有方法 ====================

    /**
     * 逐条提交测试用例到 Judge0（单条 + wait=true），同步等待全部结果
     */
    private CodeRunResultVO submitAndWait(String code, QuestionProgrammingVO config,
                                          List<QuestionTestCaseVO> testCases) {
        int passedCount = 0;
        String compileOutput = null;
        List<CodeRunResultVO.RunTestResult> tcResults = new ArrayList<>();

        for (QuestionTestCaseVO tc : testCases) {
            Map<String, Object> body = buildSubmissionBody(code, config, tc);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonBody;
            try {
                jsonBody = OBJECT_MAPPER.writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new BusinessException("序列化请求体失败");
            }
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            String url = judge0BaseUrl + "/submissions?base64_encoded=false&wait=true";
            var response = restTemplate.postForEntity(url, requestEntity, Map.class);
            Map<String, Object> result = response.getBody();

            Map<String, Object> statusMap = (Map<String, Object>) result.get("status");
            int statusId = statusMap != null ? (Integer) statusMap.get("id") : 0;

            boolean passed = statusId == JUDGE0_ACCEPTED;
            if (passed) passedCount++;

            String co = (String) result.get("compile_output");
            if (co != null && !co.isBlank() && compileOutput == null) {
                compileOutput = co;
            }

            tcResults.add(CodeRunResultVO.RunTestResult.builder()
                    .passed(passed)
                    .stdout((String) result.get("stdout"))
                    .expectedOutput(tc.getExpectedOutput())
                    .time((String) result.get("time"))
                    .memory(result.get("memory") != null ? ((Number) result.get("memory")).longValue() : null)
                    .build());
        }

        return CodeRunResultVO.builder()
                .compileSuccess(compileOutput == null)
                .compileOutput(compileOutput)
                .passedCount(passedCount)
                .totalCount(testCases.size())
                .testCaseResults(tcResults)
                .build();
    }

    /**
     * 构建 Judge0 单条提交请求体
     */
    private Map<String, Object> buildSubmissionBody(String code, QuestionProgrammingVO config,
                                                    QuestionTestCaseVO tc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_code", code);
        body.put("language_id", config.getJudge0LanguageId());
        body.put("stdin", tc.getInput() != null ? tc.getInput() : "");
        body.put("expected_output", tc.getExpectedOutput() != null ? tc.getExpectedOutput() : "");
        if (config.getTimeLimit() != null) {
            body.put("cpu_time_limit", config.getTimeLimit() / 1000.0);
        }
        if (config.getMemoryLimit() != null) {
            body.put("memory_limit", config.getMemoryLimit());
        }
        return body;
    }

    /**
     * 执行编程题判题（同步，在异步线程中调用）
     */
    private void doJudgeProgrammingQuestions(Long studentId, Long homeworkId) {
        // 1. 查询该作业下学生已提交的编程题答案
        List<HomeworkQuestion> hqList = homeworkQuestionMapper.selectList(
                Wrappers.lambdaQuery(HomeworkQuestion.class)
                        .eq(HomeworkQuestion::getHomeworkId, homeworkId));

        if (hqList.isEmpty()) {
            return;
        }

        for (HomeworkQuestion hq : hqList) {
            Long questionId = hq.getQuestionId();

            // 从 question-service 获取题目详情判断是否为编程题
            QuestionDetailVO questionDetail;
            try {
                questionDetail = fetchQuestionDetail(questionId);
            } catch (Exception e) {
                log.warn("获取题目详情失败，跳过: questionId={}", questionId, e);
                continue;
            }
            if (questionDetail == null || !Integer.valueOf(5).equals(questionDetail.getType())) {
                continue;
            }

            // 2. 获取学生答案
            HomeworkAnswer answer = homeworkAnswerMapper.selectOne(
                    Wrappers.lambdaQuery(HomeworkAnswer.class)
                            .eq(HomeworkAnswer::getHomeworkId, homeworkId)
                            .eq(HomeworkAnswer::getStudentId, studentId)
                            .eq(HomeworkAnswer::getQuestionId, questionId));
            if (answer == null || answer.getAnswer() == null || answer.getAnswer().isBlank()) {
                log.warn("编程题无作答，跳过: questionId={}", questionId);
                continue;
            }

            // 3. 创建判题记录
            // 删除旧的判题记录
            homeworkAnswerJudgeMapper.delete(
                    Wrappers.lambdaQuery(HomeworkAnswerJudge.class)
                            .eq(HomeworkAnswerJudge::getAnswerId, answer.getId()));

            HomeworkAnswerJudge judge = new HomeworkAnswerJudge();
            judge.setAnswerId(answer.getId());
            judge.setQuestionId(questionId);
            judge.setStatus(JUDGE_STATUS_PENDING);
            homeworkAnswerJudgeMapper.insert(judge);

            // 4. 提交到 Judge0 进行完整判题（所有测试用例）
            QuestionProgrammingVO programmingConfig = questionDetail.getProgrammingConfig();
            if (programmingConfig == null) {
                judge.setStatus(JUDGE_STATUS_FAILED);
                judge.setResult("编程配置缺失");
                homeworkAnswerJudgeMapper.updateById(judge);
                continue;
            }

            List<QuestionTestCaseVO> allTestCases = questionDetail.getTestCases();
            if (allTestCases == null || allTestCases.isEmpty()) {
                judge.setStatus(JUDGE_STATUS_FAILED);
                judge.setResult("无测试用例");
                homeworkAnswerJudgeMapper.updateById(judge);
                continue;
            }

            try {
                judge.setStatus(JUDGE_STATUS_PROCESSING);
                homeworkAnswerJudgeMapper.updateById(judge);

                // 逐个提交测试用例到 Judge0（单条 + wait=true）
                List<Map<String, Object>> results = new ArrayList<>();
                for (QuestionTestCaseVO tc : allTestCases) {
                    Map<String, Object> body = buildSubmissionBody(
                            answer.getAnswer(), programmingConfig, tc);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    String jsonBody;
                    try {
                        jsonBody = OBJECT_MAPPER.writeValueAsString(body);
                    } catch (JsonProcessingException e) {
                        throw new BusinessException("序列化请求体失败");
                    }
                    HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

                    String url = judge0BaseUrl + "/submissions?base64_encoded=false&wait=true";
                    var response = restTemplate.postForEntity(url, requestEntity, Map.class);

                    if (response.getBody() == null) {
                        throw new BusinessException("Judge0 返回异常");
                    }
                    results.add(response.getBody());
                }

                // 5. 处理结果
                processJudgeResults(judge, answer, results, allTestCases, hq.getScore());

            } catch (Exception e) {
                log.error("编程题判题失败: questionId={}", questionId, e);
                judge.setStatus(JUDGE_STATUS_FAILED);
                judge.setResult("判题失败: " + e.getMessage());
                homeworkAnswerJudgeMapper.updateById(judge);
            }
        }
    }

    /**
     * 处理判题结果并保存
     */
    private void processJudgeResults(HomeworkAnswerJudge judge, HomeworkAnswer answer,
                                     List<Map<String, Object>> results,
                                     List<QuestionTestCaseVO> testCases,
                                     Integer questionScore) {
        int passedCount = 0;
        int totalCount = results.size();
        String compileOutput = null;

        List<Map<String, Object>> tcResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> result = results.get(i);
            Map<String, Object> statusMap = (Map<String, Object>) result.get("status");
            int statusId = statusMap != null ? (Integer) statusMap.get("id") : 0;
            String statusDesc = statusMap != null ? (String) statusMap.get("description") : "Unknown";

            boolean passed = statusId == JUDGE0_ACCEPTED;
            if (passed) {
                passedCount++;
            }

            String co = (String) result.get("compile_output");
            if (co != null && !co.isBlank() && compileOutput == null) {
                compileOutput = co;
            }

            Map<String, Object> tcResult = new LinkedHashMap<>();
            tcResult.put("judgeStatusId", statusId);
            tcResult.put("judgeStatusDesc", statusDesc);
            tcResult.put("passed", passed);
            tcResult.put("stdout", result.get("stdout"));
            tcResult.put("expectedOutput", testCases.get(i).getExpectedOutput());
            tcResult.put("time", result.get("time"));
            tcResult.put("memory", result.get("memory"));
            tcResults.add(tcResult);
        }

        // 计算得分
        int finalScore = 0;
        if (totalCount > 0 && questionScore != null && questionScore > 0) {
            finalScore = (int) Math.round((double) passedCount / totalCount * questionScore);
        }

        // 组装结果 JSON
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("passedCount", passedCount);
        resultMap.put("totalCount", totalCount);
        resultMap.put("compileOutput", compileOutput);
        resultMap.put("testCases", tcResults);

        try {
            judge.setResult(OBJECT_MAPPER.writeValueAsString(resultMap));
        } catch (JsonProcessingException e) {
            judge.setResult("{\"error\":\"序列化失败\"}");
        }
        judge.setCompileOutput(compileOutput);
        judge.setScore(finalScore);
        judge.setStatus(JUDGE_STATUS_COMPLETED);
        homeworkAnswerJudgeMapper.updateById(judge);

        // 更新作答得分
        answer.setScore(finalScore);
        answer.setIsCorrect(passedCount == totalCount ? 1 : 0);
        homeworkAnswerMapper.updateById(answer);

        log.info("编程题判题完成: answerId={}, passed={}/{}, score={}, questionId={}",
                answer.getId(), passedCount, totalCount, finalScore, judge.getQuestionId());
    }

    /**
     * 从 question-service 获取题目详情
     */
    private QuestionDetailVO fetchQuestionDetail(Long questionId) {
        try {
            Result<QuestionDetailVO> result = questionServiceClient.getDetail(questionId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                return result.getData();
            }
            log.warn("查询题目详情失败: questionId={}, msg={}", questionId,
                    result != null ? result.getMessage() : "result is null");
            return null;
        } catch (Exception e) {
            log.error("Feign 调用 question-service 失败", e);
            throw new BusinessException("获取题目信息失败");
        }
    }

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

    private JudgeResultVO buildJudgeResult(Long answerId, HomeworkAnswerJudge judge) {
        Integer passedCount = 0;
        Integer totalCount = 0;
        String compileOutput = judge.getCompileOutput();
        List<JudgeResultVO.TestCaseResult> tcResults = null;

        if (judge.getResult() != null && !judge.getResult().isBlank()) {
            try {
                Map<String, Object> resultMap = OBJECT_MAPPER.readValue(
                        judge.getResult(), new TypeReference<Map<String, Object>>() {});
                passedCount = (Integer) resultMap.getOrDefault("passedCount", 0);
                totalCount = (Integer) resultMap.getOrDefault("totalCount", 0);
                if (compileOutput == null) {
                    compileOutput = (String) resultMap.get("compileOutput");
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tcMaps = (List<Map<String, Object>>) resultMap.get("testCases");
                if (tcMaps != null) {
                    tcResults = tcMaps.stream().map(tc -> JudgeResultVO.TestCaseResult.builder()
                            .passed((Boolean) tc.get("passed"))
                            .judgeStatusId((Integer) tc.get("judgeStatusId"))
                            .judgeStatusDesc((String) tc.get("judgeStatusDesc"))
                            .stdout((String) tc.get("stdout"))
                            .expectedOutput((String) tc.get("expectedOutput"))
                            .time((String) tc.get("time"))
                            .memory(tc.get("memory") != null ? ((Number) tc.get("memory")).longValue() : null)
                            .build()).collect(Collectors.toList());
                }
            } catch (JsonProcessingException e) {
                log.warn("解析判题结果 JSON 失败", e);
            }
        }

        return JudgeResultVO.builder()
                .answerId(answerId)
                .judgeId(judge.getId())
                .status(judge.getStatus())
                .score(judge.getScore())
                .passed(passedCount > 0 && Objects.equals(passedCount, totalCount))
                .passedCount(passedCount)
                .totalCount(totalCount > 0 ? totalCount : null)
                .compileOutput(compileOutput)
                .testCaseResults(tcResults)
                .build();
    }
}
