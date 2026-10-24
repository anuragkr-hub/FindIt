package com.findit.controller;

import com.findit.model.enums.ItemCategory;
import com.findit.service.DashboardService;
import com.findit.service.ItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ItemService itemService;
    private final DashboardService dashboardService;

    public HomeController(ItemService itemService, DashboardService dashboardService) {
        this.itemService = itemService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("recentItems", itemService.getRecentItems());
        model.addAttribute("stats", dashboardService.getDashboardStats());
        model.addAttribute("categories", ItemCategory.values());
        return "pages/index";
    }
}
