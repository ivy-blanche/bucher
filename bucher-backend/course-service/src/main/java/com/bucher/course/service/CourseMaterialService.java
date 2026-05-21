package com.bucher.course.service;

import com.bucher.course.vo.CourseMaterialVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程资料服务接口
 */
public interface CourseMaterialService {

    /**
     * 批量下载课程资料
     */
    void batchDownloadMaterials(List<Long> materialIds, Long userId, Integer role, HttpServletResponse response);

    /**
     * 教师上传课程资料
     */
    CourseMaterialVO uploadMaterial(Long courseId, MultipartFile file, Long teacherId, Integer role);

    /**
     * 获取课程资料列表（教师/学生通用）
     */
    List<CourseMaterialVO> listMaterials(Long courseId, Long userId, Integer role);

    /**
     * 下载课程资料
     */
    void downloadMaterial(Long materialId, Long userId, Integer role, HttpServletResponse response);

    /**
     * 教师删除课程资料
     */
    void deleteMaterial(Long materialId, Long teacherId, Integer role);
}
