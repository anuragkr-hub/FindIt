package com.findit.service;

import com.findit.dto.DuplicateWarningDto;
import com.findit.dto.MatchResultDto;
import com.findit.model.Item;
import com.findit.model.User;
import com.findit.model.enums.ItemCategory;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import com.findit.model.enums.Role;
import com.findit.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private MatchingService matchingService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("alice@campus.edu", "hash", "Alice", "555-0101", Role.ROLE_USER);
        user1.setId(1L);

        user2 = new User("bob@campus.edu", "hash", "Bob", "555-0102", Role.ROLE_USER);
        user2.setId(2L);
    }

    @Test
    @DisplayName("Exact match across all criteria yields 100% total score")
    void testExactMatchAllCriteria() {
        LocalDate eventDate = LocalDate.now();

        Item lost = new Item(user1, ItemType.LOST, "Space Gray MacBook Air M2",
                "Space gray Apple MacBook Air laptop with python sticker",
                ItemCategory.ELECTRONICS, "Space Gray", "Science Library 2nd Floor", eventDate, null);
        lost.setId(10L);

        Item found = new Item(user2, ItemType.FOUND, "Space Gray MacBook Air M2",
                "Space gray Apple MacBook Air laptop with python sticker",
                ItemCategory.ELECTRONICS, "Space Gray", "Science Library 2nd Floor", eventDate, null);
        found.setId(20L);

        MatchResultDto result = matchingService.evaluateMatch(lost, found);

        assertThat(result.getTotalScore()).isEqualTo(100);
        assertThat(result.getCategoryScore()).isEqualTo(30);
        assertThat(result.getColorScore()).isEqualTo(20);
        assertThat(result.getLocationScore()).isEqualTo(20);
        assertThat(result.getDateScore()).isEqualTo(15);
        assertThat(result.getDescriptionScore()).isEqualTo(15);
        assertThat(result.getConfidenceLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Related category family receives partial score (15/30)")
    void testRelatedCategoryPartialScore() {
        List<String> explanations = new ArrayList<>();
        int score = matchingService.scoreCategory(ItemCategory.ELECTRONICS, ItemCategory.PHONES_TABLETS, explanations);

        assertThat(score).isEqualTo(15);
        assertThat(explanations).anyMatch(e -> e.contains("15/30"));
    }

    @Test
    @DisplayName("Completely different categories receive 0 points")
    void testCategoryMismatchZeroScore() {
        List<String> explanations = new ArrayList<>();
        int score = matchingService.scoreCategory(ItemCategory.ELECTRONICS, ItemCategory.BOOKS_NOTEBOOKS, explanations);

        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("Missing color on either item safely awards 0 points without inflating total")
    void testMissingColorZeroPoints() {
        List<String> explanations = new ArrayList<>();

        int scoreBothNull = matchingService.scoreColor(null, null, explanations);
        int scoreOneNull = matchingService.scoreColor("Blue", null, explanations);
        int scoreOneEmpty = matchingService.scoreColor("", "Blue", explanations);

        assertThat(scoreBothNull).isZero();
        assertThat(scoreOneNull).isZero();
        assertThat(scoreOneEmpty).isZero();
    }

    @Test
    @DisplayName("Color normalization handles casing and minor substring matches safely")
    void testColorNormalizationAndTokens() {
        List<String> explanations = new ArrayList<>();

        int exactCaseInsensitive = matchingService.scoreColor("NAVY BLUE", "navy blue", explanations);
        assertThat(exactCaseInsensitive).isEqualTo(20);

        int substringScore = matchingService.scoreColor("dark blue", "blue", explanations);
        assertThat(substringScore).isGreaterThanOrEqualTo(15);

        int mismatchScore = matchingService.scoreColor("red", "green", explanations);
        assertThat(mismatchScore).isZero();
    }

    @Test
    @DisplayName("Date proximity awards points strictly based on day boundaries")
    void testDateProximityBoundaries() {
        LocalDate today = LocalDate.now();

        // 0 days difference -> 15 pts
        assertThat(matchingService.scoreDateProximity(today, today, null)).isEqualTo(15);

        // 1-3 days -> 12 pts
        assertThat(matchingService.scoreDateProximity(today, today.minusDays(2), null)).isEqualTo(12);

        // 4-7 days -> 8 pts
        assertThat(matchingService.scoreDateProximity(today, today.minusDays(5), null)).isEqualTo(8);

        // 8-14 days -> 4 pts
        assertThat(matchingService.scoreDateProximity(today, today.minusDays(10), null)).isEqualTo(4);

        // 15-30 days -> 2 pts
        assertThat(matchingService.scoreDateProximity(today, today.minusDays(20), null)).isEqualTo(2);

        // > 30 days -> 0 pts
        assertThat(matchingService.scoreDateProximity(today, today.minusDays(45), null)).isZero();

        // null date -> 0 pts
        assertThat(matchingService.scoreDateProximity(today, null, null)).isZero();
    }

    @Test
    @DisplayName("Incompatible items (same type, same ID, or resolved) are rejected from matching")
    void testIncompatibleItemsRejection() {
        LocalDate today = LocalDate.now();

        Item lost1 = new Item(user1, ItemType.LOST, "MacBook", "desc", ItemCategory.ELECTRONICS, "Gray", "Library", today, null);
        lost1.setId(1L);

        Item lost2 = new Item(user2, ItemType.LOST, "MacBook", "desc", ItemCategory.ELECTRONICS, "Gray", "Library", today, null);
        lost2.setId(2L);

        // Same type (LOST vs LOST) -> incompatible
        MatchResultDto sameTypeResult = matchingService.evaluateMatch(lost1, lost2);
        assertThat(sameTypeResult.getTotalScore()).isZero();

        // Resolved item -> incompatible
        Item found = new Item(user2, ItemType.FOUND, "MacBook", "desc", ItemCategory.ELECTRONICS, "Gray", "Library", today, null);
        found.setId(3L);
        found.setStatus(ItemStatus.RESOLVED);

        MatchResultDto resolvedResult = matchingService.evaluateMatch(lost1, found);
        assertThat(resolvedResult.getTotalScore()).isZero();
    }

    @Test
    @DisplayName("Duplicate report detector identifies highly similar active reports of same type")
    void testDuplicateReportDetector() {
        LocalDate today = LocalDate.now();

        Item existingLost = new Item(user1, ItemType.LOST, "Space Gray MacBook Air",
                "Lost my Apple MacBook in the Science Library 2nd floor",
                ItemCategory.ELECTRONICS, "Gray", "Science Library 2nd Floor", today, null);
        existingLost.setId(5L);
        existingLost.setStatus(ItemStatus.OPEN);

        when(itemRepository.findByTypeAndStatus(ItemType.LOST, ItemStatus.OPEN))
                .thenReturn(Collections.singletonList(existingLost));

        Item newLost = new Item(user2, ItemType.LOST, "Space Gray MacBook Air Laptop",
                "Misplaced Apple MacBook laptop at Science Library 2nd floor",
                ItemCategory.ELECTRONICS, "Gray", "Science Library 2nd Floor", today, null);

        Optional<DuplicateWarningDto> warning = matchingService.checkForDuplicates(newLost);

        assertThat(warning).isPresent();
        assertThat(warning.get().getSimilarityScore()).isGreaterThanOrEqualTo(65);
    }
}
