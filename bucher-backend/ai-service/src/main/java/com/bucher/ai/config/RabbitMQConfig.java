package com.bucher.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * RabbitMQ 配置 — 异步文档向量化
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EXCHANGE_AI = "ai.exchange";
    public static final String QUEUE_DOCUMENT_VECTORIZE = "ai.document.vectorize";
    public static final String ROUTING_KEY_VECTORIZE = "ai.document.vectorize";
    public static final String DLX = "ai.dlx";
    public static final String DLQ = "ai.document.vectorize.dlq";

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(EXCHANGE_AI);
    }

    @Bean
    public DirectExchange aiDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue documentVectorizeQueue() {
        return QueueBuilder.durable(QUEUE_DOCUMENT_VECTORIZE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(ROUTING_KEY_VECTORIZE + ".dlq")
                .build();
    }

    @Bean
    public Queue documentVectorizeDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding vectorizeBinding() {
        return BindingBuilder.bind(documentVectorizeQueue())
                .to(aiExchange())
                .with(ROUTING_KEY_VECTORIZE);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(documentVectorizeDlq())
                .to(aiDlx())
                .with(ROUTING_KEY_VECTORIZE + ".dlq");
    }
}
