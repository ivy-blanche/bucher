package com.bucher.homework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.homework.entity.Homework;
import com.bucher.homework.vo.HomeworkListVO;
import com.bucher.homework.vo.StudentHomeworkVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作业 Mapper
 */
@Mapper
public interface HomeworkMapper extends BaseMapper<Homework> {

    /**
     * 分页查询教师作业列表
     */
    IPage<HomeworkListVO> selectTeacherHomeworkList(Page<HomeworkListVO> page,
                                                    @Param("teacherId") Long teacherId,
                                                    @Param("filter") String filter);

    /**
     * 查询学生端作业列表
     */
    List<StudentHomeworkVO> selectStudentHomeworkList(@Param("courseId") Long courseId,
                                                      @Param("studentId") Long studentId,
                                                      @Param("classIds") List<Long> classIds);
}
