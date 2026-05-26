package com.bucher.ai.feign;

import com.bucher.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 课程服务 Feign 客户端
 */
@FeignClient(name = "course-service")
public interface CourseFeignClient {

    /**
     * 获取课程名称
     */
    @GetMapping("/internal/courses/{id}/name")
    Result<String> getCourseNameById(@PathVariable("id") Long id);
}
