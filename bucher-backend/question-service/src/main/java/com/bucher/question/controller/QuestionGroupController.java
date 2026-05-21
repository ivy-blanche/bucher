package com.bucher.question.controller;

import com.bucher.common.result.Result;
import com.bucher.question.dto.QuestionGroupCreateDTO;
import com.bucher.question.dto.QuestionGroupUpdateDTO;
import com.bucher.question.service.QuestionGroupService;
import com.bucher.question.vo.QuestionGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题库分组控制器
 */
@Tag(name = "题库分组管理")
@RestController
@RequestMapping("/question/group")
@RequiredArgsConstructor
public class QuestionGroupController {

    private final QuestionGroupService questionGroupService;

    @Operation(summary = "获取教师题库分组列表")
    @GetMapping("/list")
    public Result<List<QuestionGroupVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(questionGroupService.listByTeacherId(userId));
    }

    @Operation(summary = "创建题库分组")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody QuestionGroupCreateDTO dto,
                               @RequestHeader("X-User-Id") Long userId) {
        Long id = questionGroupService.create(dto, userId);
        return Result.success(id);
    }

    @Operation(summary = "修改题库分组名称")
    @PutMapping("/{id}")
    public Result<Void> updateName(@PathVariable Long id,
                                   @Valid @RequestBody QuestionGroupUpdateDTO dto,
                                   @RequestHeader("X-User-Id") Long userId) {
        questionGroupService.updateName(id, dto, userId);
        return Result.success();
    }

    @Operation(summary = "删除题库分组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        questionGroupService.delete(id, userId);
        return Result.success();
    }
}
