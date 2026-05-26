package com.bucher.exam.feign;

import com.bucher.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * course-service Feign 客户端
 */
@FeignClient(name = "course-service", path = "/internal")
public interface CourseServiceClient {

    @GetMapping("/course-classes/student-classes")
    Result<List<Long>> getStudentCourseClassIds(@RequestParam("studentId") Long studentId,
                                                @RequestParam("courseId") Long courseId);
}
