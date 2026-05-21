package com.bucher.message.controller;

import com.bucher.common.result.Result;
import com.bucher.message.entity.Notification;
import com.bucher.message.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知管理 Controller
 */
@Tag(name = "通知管理")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "获取通知列表（按时间倒序），并自动将所有未读标记为已读")
    @GetMapping
    public Result<List<Notification>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notificationService.listAndReadAll(userId));
    }

    @Operation(summary = "获取未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notificationService.unreadCount(userId));
    }

    @Operation(summary = "将所有未读通知标记为已读")
    @PutMapping("/read")
    public Result<Void> readAll(@RequestHeader("X-User-Id") Long userId) {
        notificationService.readAll(userId);
        return Result.success(null);
    }
}
