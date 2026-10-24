package com.findit.dto;

import com.findit.model.Item;

import java.util.ArrayList;
import java.util.List;

public class MatchResultDto implements Comparable<MatchResultDto> {

    private Item targetItem;
    private Item candidateItem;
    private int totalScore;
    private int categoryScore;
    private int colorScore;
    private int locationScore;
    private int dateScore;
    private int descriptionScore;
    private List<String> explanations = new ArrayList<>();

    public MatchResultDto() {
    }

    public MatchResultDto(Item targetItem, Item candidateItem, int totalScore,
                          int categoryScore, int colorScore, int locationScore,
                          int dateScore, int descriptionScore, List<String> explanations) {
        this.targetItem = targetItem;
        this.candidateItem = candidateItem;
        this.totalScore = totalScore;
        this.categoryScore = categoryScore;
        this.colorScore = colorScore;
        this.locationScore = locationScore;
        this.dateScore = dateScore;
        this.descriptionScore = descriptionScore;
        this.explanations = explanations != null ? explanations : new ArrayList<>();
    }

    public String getConfidenceLevel() {
        if (totalScore >= 75) return "HIGH";
        if (totalScore >= 45) return "MEDIUM";
        return "LOW";
    }

    public String getBadgeClass() {
        if (totalScore >= 75) return "badge-success";
        if (totalScore >= 45) return "badge-warning";
        return "badge-muted";
    }

    @Override
    public int compareTo(MatchResultDto other) {
        return Integer.compare(other.totalScore, this.totalScore);
    }

    // Getters and Setters
    public Item getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(Item targetItem) {
        this.targetItem = targetItem;
    }

    public Item getCandidateItem() {
        return candidateItem;
    }

    public void setCandidateItem(Item candidateItem) {
        this.candidateItem = candidateItem;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getCategoryScore() {
        return categoryScore;
    }

    public void setCategoryScore(int categoryScore) {
        this.categoryScore = categoryScore;
    }

    public int getColorScore() {
        return colorScore;
    }

    public void setColorScore(int colorScore) {
        this.colorScore = colorScore;
    }

    public int getLocationScore() {
        return locationScore;
    }

    public void setLocationScore(int locationScore) {
        this.locationScore = locationScore;
    }

    public int getDateScore() {
        return dateScore;
    }

    public void setDateScore(int dateScore) {
        this.dateScore = dateScore;
    }

    public int getDescriptionScore() {
        return descriptionScore;
    }

    public void setDescriptionScore(int descriptionScore) {
        this.descriptionScore = descriptionScore;
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public void setExplanations(List<String> explanations) {
        this.explanations = explanations;
    }
}
