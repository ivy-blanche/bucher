package com.bucher.course.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程班级成员视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseClassMemberVO {

    private Long id;

    private Long studentId;

    private String studentNo;

    private String studentName;

    private Integer joinType;

    private LocalDateTime joinTime;

    private Integer status;
}
