package com.bucher.message.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 通知交换机与队列
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "notification.exchange";
    public static final String HOMEWORK_QUEUE = "notification.homework.queue";
    public static final String EXAM_QUEUE = "notification.exam.queue";
    public static final String HOMEWORK_ROUTING_KEY = "notification.homework";
    public static final String EXAM_ROUTING_KEY = "notification.exam";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue homeworkQueue() {
        return QueueBuilder.durable(HOMEWORK_QUEUE).build();
    }

    @Bean
    public Queue examQueue() {
        return QueueBuilder.durable(EXAM_QUEUE).build();
    }

    @Bean
    public Binding homeworkBinding() {
        return BindingBuilder.bind(homeworkQueue())
                .to(notificationExchange())
                .with(HOMEWORK_ROUTING_KEY);
    }

    @Bean
    public Binding examBinding() {
        return BindingBuilder.bind(examQueue())
                .to(notificationExchange())
                .with(EXAM_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
