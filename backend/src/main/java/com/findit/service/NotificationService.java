package com.findit.service;

import com.findit.model.Notification;
import com.findit.model.User;
import com.findit.model.enums.NotificationType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    Notification sendNotification(User user, String title, String message, String linkUrl, NotificationType type);

    List<Notification> getRecentNotifications(User user);

    Page<Notification> getUserNotifications(User user, int page, int size);

    long countUnread(User user);

    void markAsRead(Long notificationId, User currentUser);

    void markAllAsRead(User user);
}
