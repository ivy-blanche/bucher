package com.bucher.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.ai.entity.AiTeacherPermission;
import com.bucher.ai.mapper.AiTeacherPermissionMapper;
import com.bucher.ai.service.AiPermissionService;
import com.bucher.ai.vo.AiPermissionVO;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * AI 权限服务实现
 */
@Service
@RequiredArgsConstructor
public class AiPermissionServiceImpl implements AiPermissionService {

    private final AiTeacherPermissionMapper permissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long teacherId, Long adminId) {

        // 查找已有记录（含逻辑删除）
        AiTeacherPermission existing = permissionMapper.selectOne(
                new LambdaQueryWrapper<AiTeacherPermission>()
                        .eq(AiTeacherPermission::getTeacherId, teacherId));

        if (existing != null) {
            if (existing.getStatus() == 1 && existing.getIsDeleted() == 0) {
                throw new BusinessException(ResultCodeEnum.AI_PERMISSION_ALREADY_EXISTS);
            }
            // 恢复已撤销/已删除的记录
            existing.setStatus(1);
            existing.setGrantedBy(adminId);
            existing.setIsDeleted(0);
            permissionMapper.updateById(existing);
        } else {
            // 新增授权
            AiTeacherPermission permission = new AiTeacherPermission();
            permission.setTeacherId(teacherId);
            permission.setStatus(1);
            permission.setGrantedBy(adminId);
            permissionMapper.insert(permission);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long teacherId) {
        AiTeacherPermission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<AiTeacherPermission>()
                        .eq(AiTeacherPermission::getTeacherId, teacherId)
                        .eq(AiTeacherPermission::getIsDeleted, 0));

        if (permission == null) {
            throw new BusinessException(ResultCodeEnum.AI_PERMISSION_NOT_FOUND);
        }

        permission.setStatus(0);
        permissionMapper.deleteById(permission.getId());
    }

    @Override
    public Boolean hasPermission(Long teacherId) {
        AiTeacherPermission permission = permissionMapper.selectOne(
                new LambdaQueryWrapper<AiTeacherPermission>()
                        .eq(AiTeacherPermission::getTeacherId, teacherId)
                        .eq(AiTeacherPermission::getStatus, 1));
        return permission != null;
    }

    @Override
    public Page<AiPermissionVO> list(Integer page, Integer size) {
        Page<AiTeacherPermission> permissionPage = permissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AiTeacherPermission>()
                        .eq(AiTeacherPermission::getIsDeleted, 0)
                        .orderByDesc(AiTeacherPermission::getCreateTime));

        Page<AiPermissionVO> voPage = new Page<>(permissionPage.getCurrent(),
                permissionPage.getSize(), permissionPage.getTotal());
        voPage.setRecords(permissionPage.getRecords().stream().map(p -> {
            AiPermissionVO vo = new AiPermissionVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).collect(Collectors.toList()));

        return voPage;
    }
}
