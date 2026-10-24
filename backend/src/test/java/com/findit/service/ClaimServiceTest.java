package com.findit.service;

import com.findit.dto.ClaimReviewDto;
import com.findit.dto.ClaimSubmissionDto;
import com.findit.exception.InvalidStateTransitionException;
import com.findit.exception.UnauthorizedException;
import com.findit.model.Claim;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ClaimStatus;
import com.findit.model.enums.ItemCategory;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import com.findit.model.enums.Role;
import com.findit.repository.ClaimRepository;
import com.findit.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ClaimService claimService;

    private User reporter;
    private User claimant;
    private User thirdParty;
    private User admin;
    private Item foundItem;

    @BeforeEach
    void setUp() {
        reporter = new User("sarah@campus.edu", "pass", "Sarah Chen", "555-0102", Role.ROLE_USER);
        reporter.setId(10L);

        claimant = new User("alex@campus.edu", "pass", "Alex Rivera", "555-0101", Role.ROLE_USER);
        claimant.setId(20L);

        thirdParty = new User("mike@campus.edu", "pass", "Mike Patel", "555-0103", Role.ROLE_USER);
        thirdParty.setId(30L);

        admin = new User("admin@campus.edu", "pass", "Admin", "555-0100", Role.ROLE_ADMIN);
        admin.setId(99L);

        foundItem = new Item(reporter, ItemType.FOUND, "Car Keys", "Keys found at union",
                ItemCategory.KEYS_BADGES, "Red", "Student Union", LocalDate.now(), null);
        foundItem.setId(100L);
        foundItem.setStatus(ItemStatus.OPEN);
    }

    @Test
    @DisplayName("User cannot submit claim on their own reported item")
    void testUserCannotClaimOwnItem() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(foundItem));

        ClaimSubmissionDto dto = new ClaimSubmissionDto("This is my key with personal tag", "555-0102");

        assertThatThrownBy(() -> claimService.submitClaim(100L, dto, reporter))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("cannot submit a claim on an item you reported yourself");
    }

    @Test
    @DisplayName("Cannot claim an item that is already resolved")
    void testCannotClaimResolvedItem() {
        foundItem.setStatus(ItemStatus.RESOLVED);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(foundItem));

        ClaimSubmissionDto dto = new ClaimSubmissionDto("This is my key with personal tag", "555-0101");

        assertThatThrownBy(() -> claimService.submitClaim(100L, dto, claimant))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("already been marked as resolved");
    }

    @Test
    @DisplayName("Submitting valid claim transitions item to CLAIM_PENDING and creates claim")
    void testSuccessfulClaimSubmission() {
        when(itemRepository.findById(100L)).thenReturn(Optional.of(foundItem));
        when(claimRepository.existsByItemAndClaimant(foundItem, claimant)).thenReturn(false);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimSubmissionDto dto = new ClaimSubmissionDto("Specific serial number tag Schlage 2841", "555-0101");
        Claim claim = claimService.submitClaim(100L, dto, claimant);

        assertThat(claim).isNotNull();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(foundItem.getStatus()).isEqualTo(ItemStatus.CLAIM_PENDING);
        verify(notificationService).sendNotification(eq(reporter), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Claimant cannot review or approve their own claim")
    void testClaimantCannotReviewOwnClaim() {
        Claim claim = new Claim(foundItem, claimant, "Proof details", "555-0101");
        claim.setId(500L);
        claim.setStatus(ClaimStatus.PENDING);

        when(claimRepository.findById(500L)).thenReturn(Optional.of(claim));

        ClaimReviewDto reviewDto = new ClaimReviewDto("APPROVE", "Looks good");

        assertThatThrownBy(() -> claimService.reviewClaim(500L, reviewDto, claimant))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("cannot review or approve your own claim");
    }

    @Test
    @DisplayName("Unauthorized third party user cannot review someone else's item claims")
    void testUnauthorizedUserCannotReviewClaim() {
        Claim claim = new Claim(foundItem, claimant, "Proof details", "555-0101");
        claim.setId(500L);
        claim.setStatus(ClaimStatus.PENDING);

        when(claimRepository.findById(500L)).thenReturn(Optional.of(claim));

        ClaimReviewDto reviewDto = new ClaimReviewDto("APPROVE", "Looks good");

        assertThatThrownBy(() -> claimService.reviewClaim(500L, reviewDto, thirdParty))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not authorized to review claims for this item");
    }

    @Test
    @DisplayName("Approving claim marks claim APPROVED and item RESOLVED")
    void testApproveClaimTransitions() {
        Claim claim = new Claim(foundItem, claimant, "Proof details", "555-0101");
        claim.setId(500L);
        claim.setStatus(ClaimStatus.PENDING);

        when(claimRepository.findById(500L)).thenReturn(Optional.of(claim));
        when(claimRepository.findByItemOrderByCreatedAtDesc(foundItem)).thenReturn(Collections.singletonList(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimReviewDto reviewDto = new ClaimReviewDto("APPROVE", "Verified student ID matches key tag");
        Claim reviewed = claimService.reviewClaim(500L, reviewDto, reporter);

        assertThat(reviewed.getStatus()).isEqualTo(ClaimStatus.APPROVED);
        assertThat(foundItem.getStatus()).isEqualTo(ItemStatus.RESOLVED);
        verify(itemRepository).save(foundItem);
    }

    @Test
    @DisplayName("Rejecting claim marks claim REJECTED and reverts item to OPEN when no other claims exist")
    void testRejectClaimRevertsItemStatus() {
        foundItem.setStatus(ItemStatus.CLAIM_PENDING);

        Claim claim = new Claim(foundItem, claimant, "Proof details", "555-0101");
        claim.setId(500L);
        claim.setStatus(ClaimStatus.PENDING);

        when(claimRepository.findById(500L)).thenReturn(Optional.of(claim));
        when(claimRepository.findByItemOrderByCreatedAtDesc(foundItem)).thenReturn(Collections.singletonList(claim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimReviewDto reviewDto = new ClaimReviewDto("REJECT", "Insufficient identifying proof");
        Claim reviewed = claimService.reviewClaim(500L, reviewDto, reporter);

        assertThat(reviewed.getStatus()).isEqualTo(ClaimStatus.REJECTED);
        assertThat(foundItem.getStatus()).isEqualTo(ItemStatus.OPEN);
        verify(itemRepository).save(foundItem);
    }
}
