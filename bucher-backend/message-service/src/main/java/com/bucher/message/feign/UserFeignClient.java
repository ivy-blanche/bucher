package com.bucher.message.feign;

import com.bucher.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/internal/users/{id}/name")
    Result<String> getUserNameById(@PathVariable("id") Long id);
}
