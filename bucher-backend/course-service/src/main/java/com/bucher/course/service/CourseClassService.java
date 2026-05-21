package com.bucher.course.service;

import com.bucher.course.dto.CourseClassCreateDTO;
import com.bucher.course.dto.JoinByInviteCodeDTO;
import com.bucher.course.vo.CourseClassMemberVO;
import com.bucher.course.vo.CourseClassVO;

import java.util.List;

/**
 * 课程班级服务接口
 */
public interface CourseClassService {

    /**
     * 创建课程班级
     */
    CourseClassVO createClass(CourseClassCreateDTO dto, Long teacherId);

    /**
     * 更新班级名称
     */
    void updateClassName(Long classId, String name, Long teacherId);

    /**
     * 删除班级
     */
    void deleteClass(Long classId, Long teacherId);

    /**
     * 刷新邀请码
     */
    String regenerateInviteCode(Long classId, Long teacherId);

    /**
     * 学生通过邀请码加入班级
     */
    void joinByInviteCode(JoinByInviteCodeDTO dto, Long studentId);

    /**
     * 教师添加学生到班级
     */
    void addStudent(Long classId, Long studentId, Long teacherId);

    /**
     * 从班级移除学生
     */
    void removeStudent(Long memberId, Long teacherId);

    /**
     * 获取班级成员列表
     */
    List<CourseClassMemberVO> listMembers(Long classId);

    /**
     * 获取课程下的班级列表
     */
    List<CourseClassVO> listCourseClasses(Long courseId);
}
