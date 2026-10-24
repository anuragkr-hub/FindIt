package com.findit.controller;

import com.findit.dto.ClaimReviewDto;
import com.findit.dto.ClaimSubmissionDto;
import com.findit.exception.InvalidStateTransitionException;
import com.findit.exception.UnauthorizedException;
import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.security.SecurityUtils;
import com.findit.service.ClaimService;
import com.findit.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ClaimController {

    private final ClaimService claimService;
    private final ItemService itemService;
    private final SecurityUtils securityUtils;

    public ClaimController(ClaimService claimService, ItemService itemService, SecurityUtils securityUtils) {
        this.claimService = claimService;
        this.itemService = itemService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/items/{id}/claim")
    public String showClaimForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Item item = itemService.findById(id);
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        if (item.isOwnedBy(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot submit a claim for an item you reported yourself.");
            return "redirect:/items/" + id;
        }

        if (!item.getStatus().isActive()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Claims cannot be submitted for this item because it is " + item.getStatus().getDisplayName().toLowerCase() + ".");
            return "redirect:/items/" + id;
        }

        ClaimSubmissionDto dto = new ClaimSubmissionDto();
        if (currentUser.getPhone() != null) {
            dto.setContactPhone(currentUser.getPhone());
        }

        model.addAttribute("item", item);
        model.addAttribute("claimDto", dto);
        return "pages/submit-claim";
    }

    @PostMapping("/items/{id}/claim")
    public String submitClaim(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("claimDto") ClaimSubmissionDto claimDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        Item item = itemService.findById(id);
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("item", item);
            return "pages/submit-claim";
        }

        try {
            claimService.submitClaim(id, claimDto, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your claim has been submitted to the reporter for verification. You will be notified when they review it.");
            return "redirect:/items/" + id;
        } catch (InvalidStateTransitionException | UnauthorizedException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("item", item);
            return "pages/submit-claim";
        }
    }

    @GetMapping("/claims/review/{id}")
    public String showReviewPage(@PathVariable("id") Long id, Model model) {
        Claim claim = claimService.findById(id);
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        // Authorization check
        if (!claim.getItem().isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to review this claim.");
        }

        model.addAttribute("claim", claim);
        model.addAttribute("item", claim.getItem());
        model.addAttribute("reviewDto", new ClaimReviewDto());
        return "pages/review-claims";
    }

    @PostMapping("/claims/review/{id}")
    public String processClaimReview(@PathVariable("id") Long id,
                                     @Valid @ModelAttribute("reviewDto") ClaimReviewDto reviewDto,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        Claim claim = claimService.findById(id);
        User currentUser = securityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("claim", claim);
            model.addAttribute("item", claim.getItem());
            return "pages/review-claims";
        }

        try {
            claimService.reviewClaim(id, reviewDto, currentUser);
            String actionVerb = reviewDto.isApprove() ? "approved" : "rejected";
            redirectAttributes.addFlashAttribute("successMessage", "The claim was successfully " + actionVerb + ".");
            return "redirect:/items/" + claim.getItem().getId();
        } catch (InvalidStateTransitionException | UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/claims/review/" + id;
        }
    }
}
