package com.bucher.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 雪花 ID 工具类
 */
public class SnowflakeIdUtil {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private SnowflakeIdUtil() {
    }

    /**
     * 获取下一个雪花 ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 获取下一个雪花 ID（字符串形式）
     */
    public static String nextIdStr() {
        return String.valueOf(SNOWFLAKE.nextId());
    }
}
