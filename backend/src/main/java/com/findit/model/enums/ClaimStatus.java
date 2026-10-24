package com.findit.model.enums;

public enum ClaimStatus {
    PENDING("Pending Verification"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ClaimStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
