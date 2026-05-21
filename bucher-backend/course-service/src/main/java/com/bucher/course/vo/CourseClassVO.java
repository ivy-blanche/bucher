package com.bucher.course.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程班级视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseClassVO {

    private Long id;

    private Long courseId;

    private String courseName;

    private String name;

    private String inviteCode;

    private LocalDateTime inviteExpireTime;

    private Integer studentCount;

    private Integer status;

    private LocalDateTime createTime;
}
