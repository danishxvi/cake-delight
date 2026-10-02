package com.cakedelight.notification.controller;

import com.cakedelight.notification.model.NotificationRecord;
import com.cakedelight.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only access to in-app notifications, reached by clients through the
 * API Gateway at /api/notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** The 50 most recent notifications, newest first. */
    @GetMapping
    public List<NotificationRecord> getRecentNotifications() {
        return notificationService.findRecent();
    }

    /** Notifications for one order, newest first. Empty until the event has been processed. */
    @GetMapping("/order/{orderId}")
    public List<NotificationRecord> getNotificationsForOrder(@PathVariable Long orderId) {
        return notificationService.findByOrder(orderId);
    }
}
