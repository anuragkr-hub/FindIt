package com.findit.service;

import com.findit.dto.ClaimReviewDto;
import com.findit.dto.ClaimSubmissionDto;
import com.findit.exception.InvalidStateTransitionException;
import com.findit.exception.ResourceNotFoundException;
import com.findit.exception.UnauthorizedException;
import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ClaimStatus;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.NotificationType;
import com.findit.repository.ClaimRepository;
import com.findit.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final NotificationService notificationService;

    public ClaimService(ClaimRepository claimRepository,
                        ItemRepository itemRepository,
                        NotificationService notificationService) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Claim submitClaim(Long itemId, ClaimSubmissionDto dto, User claimant) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Validation: Item must be active
        if (item.getStatus() == ItemStatus.RESOLVED) {
            throw new InvalidStateTransitionException("This item has already been marked as resolved.");
        }
        if (item.getStatus() == ItemStatus.WITHDRAWN) {
            throw new InvalidStateTransitionException("This item report has been withdrawn.");
        }

        // Validation: Cannot claim own item
        if (item.isOwnedBy(claimant.getId())) {
            throw new InvalidStateTransitionException("You cannot submit a claim on an item you reported yourself.");
        }

        // Validation: Duplicate claim check
        if (claimRepository.existsByItemAndClaimant(item, claimant)) {
            throw new InvalidStateTransitionException("You have already submitted a claim for this item.");
        }

        // Create Claim
        Claim claim = new Claim(item, claimant, dto.getProofDescription().trim(), dto.getContactPhone().trim());
        Claim savedClaim = claimRepository.save(claim);

        // Update item status to CLAIM_PENDING
        if (item.getStatus() == ItemStatus.OPEN) {
            item.setStatus(ItemStatus.CLAIM_PENDING);
            itemRepository.save(item);
        }

        // Notify item reporter
        notificationService.sendNotification(
                item.getUser(),
                "New Claim Submitted: " + item.getTitle(),
                String.format("%s submitted an ownership claim for your reported item '%s'. Please review their verification details.",
                        claimant.getFullName(), item.getTitle()),
                "/claims/review/" + savedClaim.getId(),
                NotificationType.CLAIM_RECEIVED
        );

        return savedClaim;
    }

    @Transactional
    public Claim reviewClaim(Long claimId, ClaimReviewDto dto, User reviewer) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        Item item = claim.getItem();

        // Server-side check: Claimant CANNOT approve their own claim
        if (claim.getClaimant().getId().equals(reviewer.getId())) {
            throw new UnauthorizedException("You cannot review or approve your own claim.");
        }

        // Server-side ownership / role enforcement: Reviewer must be item owner OR admin
        if (!item.isOwnedBy(reviewer.getId()) && !reviewer.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to review claims for this item.");
        }

        // Check current claim state
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidStateTransitionException("This claim has already been " + claim.getStatus().getDisplayName().toLowerCase() + ".");
        }

        String notes = dto.getReviewerNotes() != null ? dto.getReviewerNotes().trim() : "";
        claim.setReviewerNotes(notes);

        if (dto.isApprove()) {
            // APPROVE CLAIM
            claim.setStatus(ClaimStatus.APPROVED);
            item.setStatus(ItemStatus.RESOLVED);
            itemRepository.save(item);

            // Notify approved claimant
            notificationService.sendNotification(
                    claim.getClaimant(),
                    "Claim Approved: " + item.getTitle(),
                    "Your claim for '" + item.getTitle() + "' was approved! Note from reviewer: " + (notes.isEmpty() ? "Verified." : notes),
                    "/items/" + item.getId(),
                    NotificationType.CLAIM_APPROVED
            );

            // Auto-reject any other pending claims for this item
            List<Claim> allItemClaims = claimRepository.findByItemOrderByCreatedAtDesc(item);
            for (Claim otherClaim : allItemClaims) {
                if (!otherClaim.getId().equals(claim.getId()) && otherClaim.getStatus() == ClaimStatus.PENDING) {
                    otherClaim.setStatus(ClaimStatus.REJECTED);
                    otherClaim.setReviewerNotes("Another verified claim was approved for this item.");
                    claimRepository.save(otherClaim);

                    notificationService.sendNotification(
                            otherClaim.getClaimant(),
                            "Claim Update: " + item.getTitle(),
                            "Another verified claim was approved for '" + item.getTitle() + "'. Your claim has been closed.",
                            "/items/" + item.getId(),
                            NotificationType.CLAIM_REJECTED
                    );
                }
            }
        } else {
            // REJECT CLAIM
            claim.setStatus(ClaimStatus.REJECTED);

            // Check if there are other pending claims
            List<Claim> remainingClaims = claimRepository.findByItemOrderByCreatedAtDesc(item);
            boolean hasOtherPending = remainingClaims.stream()
                    .anyMatch(c -> !c.getId().equals(claim.getId()) && c.getStatus() == ClaimStatus.PENDING);

            if (!hasOtherPending && item.getStatus() == ItemStatus.CLAIM_PENDING) {
                item.setStatus(ItemStatus.OPEN);
                itemRepository.save(item);
            }

            // Notify rejected claimant
            notificationService.sendNotification(
                    claim.getClaimant(),
                    "Claim Rejected: " + item.getTitle(),
                    "Your claim for '" + item.getTitle() + "' was not approved. Reason: " + (notes.isEmpty() ? "Insufficient verification evidence." : notes),
                    "/items/" + item.getId(),
                    NotificationType.CLAIM_REJECTED
            );
        }

        return claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Claim findById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Claim> getClaimsSubmittedByUser(User user) {
        return claimRepository.findByClaimantOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Claim> getClaimsReceivedForUser(User user) {
        return claimRepository.findByItemOwner(user);
    }

    @Transactional(readOnly = true)
    public List<Claim> getClaimsForItem(Item item, User currentUser) {
        if (!item.isOwnedBy(currentUser.getId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedException("You are not authorized to view claims for this item.");
        }
        return claimRepository.findByItemOrderByCreatedAtDesc(item);
    }

    @Transactional(readOnly = true)
    public List<Claim> getAllPendingClaims() {
        return claimRepository.findByStatusOrderByCreatedAtDesc(ClaimStatus.PENDING);
    }
}
