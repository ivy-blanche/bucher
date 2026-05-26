package com.bucher.exam.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bucher.common.result.Result;
import com.bucher.exam.dto.AnswerSaveDTO;
import com.bucher.exam.dto.ExamDraftSaveDTO;
import com.bucher.exam.dto.ExamListQueryDTO;
import com.bucher.exam.dto.ExamPublishDTO;
import com.bucher.exam.dto.ExamSubmitDTO;
import com.bucher.exam.service.ExamService;
import com.bucher.exam.vo.ExamDoVO;
import com.bucher.exam.vo.ExamListVO;
import com.bucher.exam.vo.ExamUnpublishedVO;
import com.bucher.exam.vo.StudentExamVO;
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
 * 考试 Controller
 */
@Tag(name = "考试管理")
@Validated
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "查询教师考试列表")
    @GetMapping("/teacher/list")
    public Result<IPage<ExamListVO>> getTeacherExamList(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            ExamListQueryDTO dto) {
        dto.setTeacherId(teacherId);
        return Result.success(examService.getTeacherExamList(dto));
    }

    @Operation(summary = "查询未发布考试列表")
    @GetMapping("/teacher/unpublished-list")
    public Result<IPage<ExamUnpublishedVO>> getUnpublishedExamList(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) Integer pageSize) {
        return Result.success(examService.getUnpublishedExamList(teacherId, pageNum, pageSize));
    }

    @Operation(summary = "保存考试草稿")
    @PostMapping("/teacher/draft")
    public Result<Void> saveDraft(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody ExamDraftSaveDTO dto) {
        examService.saveDraft(dto, teacherId, role);
        return Result.success();
    }

    @Operation(summary = "发布考试")
    @PostMapping("/teacher/publish")
    public Result<Void> publish(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody ExamPublishDTO dto) {
        examService.publish(dto, teacherId, role);
        return Result.success();
    }

    @Operation(summary = "查询学生端考试列表")
    @GetMapping("/student/list")
    public Result<List<StudentExamVO>> getStudentExamList(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @RequestParam Long courseId) {
        return Result.success(examService.getStudentExamList(studentId, courseId));
    }

    @Operation(summary = "获取做题页面数据")
    @GetMapping("/student/{examId}/do")
    public Result<ExamDoVO> getExamDetail(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @PathVariable Long examId) {
        return Result.success(examService.getExamDetail(studentId, examId));
    }

    @Operation(summary = "暂存单题答案")
    @PostMapping("/student/answer/save")
    public Result<Void> saveAnswer(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody AnswerSaveDTO dto) {
        examService.saveAnswer(studentId, dto);
        return Result.success();
    }

    @Operation(summary = "提交全部答案")
    @PostMapping("/student/submit")
    public Result<Void> submitExam(
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Role") Integer role,
            @Valid @RequestBody ExamSubmitDTO dto) {
        examService.submitExam(studentId, dto);
        return Result.success();
    }

    @Operation(summary = "超时自动提交（定时任务触发）")
    @PostMapping("/teacher/auto-submit")
    public Result<Void> autoSubmitExpiredExams(
            @RequestHeader("X-User-Role") Integer role) {
        examService.autoSubmitExpiredExams();
        return Result.success();
    }
}
