package com.bucher.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.exam.entity.Exam;
import com.bucher.exam.vo.ExamListVO;
import com.bucher.exam.vo.StudentExamVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试 Mapper
 */
@Mapper
public interface ExamMapper extends BaseMapper<Exam> {

    IPage<ExamListVO> selectTeacherExamList(Page<ExamListVO> page,
                                            @Param("teacherId") Long teacherId,
                                            @Param("filter") String filter);

    List<StudentExamVO> selectStudentExamList(@Param("courseId") Long courseId,
                                              @Param("studentId") Long studentId,
                                              @Param("classIds") List<Long> classIds);

    Integer countSubmitByExamId(@Param("examId") Long examId);
}
