package com.bucher.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 厂商枚举
 */
@Getter
@AllArgsConstructor
public enum AiProviderEnum {

    DEEPSEEK("DEEPSEEK", "DeepSeek"),
    OPENAI("OPENAI", "OpenAI"),
    QIANFAN("QIANFAN", "百度千帆"),
    TONGYI("TONGYI", "阿里通义"),
    ZHIPU("ZHIPU", "智谱GLM");

    private final String code;
    private final String desc;

    public static AiProviderEnum getByCode(String code) {
        for (AiProviderEnum provider : values()) {
            if (provider.getCode().equals(code)) {
                return provider;
            }
        }
        return null;
    }
}
