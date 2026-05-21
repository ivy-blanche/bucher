package com.bucher.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.message.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
