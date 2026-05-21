package com.bucher.homework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bucher.common.result.Result;
import com.bucher.homework.dto.AnswerSaveDTO;
import com.bucher.homework.dto.HomeworkDraftSaveDTO;
import com.bucher.homework.dto.HomeworkListQueryDTO;
import com.bucher.homework.dto.HomeworkPublishDTO;
import com.bucher.homework.dto.HomeworkSubmitDTO;
import com.bucher.homework.dto.JudgeSubmitDTO;
import com.bucher.homework.service.HomeworkService;
import com.bucher.homework.service.JudgeService;
import com.bucher.homework.vo.CodeRunResultVO;
import com.bucher.homework.vo.HomeworkDoVO;
import com.bucher.homework.vo.HomeworkListVO;
import com.bucher.homework.vo.HomeworkUnpublishedVO;
import com.bucher.homework.vo.JudgeResultVO;
import com.bucher.homework.vo.StudentHomeworkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作业 Controller
 */
@Tag(name = "作业管理")
@Validated
@RestController
@RequestMapping("/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;
    private final JudgeService judgeService;

    @Operation(summary = "查询教师作业列表")
    @GetMapping("/teacher/list")
    public Result<IPage<HomeworkListVO>> getTeacherHomeworkList(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            HomeworkListQueryDTO dto) {
        dto.setTeacherId(teacherId);
        return Result.success(homeworkService.getTeacherHomeworkList(dto));
    }

    @Operation(summary = "查询未发布作业列表")
    @GetMapping("/teacher/unpublished-list")
    public Result<IPage<HomeworkUnpublishedVO>> getUnpublishedHomeworkList(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return Result.success(homeworkService.getUnpublishedHomeworkList(teacherId, pageNum, pageSize));
    }

    @Operation(summary = "保存作业草稿")
    @PostMapping("/teacher/draft")
    public Result<Void> saveDraft(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody HomeworkDraftSaveDTO dto) {
        homeworkService.saveDraft(dto, teacherId, role);
        return Result.success();
    }

    @Operation(summary = "查询学生端作业列表")
    @GetMapping("/student/list")
    public Result<List<StudentHomeworkVO>> getStudentHomeworkList(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam Long courseId) {
        return Result.success(homeworkService.getStudentHomeworkList(studentId, courseId));
    }

    @Operation(summary = "发布作业")
    @PostMapping("/teacher/publish")
    public Result<Void> publish(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody HomeworkPublishDTO dto) {
        homeworkService.publish(dto, teacherId, role);
        return Result.success();
    }

    @Operation(summary = "获取做题页面数据")
    @GetMapping("/student/{homeworkId}/do")
    public Result<HomeworkDoVO> getHomeworkDetail(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long homeworkId) {
        return Result.success(homeworkService.getHomeworkDetail(studentId, homeworkId));
    }

    @Operation(summary = "暂存单题答案")
    @PostMapping("/student/answer/save")
    public Result<Void> saveAnswer(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody AnswerSaveDTO dto) {
        homeworkService.saveAnswer(studentId, dto);
        return Result.success();
    }

    @Operation(summary = "提交全部答案（提交后自动判编程题）")
    @PostMapping("/student/submit")
    public Result<Void> submitHomework(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody HomeworkSubmitDTO dto) {
        homeworkService.submitHomework(studentId, dto);
        // 提交后异步触发编程题判题
        judgeService.asyncJudgeProgrammingQuestions(studentId, dto.getHomeworkId());
        return Result.success();
    }

    @Operation(summary = "运行编程题代码（仅样例，不保存）")
    @PostMapping("/student/answer/run")
    public Result<CodeRunResultVO> runCode(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody JudgeSubmitDTO dto) {
        return Result.success(judgeService.runCode(studentId, dto));
    }

    @Operation(summary = "查询编程题判题状态")
    @GetMapping("/student/answer/judge/status/{answerId}")
    public Result<JudgeResultVO> getJudgeStatus(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long answerId) {
        return Result.success(judgeService.getJudgeStatus(answerId));
    }
}
