package com.bucher.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.user.dto.*;
import com.bucher.user.entity.Department;
import com.bucher.user.vo.*;
import org.springframework.web.multipart.MultipartFile;

public interface AdminService {

    // Department
    Page<DepartmentPageVO> listDepartments(Integer page, Integer size, String name, Integer type);

    Department getDepartmentById(Long id);

    DepartmentVO createDepartment(DepartmentCreateDTO dto);

    DepartmentVO updateDepartment(Long id, DepartmentCreateDTO dto);

    void deleteDepartment(Long id);

    // AdminClass
    Page<AdminClassVO> listAdminClasses(Integer page, Integer size, Long deptId, String name);

    AdminClassVO getAdminClassById(Long id);

    AdminClassVO createAdminClass(AdminClassCreateDTO dto);

    AdminClassVO updateAdminClass(Long id, AdminClassCreateDTO dto);

    void deleteAdminClass(Long id);

    // User list
    Page<UserPageVO> listUsers(Integer page, Integer size, Integer role, Long deptId,
                               Long adminClassId, String keyword);

    // Import
    ImportResultVO importStudents(Long adminClassId, MultipartFile file);

    ImportResultVO importTeachers(Long deptId, MultipartFile file);

    // Password reset
    void resetUserPassword(Long userId, Long operatorId);
}
