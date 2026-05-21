package com.bucher.course.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程信息视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseVO {

    private Long id;

    private String name;

    private Long teacherId;

    private String semester;

    private String description;

    private String coverUrl;

    private String courseCode;

    private Integer status;

    private Integer classCount;

    private LocalDateTime createTime;
}
