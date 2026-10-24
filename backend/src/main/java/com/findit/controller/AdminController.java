package com.findit.controller;

import com.findit.dto.DashboardStatsDto;
import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.repository.ItemRepository;
import com.findit.security.SecurityUtils;
import com.findit.service.ClaimService;
import com.findit.service.DashboardService;
import com.findit.service.ItemService;
import com.findit.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DashboardService dashboardService;
    private final ItemService itemService;
    private final ClaimService claimService;
    private final UserService userService;
    private final ItemRepository itemRepository;
    private final SecurityUtils securityUtils;

    public AdminController(DashboardService dashboardService,
                           ItemService itemService,
                           ClaimService claimService,
                           UserService userService,
                           ItemRepository itemRepository,
                           SecurityUtils securityUtils) {
        this.dashboardService = dashboardService;
        this.itemService = itemService;
        this.claimService = claimService;
        this.userService = userService;
        this.itemRepository = itemRepository;
        this.securityUtils = securityUtils;
    }

    @GetMapping({"", "/dashboard"})
    public String adminDashboard(@RequestParam(value = "userPage", defaultValue = "0") int userPage,
                                 @RequestParam(value = "itemPage", defaultValue = "0") int itemPage,
                                 Model model) {
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        List<Claim> pendingClaims = claimService.getAllPendingClaims();
        Page<User> users = userService.getAllUsers(userPage, 10);
        Page<Item> items = itemRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(itemPage, 10));

        model.addAttribute("stats", stats);
        model.addAttribute("pendingClaims", pendingClaims);
        model.addAttribute("users", users);
        model.addAttribute("items", items);

        return "pages/admin/dashboard";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long targetUserId,
                                   RedirectAttributes redirectAttributes) {
        User admin = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Admin authentication required"));

        try {
            userService.toggleUserStatus(targetUserId, admin);
            redirectAttributes.addFlashAttribute("successMessage", "User account status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/dashboard#users-tab";
    }

    @PostMapping("/users/{id}/toggle-role")
    public String toggleUserRole(@PathVariable("id") Long targetUserId,
                                 RedirectAttributes redirectAttributes) {
        User admin = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Admin authentication required"));

        try {
            userService.toggleUserRole(targetUserId, admin);
            redirectAttributes.addFlashAttribute("successMessage", "User role permissions updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/dashboard#users-tab";
    }

    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable("id") Long itemId,
                             RedirectAttributes redirectAttributes) {
        User admin = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Admin authentication required"));

        try {
            itemService.deleteItem(itemId, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Report permanently removed by moderator.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/dashboard#items-tab";
    }
}
