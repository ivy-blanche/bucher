package com.bucher.exam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 考试服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.bucher.common", "com.bucher.exam"})
public class ExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}
