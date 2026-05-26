package com.bucher.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.ai.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 配置 Mapper
 */
@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {
}
