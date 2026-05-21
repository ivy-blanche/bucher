package com.bucher.question.controller;

import com.bucher.common.result.Result;
import com.bucher.question.dto.QuestionBatchSaveDTO;
import com.bucher.question.dto.QuestionImportExecuteDTO;
import com.bucher.question.service.QuestionService;
import com.bucher.question.vo.QuestionDetailVO;
import com.bucher.question.vo.QuestionImportResultVO;
import com.bucher.question.vo.QuestionImportUploadVO;
import com.bucher.question.vo.QuestionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bucher.common.exception.BusinessException;
import java.io.IOException;
import java.util.List;

/**
 * 题目控制器
 */
@Tag(name = "题目管理")
@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "获取题库下的题目列表")
    @GetMapping("/list")
    public Result<List<QuestionListVO>> listByGroupId(@RequestParam Long groupId) {
        return Result.success(questionService.listByGroupId(groupId));
    }

    @Operation(summary = "获取题目详情（含选项）")
    @GetMapping("/{id}")
    public Result<QuestionDetailVO> getDetail(@PathVariable Long id) {
        QuestionDetailVO vo = questionService.getDetail(id);
        if (vo == null) {
            return Result.error(404, "题目不存在");
        }
        return Result.success(vo);
    }

    @Operation(summary = "批量查询题目详情（内部调用）")
    @PostMapping("/internal/batch")
    public Result<List<QuestionDetailVO>> getBatchDetail(@RequestBody List<Long> ids) {
        return Result.success(questionService.getBatchDetail(ids));
    }

    @Operation(summary = "批量保存题目（新增/修改/删除）")
    @PostMapping("/batch-save")
    public Result<Void> batchSave(@Valid @RequestBody QuestionBatchSaveDTO dto,
                                  @RequestHeader("X-User-Id") Long userId) {
        questionService.batchSave(dto, userId);
        return Result.success();
    }

    @Operation(summary = "下载题目导入模板")
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=题目导入模板.xlsx");
        questionService.downloadTemplate(response.getOutputStream());
    }

    @Operation(summary = "上传导入文件")
    @PostMapping("/import/upload")
    public Result<QuestionImportUploadVO> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        try {
            QuestionImportUploadVO vo = questionService.uploadFile(
                    file.getOriginalFilename(), file.getInputStream());
            return Result.success(vo);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }
    }

    @Operation(summary = "执行导入")
    @PostMapping("/import/execute")
    public Result<QuestionImportResultVO> executeImport(@Valid @RequestBody QuestionImportExecuteDTO dto,
                                                        @RequestHeader("X-User-Id") Long userId) {
        QuestionImportResultVO vo = questionService.executeImport(dto.getGroupId(), dto.getFileKey(), userId);
        return Result.success(vo);
    }
}
