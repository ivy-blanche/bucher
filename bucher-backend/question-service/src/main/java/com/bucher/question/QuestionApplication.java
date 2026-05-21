package com.bucher.question;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 题库服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.bucher.common", "com.bucher.question"})
public class QuestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestionApplication.class, args);
    }
}
