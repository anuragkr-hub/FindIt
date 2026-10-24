package com.findit.controller;

import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.security.SecurityUtils;
import com.findit.service.ClaimService;
import com.findit.service.ItemService;
import com.findit.service.NotificationService;
import com.findit.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DashboardController {

    private final ItemService itemService;
    private final ClaimService claimService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public DashboardController(ItemService itemService,
                               ClaimService claimService,
                               NotificationService notificationService,
                               UserService userService,
                               SecurityUtils securityUtils) {
        this.itemService = itemService;
        this.claimService = claimService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        List<Item> myItems = itemService.getUserItems(currentUser);
        List<Claim> mySubmittedClaims = claimService.getClaimsSubmittedByUser(currentUser);
        List<Claim> myReceivedClaims = claimService.getClaimsReceivedForUser(currentUser);

        long myOpenItemsCount = myItems.stream().filter(i -> i.getStatus().isActive()).count();
        long myResolvedItemsCount = myItems.stream().filter(i -> i.getStatus().name().equals("RESOLVED")).count();
        long myPendingClaimsCount = mySubmittedClaims.stream().filter(Claim::isPending).count();
        long incomingPendingCount = myReceivedClaims.stream().filter(Claim::isPending).count();

        model.addAttribute("myItems", myItems);
        model.addAttribute("mySubmittedClaims", mySubmittedClaims);
        model.addAttribute("myReceivedClaims", myReceivedClaims);
        model.addAttribute("recentNotifications", notificationService.getRecentNotifications(currentUser));

        model.addAttribute("myOpenItemsCount", myOpenItemsCount);
        model.addAttribute("myResolvedItemsCount", myResolvedItemsCount);
        model.addAttribute("myPendingClaimsCount", myPendingClaimsCount);
        model.addAttribute("incomingPendingCount", incomingPendingCount);

        return "pages/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        List<Item> myItems = itemService.getUserItems(currentUser);
        List<Claim> myClaims = claimService.getClaimsSubmittedByUser(currentUser);

        model.addAttribute("profileUser", currentUser);
        model.addAttribute("totalItemsCount", myItems.size());
        model.addAttribute("totalClaimsCount", myClaims.size());
        return "pages/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        if (fullName == null || fullName.trim().length() < 2) {
            redirectAttributes.addFlashAttribute("errorMessage", "Full name must be at least 2 characters long.");
            return "redirect:/profile";
        }

        userService.updateProfile(currentUser, fullName, phone);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/profile";
        }

        try {
            userService.changePassword(currentUser, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/profile";
    }
}
