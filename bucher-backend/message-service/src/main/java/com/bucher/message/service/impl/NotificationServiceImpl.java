package com.bucher.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bucher.message.entity.Notification;
import com.bucher.message.mapper.NotificationMapper;
import com.bucher.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Notification> listAndReadAll(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = Wrappers.lambdaQuery(Notification.class)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsDeleted, 0)
                .orderByDesc(Notification::getCreateTime);
        List<Notification> list = notificationMapper.selectList(wrapper);

        readAll(userId);

        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll(Long userId) {
        Notification update = new Notification();
        update.setIsRead(true);
        LambdaQueryWrapper<Notification> unreadWrapper = Wrappers.lambdaQuery(Notification.class)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .eq(Notification::getIsDeleted, 0);
        notificationMapper.update(update, unreadWrapper);
    }

    @Override
    public Long unreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = Wrappers.lambdaQuery(Notification.class)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .eq(Notification::getIsDeleted, 0);
        return notificationMapper.selectCount(wrapper);
    }
}
