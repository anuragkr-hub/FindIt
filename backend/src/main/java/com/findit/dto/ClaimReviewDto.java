package com.findit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClaimReviewDto {

    @NotBlank(message = "Action is required")
    private String action; // APPROVE or REJECT

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String reviewerNotes;

    public ClaimReviewDto() {
    }

    public ClaimReviewDto(String action, String reviewerNotes) {
        this.action = action;
        this.reviewerNotes = reviewerNotes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReviewerNotes() {
        return reviewerNotes;
    }

    public void setReviewerNotes(String reviewerNotes) {
        this.reviewerNotes = reviewerNotes;
    }

    public boolean isApprove() {
        return "APPROVE".equalsIgnoreCase(action);
    }
}
