package com.bucher.message;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 消息服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.bucher.common", "com.bucher.message"})
@EnableFeignClients
@EnableRabbit
public class MessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageApplication.class, args);
    }
}
