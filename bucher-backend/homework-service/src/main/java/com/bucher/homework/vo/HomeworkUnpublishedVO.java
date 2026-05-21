package com.bucher.homework.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未发布作业 VO
 */
@Data
public class HomeworkUnpublishedVO {

    private Long id;

    private String title;

    private String courseName;

    private LocalDateTime deadline;
}
