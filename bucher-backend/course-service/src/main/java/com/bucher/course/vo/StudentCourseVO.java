package com.bucher.course.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生已选课程视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseVO {

    private Long id;

    private String name;

    private Long teacherId;

    private String teacherName;

    private String semester;
}
