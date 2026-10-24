package com.findit.controller;

import com.findit.dto.DuplicateWarningDto;
import com.findit.dto.ItemReportDto;
import com.findit.dto.ItemSearchFilterDto;
import com.findit.dto.MatchResultDto;
import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ItemCategory;
import com.findit.model.enums.ItemType;
import com.findit.security.SecurityUtils;
import com.findit.service.ClaimService;
import com.findit.service.ItemService;
import com.findit.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class ItemController {

    private final ItemService itemService;
    private final ClaimService claimService;
    private final MatchingService matchingService;
    private final SecurityUtils securityUtils;

    public ItemController(ItemService itemService,
                          ClaimService claimService,
                          MatchingService matchingService,
                          SecurityUtils securityUtils) {
        this.itemService = itemService;
        this.claimService = claimService;
        this.matchingService = matchingService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/items")
    public String browseItems(@ModelAttribute("filter") ItemSearchFilterDto filter, Model model) {
        Page<Item> itemPage = itemService.searchItems(filter);
        model.addAttribute("itemPage", itemPage);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("types", ItemType.values());
        return "pages/browse";
    }

    @GetMapping("/items/{id}")
    public String itemDetail(@PathVariable("id") Long id, Model model) {
        Item item = itemService.findById(id);
        model.addAttribute("item", item);

        User currentUser = securityUtils.getCurrentUser().orElse(null);
        boolean isOwner = currentUser != null && item.isOwnedBy(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.isAdmin();

        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canClaim", currentUser != null && !isOwner && item.getStatus().isActive());

        // If owner or admin, retrieve submitted claims to review
        if (isOwner || isAdmin) {
            List<Claim> claims = claimService.getClaimsForItem(item, currentUser);
            model.addAttribute("claims", claims);
        }

        // Preview top 3 match suggestions if owner or admin
        if (isOwner || isAdmin) {
            List<MatchResultDto> matches = matchingService.findMatchesForItem(item, 40);
            model.addAttribute("suggestedMatches", matches.stream().limit(3).toList());
            model.addAttribute("totalMatchCount", matches.size());
        }

        return "pages/item-detail";
    }

    @GetMapping("/items/report/lost")
    public String reportLostForm(Model model) {
        ItemReportDto dto = new ItemReportDto();
        dto.setType(ItemType.LOST);
        model.addAttribute("itemDto", dto);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("isEdit", false);
        return "pages/report-lost";
    }

    @PostMapping("/items/report/lost")
    public String submitReportLost(@Valid @ModelAttribute("itemDto") ItemReportDto itemDto,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        itemDto.setType(ItemType.LOST);

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("isEdit", false);
            return "pages/report-lost";
        }

        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        Item created = itemService.createItem(itemDto, user);
        redirectAttributes.addFlashAttribute("successMessage", "Your lost item report has been published successfully!");
        return "redirect:/items/" + created.getId();
    }

    @GetMapping("/items/report/found")
    public String reportFoundForm(Model model) {
        ItemReportDto dto = new ItemReportDto();
        dto.setType(ItemType.FOUND);
        model.addAttribute("itemDto", dto);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("isEdit", false);
        return "pages/report-found";
    }

    @PostMapping("/items/report/found")
    public String submitReportFound(@Valid @ModelAttribute("itemDto") ItemReportDto itemDto,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        itemDto.setType(ItemType.FOUND);

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("isEdit", false);
            return "pages/report-found";
        }

        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        Item created = itemService.createItem(itemDto, user);
        redirectAttributes.addFlashAttribute("successMessage", "Thank you for reporting this found item!");
        return "redirect:/items/" + created.getId();
    }

    @GetMapping("/items/edit/{id}")
    public String editItemForm(@PathVariable("id") Long id, Model model) {
        Item item = itemService.findById(id);
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        if (!item.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            return "redirect:/items/" + id + "?error=unauthorized";
        }

        ItemReportDto dto = new ItemReportDto();
        dto.setId(item.getId());
        dto.setType(item.getType());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setColor(item.getColor());
        dto.setLocation(item.getLocation());
        dto.setEventDate(item.getEventDate());
        dto.setExistingImageUrl(item.getImageUrl());

        model.addAttribute("itemDto", dto);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("item", item);

        return "pages/edit-item";
    }

    @PostMapping("/items/edit/{id}")
    public String processEditItem(@PathVariable("id") Long id,
                                  @Valid @ModelAttribute("itemDto") ItemReportDto itemDto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            Item item = itemService.findById(id);
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("item", item);
            return "pages/edit-item";
        }

        User user = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        itemService.updateItem(id, itemDto, user);
        redirectAttributes.addFlashAttribute("successMessage", "Item report updated successfully.");
        return "redirect:/items/" + id;
    }

    @PostMapping("/items/{id}/withdraw")
    public String withdrawItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));
        itemService.withdrawItem(id, currentUser);
        redirectAttributes.addFlashAttribute("infoMessage", "Item report has been withdrawn.");
        return "redirect:/items/" + id;
    }

    @PostMapping("/items/{id}/reopen")
    public String reopenItem(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));
        itemService.reopenItem(id, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Item report has been reopened.");
        return "redirect:/items/" + id;
    }

    @PostMapping("/api/items/check-duplicate")
    @ResponseBody
    public ResponseEntity<?> checkDuplicateAjax(@RequestBody ItemReportDto dto) {
        Optional<DuplicateWarningDto> warning = itemService.checkDuplicate(dto);
        if (warning.isPresent()) {
            return ResponseEntity.ok(warning.get());
        }
        return ResponseEntity.ok(Collections.singletonMap("duplicateFound", false));
    }
}
