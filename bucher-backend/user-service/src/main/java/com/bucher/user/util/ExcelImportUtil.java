package com.bucher.user.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.user.dto.StudentImportDTO;
import com.bucher.user.dto.TeacherImportDTO;
import com.bucher.user.entity.User;
import com.bucher.user.mapper.UserMapper;
import com.bucher.user.vo.ImportErrorVO;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

/**
 * Excel 导入工具类 — 行级校验 + 构建 User 实体
 */
public class ExcelImportUtil {

    public static User validateAndBuildStudent(StudentImportDTO dto, int row,
                                                Long adminClassId, Long deptId,
                                                PasswordEncoder passwordEncoder,
                                                String defaultPassword,
                                                UserMapper userMapper,
                                                List<ImportErrorVO> errors,
                                                Set<String> seenUserNos) {
        if (!seenUserNos.add(dto.getUserNo())) {
            errors.add(ImportErrorVO.builder().row(row).userNo(dto.getUserNo())
                    .reason("学号在导入文件中重复").build());
            return null;
        }

        User existByNo = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUserNo, dto.getUserNo()));
        if (existByNo != null) {
            errors.add(ImportErrorVO.builder().row(row).userNo(dto.getUserNo())
                    .reason("学号已被使用").build());
            return null;
        }

        User user = new User();
        user.setUserNo(dto.getUserNo());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setRole(3);
        user.setDeptId(deptId);
        user.setAdminClassId(adminClassId);
        user.setSource(1);
        user.setStatus(1);
        user.setPwdReset(0);
        return user;
    }

    public static User validateAndBuildTeacher(TeacherImportDTO dto, int row,
                                                Long deptId,
                                                PasswordEncoder passwordEncoder,
                                                String defaultPassword,
                                                UserMapper userMapper,
                                                List<ImportErrorVO> errors,
                                                Set<String> seenUserNos) {
        if (!seenUserNos.add(dto.getUserNo())) {
            errors.add(ImportErrorVO.builder().row(row).userNo(dto.getUserNo())
                    .reason("工号在导入文件中重复").build());
            return null;
        }

        User existByNo = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUserNo, dto.getUserNo()));
        if (existByNo != null) {
            errors.add(ImportErrorVO.builder().row(row).userNo(dto.getUserNo())
                    .reason("工号已被使用").build());
            return null;
        }

        User user = new User();
        user.setUserNo(dto.getUserNo());
        user.setRealName(dto.getRealName());
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setRole(2);
        user.setDeptId(deptId);
        user.setSource(1);
        user.setStatus(1);
        user.setPwdReset(0);
        return user;
    }
}
