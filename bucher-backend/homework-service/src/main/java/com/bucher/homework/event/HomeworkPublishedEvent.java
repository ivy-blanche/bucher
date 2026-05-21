package com.bucher.homework.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业发布事件（发送至 RabbitMQ，由 message-service 消费）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkPublishedEvent {
    private Long homeworkId;
    private Long courseId;
    private Long teacherId;
    private List<Long> classIds;
    private String title;
    private LocalDateTime deadline;
}
