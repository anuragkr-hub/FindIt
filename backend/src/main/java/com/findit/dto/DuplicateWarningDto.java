package com.findit.dto;

import com.findit.model.Item;

public class DuplicateWarningDto {

    private Item existingItem;
    private int similarityScore;
    private String reason;

    public DuplicateWarningDto() {
    }

    public DuplicateWarningDto(Item existingItem, int similarityScore, String reason) {
        this.existingItem = existingItem;
        this.similarityScore = similarityScore;
        this.reason = reason;
    }

    public Item getExistingItem() {
        return existingItem;
    }

    public void setExistingItem(Item existingItem) {
        this.existingItem = existingItem;
    }

    public int getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(int similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
