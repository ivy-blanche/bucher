package com.bucher.ai.service;

import com.bucher.ai.dto.AiConfigDTO;
import com.bucher.ai.vo.AiConfigVO;

/**
 * AI 配置服务接口
 */
public interface AiConfigService {

    /**
     * 保存 AI 配置（全局唯一，有则更新，无则新增）
     */
    void save(AiConfigDTO dto);

    /**
     * 获取当前 AI 配置
     */
    AiConfigVO get();
}
