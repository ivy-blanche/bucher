package com.bucher.homework.feign;

import com.bucher.common.result.Result;
import com.bucher.homework.vo.QuestionDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * question-service Feign 客户端
 */
@FeignClient(name = "question-service", path = "/question")
public interface QuestionServiceClient {

    /**
     * 获取题目详情（含选项）
     */
    @GetMapping("/{id}")
    Result<QuestionDetailVO> getDetail(@PathVariable("id") Long id);

    /**
     * 批量查询题目详情（内部调用）
     */
    @PostMapping("/internal/batch")
    Result<List<QuestionDetailVO>> getBatchDetail(@RequestBody List<Long> ids);
}
