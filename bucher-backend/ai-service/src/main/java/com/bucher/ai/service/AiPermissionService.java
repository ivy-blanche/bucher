package com.bucher.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bucher.ai.vo.AiPermissionVO;

/**
 * AI 权限服务接口
 */
public interface AiPermissionService {

    /**
     * 授权教师使用 AI
     */
    void grant(Long teacherId, Long adminId);

    /**
     * 撤销教师 AI 权限
     */
    void revoke(Long teacherId);

    /**
     * 分页查询已授权教师列表
     */
    Page<AiPermissionVO> list(Integer page, Integer size);

    /**
     * 判断教师是否已被授权 AI 权限
     */
    Boolean hasPermission(Long teacherId);
}
