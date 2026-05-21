package com.bucher.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.user.dto.*;
import com.bucher.user.entity.*;
import com.bucher.user.enums.UserRoleEnum;
import com.bucher.user.mapper.*;
import com.bucher.user.service.AdminService;
import com.bucher.user.util.ExcelImportUtil;
import com.bucher.user.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final DepartmentMapper departmentMapper;
    private final AdminClassMapper adminClassMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${user.default-password:Zx@123456}")
    private String defaultPassword;

    // ──────── Department CRUD ────────

    @Override
    public Page<DepartmentPageVO> listDepartments(Integer page, Integer size, String name, Integer type) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(Department::getName, name);
        }
        if (type != null) {
            wrapper.eq(Department::getType, type);
        }
        wrapper.orderByDesc(Department::getCreateTime);
        Page<Department> entityPage = departmentMapper.selectPage(new Page<>(page, size), wrapper);

        List<Department> records = entityPage.getRecords();
        if (records.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        Set<Long> deptIds = records.stream().map(Department::getId).collect(Collectors.toSet());

        // 统计各院系下的行政班级数
        Map<Long, Long> classCountMap = deptIds.isEmpty() ? Collections.emptyMap()
                : adminClassMapper.selectMaps(new QueryWrapper<AdminClass>()
                        .select("dept_id, COUNT(*) as cnt")
                        .in("dept_id", deptIds)
                        .groupBy("dept_id"))
                .stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("dept_id"),
                        m -> ((Number) m.get("cnt")).longValue()));

        // 统计各院系下的学生数
        Map<Long, Long> studentCountMap = deptIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectMaps(new QueryWrapper<User>()
                        .select("dept_id, COUNT(*) as cnt")
                        .in("dept_id", deptIds)
                        .eq("role", UserRoleEnum.STUDENT.getCode())
                        .groupBy("dept_id"))
                .stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("dept_id"),
                        m -> ((Number) m.get("cnt")).longValue()));

        // 统计各院系下的教师数
        Map<Long, Long> teacherCountMap = deptIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectMaps(new QueryWrapper<User>()
                        .select("dept_id, COUNT(*) as cnt")
                        .in("dept_id", deptIds)
                        .eq("role", UserRoleEnum.TEACHER.getCode())
                        .groupBy("dept_id"))
                .stream()
                .collect(Collectors.toMap(
                        m -> (Long) m.get("dept_id"),
                        m -> ((Number) m.get("cnt")).longValue()));

        Page<DepartmentPageVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(records.stream().map(dept -> {
            DepartmentPageVO vo = new DepartmentPageVO();
            vo.setId(dept.getId());
            vo.setName(dept.getName());
            vo.setDeptCode(dept.getDeptCode());
            vo.setType(dept.getType());
            vo.setDescription(dept.getDescription());
            vo.setCreateTime(dept.getCreateTime());
            vo.setClassCount(classCountMap.getOrDefault(dept.getId(), 0L));
            vo.setStudentCount(studentCountMap.getOrDefault(dept.getId(), 0L));
            vo.setTeacherCount(teacherCountMap.getOrDefault(dept.getId(), 0L));
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Department getDepartmentById(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }
        return dept;
    }

    @Override
    public DepartmentVO createDepartment(DepartmentCreateDTO dto) {
        Department dept = new Department();
        dept.setName(dto.getName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setType(dto.getType());
        dept.setDescription(dto.getDescription());
        departmentMapper.insert(dept);

        DepartmentVO vo = new DepartmentVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setDeptCode(dept.getDeptCode());
        vo.setType(dept.getType());
        vo.setDescription(dept.getDescription());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentVO updateDepartment(Long id, DepartmentCreateDTO dto) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }
        dept.setName(dto.getName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setType(dto.getType());
        dept.setDescription(dto.getDescription());
        departmentMapper.updateById(dept);

        DepartmentVO vo = new DepartmentVO();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setDeptCode(dept.getDeptCode());
        vo.setType(dept.getType());
        vo.setDescription(dept.getDescription());
        vo.setCreateTime(dept.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }
        Long classCount = adminClassMapper.selectCount(
                new LambdaQueryWrapper<AdminClass>().eq(AdminClass::getDeptId, id));
        if (classCount > 0) {
            throw new BusinessException("该院系下存在行政班级，无法删除");
        }
        departmentMapper.deleteById(id);
    }

    // ──────── AdminClass CRUD ────────

    @Override
    public Page<AdminClassVO> listAdminClasses(Integer page, Integer size, Long deptId, String name) {
        LambdaQueryWrapper<AdminClass> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(AdminClass::getDeptId, deptId);
        }
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(AdminClass::getName, name);
        }
        wrapper.orderByDesc(AdminClass::getYear).orderByAsc(AdminClass::getName);
        Page<AdminClass> entityPage = adminClassMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> deptIds = entityPage.getRecords().stream()
                .map(AdminClass::getDeptId).collect(Collectors.toSet());
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getName));

        Page<AdminClassVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(ac -> {
            AdminClassVO vo = new AdminClassVO();
            vo.setId(ac.getId());
            vo.setName(ac.getName());
            vo.setDeptId(ac.getDeptId());
            vo.setDeptName(deptNameMap.getOrDefault(ac.getDeptId(), ""));
            vo.setYear(ac.getYear());
            vo.setCreateTime(ac.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public AdminClassVO getAdminClassById(Long id) {
        AdminClass ac = adminClassMapper.selectById(id);
        if (ac == null) {
            throw new BusinessException(ResultCodeEnum.ADMIN_CLASS_NOT_FOUND);
        }
        AdminClassVO vo = new AdminClassVO();
        vo.setId(ac.getId());
        vo.setName(ac.getName());
        vo.setDeptId(ac.getDeptId());
        vo.setYear(ac.getYear());
        vo.setCreateTime(ac.getCreateTime());

        Department dept = departmentMapper.selectById(ac.getDeptId());
        vo.setDeptName(dept != null ? dept.getName() : "");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminClassVO createAdminClass(AdminClassCreateDTO dto) {
        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }

        AdminClass ac = new AdminClass();
        ac.setName(dto.getName());
        ac.setDeptId(dto.getDeptId());
        ac.setYear(dto.getYear());
        adminClassMapper.insert(ac);

        AdminClassVO vo = new AdminClassVO();
        vo.setId(ac.getId());
        vo.setName(ac.getName());
        vo.setDeptId(ac.getDeptId());
        vo.setDeptName(dept.getName());
        vo.setYear(ac.getYear());
        vo.setCreateTime(ac.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminClassVO updateAdminClass(Long id, AdminClassCreateDTO dto) {
        AdminClass ac = adminClassMapper.selectById(id);
        if (ac == null) {
            throw new BusinessException(ResultCodeEnum.ADMIN_CLASS_NOT_FOUND);
        }
        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }

        ac.setName(dto.getName());
        ac.setDeptId(dto.getDeptId());
        ac.setYear(dto.getYear());
        adminClassMapper.updateById(ac);

        AdminClassVO vo = new AdminClassVO();
        vo.setId(ac.getId());
        vo.setName(ac.getName());
        vo.setDeptId(ac.getDeptId());
        vo.setDeptName(dept.getName());
        vo.setYear(ac.getYear());
        vo.setCreateTime(ac.getCreateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdminClass(Long id) {
        AdminClass ac = adminClassMapper.selectById(id);
        if (ac == null) {
            throw new BusinessException(ResultCodeEnum.ADMIN_CLASS_NOT_FOUND);
        }
        Long studentCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getAdminClassId, id));
        if (studentCount > 0) {
            throw new BusinessException(ResultCodeEnum.ADMIN_CLASS_HAS_STUDENTS);
        }
        adminClassMapper.deleteById(id);
    }

    // ──────── User list ────────

    @Override
    public Page<UserPageVO> listUsers(Integer page, Integer size, Integer role,
                                       Long deptId, Long adminClassId, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (deptId != null) {
            wrapper.eq(User::getDeptId, deptId);
        }
        if (adminClassId != null) {
            wrapper.eq(User::getAdminClassId, adminClassId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getRealName, keyword)
                    .or().like(User::getUserNo, keyword)
                    .or().like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> entityPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        Set<Long> deptIds = entityPage.getRecords().stream()
                .map(User::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> classIds = entityPage.getRecords().stream()
                .map(User::getAdminClassId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(deptIds).stream()
                        .collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, String> classNameMap = classIds.isEmpty() ? Collections.emptyMap()
                : adminClassMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(AdminClass::getId, AdminClass::getName));

        Page<UserPageVO> voPage = new Page<>(page, size, entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(u -> {
            UserPageVO vo = new UserPageVO();
            vo.setId(u.getId());
            vo.setUserNo(u.getUserNo());
            vo.setRealName(u.getRealName());
            vo.setEmail(u.getEmail());
            vo.setRole(u.getRole());
            UserRoleEnum roleEnum = UserRoleEnum.getByCode(u.getRole());
            vo.setRoleName(roleEnum != null ? roleEnum.getDesc() : "");
            vo.setDeptName(deptNameMap.get(u.getDeptId()));
            vo.setAdminClassName(classNameMap.get(u.getAdminClassId()));
            vo.setStatus(u.getStatus());
            vo.setPwdReset(u.getPwdReset());
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    // ──────── Import ────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importStudents(Long adminClassId, MultipartFile file) {
        AdminClass ac = adminClassMapper.selectById(adminClassId);
        if (ac == null) {
            throw new BusinessException(ResultCodeEnum.ADMIN_CLASS_NOT_FOUND);
        }
        Department dept = departmentMapper.selectById(ac.getDeptId());
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }

        if (file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.UPLOAD_FILE_EMPTY);
        }

        List<StudentImportDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(StudentImportDTO.class)
                    .sheet().doReadSync();
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new BusinessException(ResultCodeEnum.EXCEL_IMPORT_ERROR);
        }

        if (rows.isEmpty()) {
            throw new BusinessException("Excel 文件中没有数据");
        }

        List<ImportErrorVO> errors = new ArrayList<>();
        List<User> users = new ArrayList<>();
        Set<String> seenUserNos = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            StudentImportDTO row = rows.get(i);
            int rowNum = i + 2;
            User user = ExcelImportUtil.validateAndBuildStudent(
                    row, rowNum, adminClassId, ac.getDeptId(),
                    passwordEncoder, defaultPassword, userMapper,
                    errors, seenUserNos);
            if (user != null) {
                users.add(user);
            }
        }

        if (!errors.isEmpty()) {
            return ImportResultVO.builder()
                    .totalCount(rows.size())
                    .successCount(0)
                    .failCount(errors.size())
                    .errors(errors)
                    .build();
        }

        users.forEach(userMapper::insert);

        log.info("批量导入学生成功: adminClassId={}, count={}", adminClassId, users.size());
        return ImportResultVO.builder()
                .totalCount(rows.size())
                .successCount(users.size())
                .failCount(0)
                .errors(Collections.emptyList())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importTeachers(Long deptId, MultipartFile file) {
        Department dept = departmentMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessException(ResultCodeEnum.DEPARTMENT_NOT_FOUND);
        }

        if (file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.UPLOAD_FILE_EMPTY);
        }

        List<TeacherImportDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(TeacherImportDTO.class)
                    .sheet().doReadSync();
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new BusinessException(ResultCodeEnum.EXCEL_IMPORT_ERROR);
        }

        if (rows.isEmpty()) {
            throw new BusinessException("Excel 文件中没有数据");
        }

        List<ImportErrorVO> errors = new ArrayList<>();
        List<User> users = new ArrayList<>();
        Set<String> seenUserNos = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            TeacherImportDTO row = rows.get(i);
            int rowNum = i + 2;
            User user = ExcelImportUtil.validateAndBuildTeacher(
                    row, rowNum, deptId,
                    passwordEncoder, defaultPassword, userMapper,
                    errors, seenUserNos);
            if (user != null) {
                users.add(user);
            }
        }

        if (!errors.isEmpty()) {
            return ImportResultVO.builder()
                    .totalCount(rows.size())
                    .successCount(0)
                    .failCount(errors.size())
                    .errors(errors)
                    .build();
        }

        users.forEach(userMapper::insert);

        log.info("批量导入教师成功: deptId={}, count={}", deptId, users.size());
        return ImportResultVO.builder()
                .totalCount(rows.size())
                .successCount(users.size())
                .failCount(0)
                .errors(Collections.emptyList())
                .build();
    }

    // ──────── Password reset ────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(Long userId, Long operatorId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeEnum.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setPwdReset(1);
        userMapper.updateById(user);

        log.info("管理员重置用户密码: operatorId={}, targetUserId={}", operatorId, userId);
    }
}
