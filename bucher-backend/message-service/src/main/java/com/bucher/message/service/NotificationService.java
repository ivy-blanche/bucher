package com.bucher.message.service;

import com.bucher.message.entity.Notification;

import java.util.List;

/**
 * 通知 Service 接口
 */
public interface NotificationService {

    /**
     * 获取用户通知列表（按时间倒序），并将所有未读通知标记为已读
     */
    List<Notification> listAndReadAll(Long userId);

    /**
     * 获取用户未读通知数
     */
    Long unreadCount(Long userId);

    /**
     * 将所有未读通知标记为已读
     */
    void readAll(Long userId);
}
