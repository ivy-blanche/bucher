package com.bucher.ai.feign;

import com.bucher.common.result.Result;
import com.bucher.common.vo.TeacherSearchVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    /**
     * 根据工号搜索教师
     */
    @GetMapping("/internal/teachers/search")
    Result<List<TeacherSearchVO>> searchTeachersByUserNo(@RequestParam("keyword") String keyword);

    /**
     * 根据 ID 获取用户姓名
     */
    @GetMapping("/internal/users/{id}/name")
    Result<String> getUserNameById(@PathVariable("id") Long id);
}
