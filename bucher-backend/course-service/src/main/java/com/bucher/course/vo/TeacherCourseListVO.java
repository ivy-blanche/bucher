package com.bucher.course.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教师课程列表-轻量视图（仅返回前端所需字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseListVO {

    private Long id;

    private String name;

    private String semester;

    private String courseCode;
}
