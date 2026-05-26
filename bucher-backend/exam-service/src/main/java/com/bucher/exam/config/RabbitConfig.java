package com.bucher.exam.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置（考试通知）
 */
@Configuration
public class RabbitConfig {

    public static final String EXAM_NOTIFICATION_QUEUE = "notification.exam.queue";
    public static final String EXAM_NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String EXAM_NOTIFICATION_ROUTING_KEY = "notification.exam";

    @Bean
    public Queue examNotificationQueue() {
        return new Queue(EXAM_NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding examNotificationBinding() {
        return BindingBuilder.bind(examNotificationQueue())
                .to(new DirectExchange(EXAM_NOTIFICATION_EXCHANGE))
                .with(EXAM_NOTIFICATION_ROUTING_KEY);
    }
}
