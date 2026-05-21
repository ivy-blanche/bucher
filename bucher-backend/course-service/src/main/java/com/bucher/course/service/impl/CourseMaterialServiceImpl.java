package com.bucher.course.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.course.constant.MaterialConstants;
import com.bucher.course.entity.Course;
import com.bucher.course.entity.CourseEnrollment;
import com.bucher.course.entity.CourseMaterial;
import com.bucher.course.mapper.CourseEnrollmentMapper;
import com.bucher.course.mapper.CourseMapper;
import com.bucher.course.mapper.CourseMaterialMapper;
import com.bucher.course.service.CourseMaterialService;
import com.bucher.course.util.MinioUtil;
import com.bucher.course.vo.CourseMaterialVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 课程资料服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMaterialServiceImpl implements CourseMaterialService {

    private final CourseMaterialMapper courseMaterialMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseMaterialVO uploadMaterial(Long courseId, MultipartFile file, Long teacherId, Integer role) {
        // 校验教师角色
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        // 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.UPLOAD_FILE_EMPTY);
        }

        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String fileExt = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            fileExt = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!MaterialConstants.ALLOWED_EXTENSIONS.contains(fileExt)) {
            throw new BusinessException(ResultCodeEnum.MATERIAL_FILE_TYPE_INVALID);
        }

        // 校验课程存在性及归属
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
        }

        // 生成雪花ID（用于主键和MinIO对象名）
        Long materialId = IdUtil.getSnowflakeNextId();
        String objectName = "course/" + courseId + "/" + materialId + "_" + originalFilename;

        // 上传到 MinIO
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        try (InputStream inputStream = file.getInputStream()) {
            minioUtil.putObject(objectName, inputStream, contentType, file.getSize());
        } catch (IOException e) {
            log.error("读取上传文件流失败: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.MATERIAL_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            // MinIO 上传失败
            throw new BusinessException(ResultCodeEnum.MATERIAL_UPLOAD_FAILED);
        }

        // 写入数据库
        CourseMaterial material = new CourseMaterial();
        material.setId(materialId);
        material.setCourseId(courseId);
        material.setTeacherId(teacherId);
        material.setFileName(originalFilename);
        material.setFileSize(file.getSize());
        material.setFileType(contentType);
        material.setFileExt(fileExt);
        material.setObjectName(objectName);
        material.setBucketName("course-bucket");
        material.setStatus(1);
        courseMaterialMapper.insert(material);

        log.info("教师上传课程资料成功: teacherId={}, courseId={}, file={}, size={}",
                teacherId, courseId, originalFilename, file.getSize());

        return toVO(material);
    }

    @Override
    public List<CourseMaterialVO> listMaterials(Long courseId, Long userId, Integer role) {
        // 校验课程存在
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }

        // 权限校验
        if (Integer.valueOf(2).equals(role)) {
            // 教师只能看自己的课程
            if (!course.getTeacherId().equals(userId)) {
                throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
            }
        } else {
            // 学生必须已选课
            Long count = courseEnrollmentMapper.selectCount(
                    new LambdaQueryWrapper<CourseEnrollment>()
                            .eq(CourseEnrollment::getCourseId, courseId)
                            .eq(CourseEnrollment::getStudentId, userId)
                            .eq(CourseEnrollment::getStatus, 1)
            );
            if (count == null || count == 0) {
                throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
            }
        }

        // 查询资料列表（仅正常状态）
        List<CourseMaterial> materials = courseMaterialMapper.selectList(
                new LambdaQueryWrapper<CourseMaterial>()
                        .eq(CourseMaterial::getCourseId, courseId)
                        .eq(CourseMaterial::getStatus, 1)
                        .orderByDesc(CourseMaterial::getCreateTime)
        );

        return materials.stream().map(this::toVO).toList();
    }

    @Override
    public void downloadMaterial(Long materialId, Long userId, Integer role, HttpServletResponse response) {
        // 查询资料
        CourseMaterial material = courseMaterialMapper.selectById(materialId);
        if (material == null || !Integer.valueOf(1).equals(material.getStatus())) {
            throw new BusinessException(ResultCodeEnum.MATERIAL_NOT_FOUND);
        }

        // 查询对应课程
        Course course = courseMapper.selectById(material.getCourseId());
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }

        // 权限校验（同列表接口）
        if (Integer.valueOf(2).equals(role)) {
            if (!course.getTeacherId().equals(userId)) {
                throw new BusinessException(ResultCodeEnum.MATERIAL_FORBIDDEN);
            }
        } else {
            Long count = courseEnrollmentMapper.selectCount(
                    new LambdaQueryWrapper<CourseEnrollment>()
                            .eq(CourseEnrollment::getCourseId, material.getCourseId())
                            .eq(CourseEnrollment::getStudentId, userId)
                            .eq(CourseEnrollment::getStatus, 1)
            );
            if (count == null || count == 0) {
                throw new BusinessException(ResultCodeEnum.MATERIAL_FORBIDDEN);
            }
        }

        // 大文件或视频使用预签名 URL 重定向
        boolean usePresignedUrl = material.getFileSize() > MaterialConstants.PRESIGNED_URL_THRESHOLD
                || MaterialConstants.VIDEO_EXTENSIONS.contains(material.getFileExt());

        if (usePresignedUrl) {
            String signedUrl = minioUtil.getPresignedObjectUrl(
                    material.getObjectName(), MaterialConstants.PRESIGNED_URL_EXPIRY);
            try {
                response.sendRedirect(signedUrl);
            } catch (IOException e) {
                log.error("重定向到预签名URL失败: {}", e.getMessage());
                throw new BusinessException(ResultCodeEnum.MATERIAL_UPLOAD_FAILED);
            }
            return;
        }

        // 小文件直接流式传输
        try (InputStream inputStream = minioUtil.getObject(material.getObjectName());
             OutputStream outputStream = response.getOutputStream()) {

            String encodedFileName = URLEncoder.encode(material.getFileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType(material.getFileType());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(material.getFileSize());

            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件下载流式传输失败: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.MATERIAL_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(Long materialId, Long teacherId, Integer role) {
        // 校验教师角色
        if (!Integer.valueOf(2).equals(role)) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN_OPERATION);
        }

        // 查询资料
        CourseMaterial material = courseMaterialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(ResultCodeEnum.MATERIAL_NOT_FOUND);
        }

        // 校验课程归属
        Course course = courseMapper.selectById(material.getCourseId());
        if (course == null || !course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.MATERIAL_FORBIDDEN);
        }

        // 删除 MinIO 文件
        try {
            minioUtil.removeObject(material.getObjectName());
        } catch (RuntimeException e) {
            log.warn("MinIO 删除文件失败，将继续软删除数据库记录: objectName={}", material.getObjectName());
        }

        // 软删除数据库记录
        courseMaterialMapper.deleteById(materialId);

        log.info("教师删除课程资料成功: teacherId={}, materialId={}, file={}",
                teacherId, materialId, material.getFileName());
    }

    @Override
    public void batchDownloadMaterials(List<Long> materialIds, Long userId, Integer role, HttpServletResponse response) {
        if (materialIds == null || materialIds.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST);
        }

        // 查询所有资料
        List<CourseMaterial> materials = courseMaterialMapper.selectBatchIds(materialIds);
        if (materials.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.MATERIAL_NOT_FOUND);
        }

        // 预校验所有资料的权限
        for (CourseMaterial material : materials) {
            if (!Integer.valueOf(1).equals(material.getStatus())) {
                throw new BusinessException(ResultCodeEnum.MATERIAL_NOT_FOUND);
            }

            Course course = courseMapper.selectById(material.getCourseId());
            if (course == null) {
                throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
            }

            if (Integer.valueOf(2).equals(role)) {
                if (!course.getTeacherId().equals(userId)) {
                    throw new BusinessException(ResultCodeEnum.MATERIAL_FORBIDDEN);
                }
            } else {
                Long count = courseEnrollmentMapper.selectCount(
                        new LambdaQueryWrapper<CourseEnrollment>()
                                .eq(CourseEnrollment::getCourseId, material.getCourseId())
                                .eq(CourseEnrollment::getStudentId, userId)
                                .eq(CourseEnrollment::getStatus, 1)
                );
                if (count == null || count == 0) {
                    throw new BusinessException(ResultCodeEnum.MATERIAL_FORBIDDEN);
                }
            }
        }

        // 设置 ZIP 响应头
        String zipFileName = URLEncoder.encode("course-materials.zip", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + zipFileName);

        // 流式写入 ZIP
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[8192];

            for (CourseMaterial material : materials) {
                try (InputStream in = minioUtil.getObject(material.getObjectName())) {
                    zos.putNextEntry(new ZipEntry(material.getFileName()));

                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                    log.debug("已打包: materialId={}, fileName={}", material.getId(), material.getFileName());
                } catch (Exception e) {
                    log.warn("打包文件失败，已跳过: materialId={}, fileName={}, error={}",
                            material.getId(), material.getFileName(), e.getMessage());
                    continue;
                }
            }
            zos.flush();
        } catch (IOException e) {
            log.error("批量下载ZIP打包失败: {}", e.getMessage());
            throw new BusinessException(ResultCodeEnum.MATERIAL_UPLOAD_FAILED);
        }

        log.info("批量下载成功: userId={}, role={}, materialCount={}", userId, role, materials.size());
    }

    private CourseMaterialVO toVO(CourseMaterial material) {
        return CourseMaterialVO.builder()
                .id(material.getId())
                .courseId(material.getCourseId())
                .fileName(material.getFileName())
                .fileSize(material.getFileSize())
                .fileType(material.getFileType())
                .fileExt(material.getFileExt())
                .duration(material.getDuration())
                .createTime(material.getCreateTime())
                .build();
    }
}
