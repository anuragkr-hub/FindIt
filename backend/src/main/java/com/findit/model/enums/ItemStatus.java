package com.findit.model.enums;

public enum ItemStatus {
    OPEN("Open"),
    CLAIM_PENDING("Claim Pending"),
    RESOLVED("Resolved / Returned"),
    WITHDRAWN("Withdrawn");

    private final String displayName;

    ItemStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == OPEN || this == CLAIM_PENDING;
    }
}
