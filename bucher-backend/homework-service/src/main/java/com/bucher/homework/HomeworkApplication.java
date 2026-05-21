package com.bucher.homework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 作业服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.bucher.common", "com.bucher.homework"})
@EnableFeignClients
public class HomeworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeworkApplication.class, args);
    }
}
