package com.bucher.exam.feign;

import com.bucher.common.result.Result;
import com.bucher.exam.vo.QuestionDetailVO;
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

    @GetMapping("/{id}")
    Result<QuestionDetailVO> getDetail(@PathVariable("id") Long id);

    @PostMapping("/internal/batch")
    Result<List<QuestionDetailVO>> getBatchDetail(@RequestBody List<Long> ids);
}
