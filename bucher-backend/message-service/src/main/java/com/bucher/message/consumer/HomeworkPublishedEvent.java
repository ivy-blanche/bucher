package com.bucher.message.consumer;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业发布事件（由 homework-service 发布，message-service 消费）
 */
@Data
public class HomeworkPublishedEvent {
    private Long homeworkId;
    private Long courseId;
    private Long teacherId;
    private List<Long> classIds;
    private String title;
    private LocalDateTime deadline;
}
