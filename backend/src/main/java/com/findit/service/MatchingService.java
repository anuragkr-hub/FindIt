package com.findit.service;

import com.findit.dto.DuplicateWarningDto;
import com.findit.dto.MatchResultDto;
import com.findit.model.Item;
import com.findit.model.enums.ItemCategory;
import com.findit.model.enums.ItemStatus;
import com.findit.model.enums.ItemType;
import com.findit.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private final ItemRepository itemRepository;

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "with", "it", "my",
            "this", "that", "was", "has", "have", "lost", "found", "is", "of", "by", "from",
            "item", "campus", "hall", "building", "room", "floor", "near", "please", "help"
    ));

    public MatchingService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Finds matches for an item against active items of the opposite type.
     */
    @Transactional(readOnly = true)
    public List<MatchResultDto> findMatchesForItem(Item item, int minThreshold) {
        if (item == null || item.getStatus() == ItemStatus.RESOLVED || item.getStatus() == ItemStatus.WITHDRAWN) {
            return Collections.emptyList();
        }

        ItemType oppositeType = (item.getType() == ItemType.LOST) ? ItemType.FOUND : ItemType.LOST;
        List<Item> candidates = itemRepository.findCandidateMatches(oppositeType, ItemStatus.OPEN, item.getId());

        // Also check candidates in CLAIM_PENDING as potential matches
        List<Item> pendingCandidates = itemRepository.findCandidateMatches(oppositeType, ItemStatus.CLAIM_PENDING, item.getId());
        List<Item> allCandidates = new ArrayList<>(candidates);
        allCandidates.addAll(pendingCandidates);

        List<MatchResultDto> results = new ArrayList<>();
        for (Item candidate : allCandidates) {
            MatchResultDto match = evaluateMatch(item, candidate);
            if (match.getTotalScore() >= minThreshold) {
                results.add(match);
            }
        }

        Collections.sort(results);
        return results;
    }

    /**
     * Evaluates a deterministic weighted match between two items.
     * Weights: Category 30%, Color 20%, Location 20%, Date Proximity 15%, Description 15%
     */
    public MatchResultDto evaluateMatch(Item item1, Item item2) {
        // Enforce compatibility
        if (item1 == null || item2 == null || item1.getType() == item2.getType() ||
                Objects.equals(item1.getId(), item2.getId()) ||
                item1.getStatus() == ItemStatus.RESOLVED || item1.getStatus() == ItemStatus.WITHDRAWN ||
                item2.getStatus() == ItemStatus.RESOLVED || item2.getStatus() == ItemStatus.WITHDRAWN) {
            return new MatchResultDto(item1, item2, 0, 0, 0, 0, 0, 0,
                    Collections.singletonList("Items are incompatible for matching (same type or inactive status)."));
        }

        List<String> explanations = new ArrayList<>();

        // 1. Category (30%)
        int categoryScore = scoreCategory(item1.getCategory(), item2.getCategory(), explanations);

        // 2. Color (20%)
        int colorScore = scoreColor(item1.getColor(), item2.getColor(), explanations);

        // 3. Location (20%)
        int locationScore = scoreLocation(item1.getLocation(), item2.getLocation(), explanations);

        // 4. Date Proximity (15%)
        int dateScore = scoreDateProximity(item1.getEventDate(), item2.getEventDate(), explanations);

        // 5. Description & Title Similarity (15%)
        int descriptionScore = scoreDescription(
                item1.getTitle() + " " + item1.getDescription(),
                item2.getTitle() + " " + item2.getDescription(),
                explanations
        );

        int totalScore = categoryScore + colorScore + locationScore + dateScore + descriptionScore;
        totalScore = Math.min(100, Math.max(0, totalScore));

        return new MatchResultDto(item1, item2, totalScore, categoryScore, colorScore, locationScore, dateScore, descriptionScore, explanations);
    }

    /**
     * Category scoring: exact match = 30 pts, compatible family = 15 pts, mismatch = 0 pts.
     */
    public int scoreCategory(ItemCategory c1, ItemCategory c2, List<String> explanations) {
        if (c1 == null || c2 == null) {
            if (explanations != null) explanations.add("Category missing: 0/30 pts");
            return 0;
        }

        if (c1 == c2) {
            if (explanations != null) explanations.add("Category match (" + c1.getDisplayName() + "): 30/30 pts");
            return 30;
        }

        // Compatible categories
        boolean techCompatible = (c1 == ItemCategory.ELECTRONICS && c2 == ItemCategory.PHONES_TABLETS) ||
                (c1 == ItemCategory.PHONES_TABLETS && c2 == ItemCategory.ELECTRONICS);
        boolean cardCompatible = (c1 == ItemCategory.ID_DOCUMENTS && c2 == ItemCategory.WALLETS_BAGS) ||
                (c1 == ItemCategory.WALLETS_BAGS && c2 == ItemCategory.ID_DOCUMENTS);
        boolean accessoryCompatible = (c1 == ItemCategory.CLOTHING_ACCESSORIES && c2 == ItemCategory.JEWELRY_WATCHES) ||
                (c1 == ItemCategory.JEWELRY_WATCHES && c2 == ItemCategory.CLOTHING_ACCESSORIES);

        if (techCompatible || cardCompatible || accessoryCompatible) {
            if (explanations != null) explanations.add("Related category overlap (" + c1.getDisplayName() + " / " + c2.getDisplayName() + "): 15/30 pts");
            return 15;
        }

        if (explanations != null) explanations.add("Category mismatch: 0/30 pts");
        return 0;
    }

    /**
     * Color scoring: exact match = 20 pts, substring/token overlap = proportional, missing = 0 pts.
     */
    public int scoreColor(String color1, String color2, List<String> explanations) {
        if (color1 == null || color2 == null || color1.trim().isEmpty() || color2.trim().isEmpty()) {
            if (explanations != null) explanations.add("Color not specified on both items: 0/20 pts");
            return 0;
        }

        String norm1 = normalizeText(color1);
        String norm2 = normalizeText(color2);

        if (norm1.isEmpty() || norm2.isEmpty()) {
            if (explanations != null) explanations.add("Color empty after normalization: 0/20 pts");
            return 0;
        }

        if (norm1.equals(norm2)) {
            if (explanations != null) explanations.add("Exact color match ('" + color1.trim() + "'): 20/20 pts");
            return 20;
        }

        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            if (explanations != null) explanations.add("Close color match ('" + color1.trim() + "' / '" + color2.trim() + "'): 15/20 pts");
            return 15;
        }

        Set<String> tokens1 = tokenize(norm1);
        Set<String> tokens2 = tokenize(norm2);
        double jaccard = computeJaccard(tokens1, tokens2);

        if (jaccard > 0.0) {
            int score = (int) Math.round(jaccard * 20);
            score = Math.min(15, Math.max(5, score));
            if (explanations != null) explanations.add("Partial color overlap: " + score + "/20 pts");
            return score;
        }

        if (explanations != null) explanations.add("Color mismatch: 0/20 pts");
        return 0;
    }

    /**
     * Location scoring: exact match = 20 pts, token overlap = up to 18 pts, missing = 0 pts.
     */
    public int scoreLocation(String loc1, String loc2, List<String> explanations) {
        if (loc1 == null || loc2 == null || loc1.trim().isEmpty() || loc2.trim().isEmpty()) {
            if (explanations != null) explanations.add("Location missing: 0/20 pts");
            return 0;
        }

        String norm1 = normalizeText(loc1);
        String norm2 = normalizeText(loc2);

        if (norm1.equals(norm2)) {
            if (explanations != null) explanations.add("Exact location match ('" + loc1.trim() + "'): 20/20 pts");
            return 20;
        }

        if (norm1.contains(norm2) || norm2.contains(norm1)) {
            if (explanations != null) explanations.add("Strong location overlap ('" + loc1.trim() + "' / '" + loc2.trim() + "'): 16/20 pts");
            return 16;
        }

        Set<String> tokens1 = tokenizeFiltered(norm1);
        Set<String> tokens2 = tokenizeFiltered(norm2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            if (explanations != null) explanations.add("Generic location description: 4/20 pts");
            return 4;
        }

        double jaccard = computeJaccard(tokens1, tokens2);
        if (jaccard > 0.0) {
            int score = (int) Math.round(jaccard * 20);
            score = Math.min(18, Math.max(6, score));
            if (explanations != null) explanations.add("Shared campus area/landmark: " + score + "/20 pts");
            return score;
        }

        if (explanations != null) explanations.add("Different locations: 0/20 pts");
        return 0;
    }

    /**
     * Date proximity scoring:
     * 0 days diff: 15 pts
     * 1-3 days: 12 pts
     * 4-7 days: 8 pts
     * 8-14 days: 4 pts
     * 15-30 days: 2 pts
     * > 30 days: 0 pts
     */
    public int scoreDateProximity(LocalDate d1, LocalDate d2, List<String> explanations) {
        if (d1 == null || d2 == null) {
            if (explanations != null) explanations.add("Date missing: 0/15 pts");
            return 0;
        }

        long daysDiff = Math.abs(ChronoUnit.DAYS.between(d1, d2));

        if (daysDiff == 0) {
            if (explanations != null) explanations.add("Same date event (0 days difference): 15/15 pts");
            return 15;
        } else if (daysDiff <= 3) {
            if (explanations != null) explanations.add("High date proximity (" + daysDiff + " days difference): 12/15 pts");
            return 12;
        } else if (daysDiff <= 7) {
            if (explanations != null) explanations.add("Within same week (" + daysDiff + " days difference): 8/15 pts");
            return 8;
        } else if (daysDiff <= 14) {
            if (explanations != null) explanations.add("Within two weeks (" + daysDiff + " days difference): 4/15 pts");
            return 4;
        } else if (daysDiff <= 30) {
            if (explanations != null) explanations.add("Within a month (" + daysDiff + " days difference): 2/15 pts");
            return 2;
        } else {
            if (explanations != null) explanations.add("Event dates too distant (> 30 days apart): 0/15 pts");
            return 0;
        }
    }

    /**
     * Description similarity: keyword extraction, stopwords removal, token overlap (Max 15 pts).
     */
    public int scoreDescription(String text1, String text2, List<String> explanations) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            if (explanations != null) explanations.add("Description empty: 0/15 pts");
            return 0;
        }

        Set<String> tokens1 = tokenizeFiltered(normalizeText(text1));
        Set<String> tokens2 = tokenizeFiltered(normalizeText(text2));

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            if (explanations != null) explanations.add("No distinctive keywords in description: 0/15 pts");
            return 0;
        }

        double jaccard = computeJaccard(tokens1, tokens2);
        if (jaccard >= 0.5) {
            if (explanations != null) explanations.add("Very high keyword similarity: 15/15 pts");
            return 15;
        } else if (jaccard >= 0.25) {
            int score = (int) Math.round(jaccard * 30);
            score = Math.min(14, Math.max(8, score));
            if (explanations != null) explanations.add("Strong keyword overlap: " + score + "/15 pts");
            return score;
        } else if (jaccard > 0.0) {
            int score = (int) Math.round(jaccard * 25);
            score = Math.min(7, Math.max(3, score));
            if (explanations != null) explanations.add("Partial keyword match: " + score + "/15 pts");
            return score;
        }

        if (explanations != null) explanations.add("No significant keyword overlap: 0/15 pts");
        return 0;
    }

    /**
     * Checks for duplicate reports of the SAME item type to prevent spam/redundancy.
     */
    @Transactional(readOnly = true)
    public Optional<DuplicateWarningDto> checkForDuplicates(Item newItem) {
        if (newItem == null || newItem.getType() == null) {
            return Optional.empty();
        }

        List<Item> sameTypeItems = itemRepository.findByTypeAndStatus(newItem.getType(), ItemStatus.OPEN);

        for (Item existing : sameTypeItems) {
            if (newItem.getId() != null && newItem.getId().equals(existing.getId())) {
                continue;
            }

            int catScore = scoreCategory(newItem.getCategory(), existing.getCategory(), null);
            int locScore = scoreLocation(newItem.getLocation(), existing.getLocation(), null);
            int dateScore = scoreDateProximity(newItem.getEventDate(), existing.getEventDate(), null);
            int descScore = scoreDescription(newItem.getTitle() + " " + newItem.getDescription(),
                    existing.getTitle() + " " + existing.getDescription(), null);

            // Duplicate similarity: Category (30) + Location (25) + Date (20) + Title/Desc (25)
            int simScore = catScore + locScore + dateScore + descScore;
            if (simScore >= 65) {
                String reason = String.format("A similar %s report '%s' already exists at %s on %s.",
                        newItem.getType().getDisplayName(), existing.getTitle(), existing.getLocation(), existing.getEventDate());
                return Optional.of(new DuplicateWarningDto(existing, simScore, reason));
            }
        }

        return Optional.empty();
    }

    // Helper utilities
    private String normalizeText(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokenize(String input) {
        if (input == null || input.isEmpty()) return Collections.emptySet();
        return Arrays.stream(input.split("\\s+"))
                .filter(w -> !w.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> tokenizeFiltered(String input) {
        if (input == null || input.isEmpty()) return Collections.emptySet();
        return Arrays.stream(input.split("\\s+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOPWORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private double computeJaccard(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }
}
