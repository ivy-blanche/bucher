package com.bucher.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI 服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.bucher")
@MapperScan("com.bucher.ai.mapper")
@EnableFeignClients(basePackages = "com.bucher")
public class  AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
