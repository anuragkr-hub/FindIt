package com.findit.controller;

import com.findit.model.User;
import com.findit.security.SecurityUtils;
import com.findit.service.NotificationService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalModelAttributesAdvice {

    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public GlobalModelAttributesAdvice(SecurityUtils securityUtils, NotificationService notificationService) {
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        return securityUtils.getCurrentUser().orElse(null);
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        return securityUtils.isAuthenticated();
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return securityUtils.isCurrentUserAdmin();
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount() {
        Optional<User> userOpt = securityUtils.getCurrentUser();
        return userOpt.map(notificationService::countUnread).orElse(0L);
    }
}
