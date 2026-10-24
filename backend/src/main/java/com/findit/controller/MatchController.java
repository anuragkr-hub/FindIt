package com.findit.controller;

import com.findit.dto.MatchResultDto;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.security.SecurityUtils;
import com.findit.service.ItemService;
import com.findit.service.MatchingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MatchController {

    private final ItemService itemService;
    private final MatchingService matchingService;
    private final SecurityUtils securityUtils;

    public MatchController(ItemService itemService,
                           MatchingService matchingService,
                           SecurityUtils securityUtils) {
        this.itemService = itemService;
        this.matchingService = matchingService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/matches/{id}")
    public String itemMatches(@PathVariable("id") Long id,
                              @RequestParam(value = "threshold", defaultValue = "35") int threshold,
                              Model model) {
        Item item = itemService.findById(id);
        List<MatchResultDto> matches = matchingService.findMatchesForItem(item, threshold);

        model.addAttribute("targetItem", item);
        model.addAttribute("matches", matches);
        model.addAttribute("threshold", threshold);
        return "pages/matches";
    }

    @GetMapping("/matches")
    public String userMatches(Model model) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        List<Item> userItems = itemService.getUserItems(currentUser);
        Map<Item, List<MatchResultDto>> itemMatchesMap = new HashMap<>();
        int totalMatchesFound = 0;

        for (Item item : userItems) {
            if (item.getStatus().isActive()) {
                List<MatchResultDto> matches = matchingService.findMatchesForItem(item, 40);
                if (!matches.isEmpty()) {
                    itemMatchesMap.put(item, matches);
                    totalMatchesFound += matches.size();
                }
            }
        }

        model.addAttribute("userItems", userItems);
        model.addAttribute("itemMatchesMap", itemMatchesMap);
        model.addAttribute("totalMatchesFound", totalMatchesFound);
        return "pages/matches";
    }
}
