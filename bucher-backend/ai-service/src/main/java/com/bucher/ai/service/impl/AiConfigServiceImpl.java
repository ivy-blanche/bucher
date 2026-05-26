package com.bucher.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bucher.ai.dto.AiConfigDTO;
import com.bucher.ai.entity.AiConfig;
import com.bucher.ai.mapper.AiConfigMapper;
import com.bucher.ai.service.AiConfigService;
import com.bucher.ai.vo.AiConfigVO;
import com.bucher.common.exception.BusinessException;
import com.bucher.common.enums.ResultCodeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 配置服务实现
 */
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {

    private final AiConfigMapper aiConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AiConfigDTO dto) {
        AiConfig existing = aiConfigMapper.selectOne(Wrappers.<AiConfig>lambdaQuery()
                .eq(AiConfig::getIsDeleted, 0));

        if (existing != null) {
            // 更新现有配置
            BeanUtils.copyProperties(dto, existing);
            aiConfigMapper.updateById(existing);
        } else {
            // 新增配置
            AiConfig config = new AiConfig();
            BeanUtils.copyProperties(dto, config);
            aiConfigMapper.insert(config);
        }
    }

    @Override
    public AiConfigVO get() {
        AiConfig config = aiConfigMapper.selectOne(Wrappers.<AiConfig>lambdaQuery()
                .eq(AiConfig::getIsDeleted, 0));

        if (config == null) {
            return null;
        }

        AiConfigVO vo = new AiConfigVO();
        BeanUtils.copyProperties(config, vo);
        // API 密钥脱敏
        vo.setApiKey(maskApiKey(config.getApiKey()));
        vo.setEmbeddingApiKey(maskApiKey(config.getEmbeddingApiKey()));
        return vo;
    }

    /**
     * API 密钥脱敏：保留前 8 位，替换为 ****
     */
    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return apiKey;
        }
        if (apiKey.length() <= 8) {
            return apiKey.substring(0, 1) + "****";
        }
        return apiKey.substring(0, 8) + "****";
    }
}
