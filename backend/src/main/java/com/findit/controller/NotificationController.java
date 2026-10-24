package com.findit.controller;

import com.findit.model.Notification;
import com.findit.model.User;
import com.findit.security.SecurityUtils;
import com.findit.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationController(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/notifications")
    public String viewNotifications(@RequestParam(value = "page", defaultValue = "0") int page,
                                    @RequestParam(value = "size", defaultValue = "10") int size,
                                    Model model) {
        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        Page<Notification> notificationPage = notificationService.getUserNotifications(user, page, size);
        model.addAttribute("notificationPage", notificationPage);
        return "pages/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markSingleRead(@PathVariable("id") Long id,
                                 @RequestParam(value = "redirect", required = false) String redirectUrl) {
        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        notificationService.markAsRead(id, user);

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && !redirectUrl.contains("://")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(RedirectAttributes redirectAttributes) {
        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        notificationService.markAllAsRead(user);
        redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
