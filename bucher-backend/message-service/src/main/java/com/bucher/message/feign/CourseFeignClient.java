package com.bucher.message.feign;

import com.bucher.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 课程服务 Feign 客户端
 */
@FeignClient(name = "course-service")
public interface CourseFeignClient {

    @GetMapping("/internal/courses/{id}/name")
    Result<String> getCourseNameById(@PathVariable("id") Long id);

    @GetMapping("/internal/course-classes/members")
    Result<List<Long>> getClassMemberIds(@RequestParam("classIds") List<Long> classIds);
}
