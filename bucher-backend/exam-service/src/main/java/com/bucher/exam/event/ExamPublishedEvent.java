package com.bucher.exam.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试发布事件
 */
@Data
@AllArgsConstructor
public class ExamPublishedEvent implements Serializable {

    private Long examId;
    private Long courseId;
    private Long teacherId;
    private List<Long> classIds;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
