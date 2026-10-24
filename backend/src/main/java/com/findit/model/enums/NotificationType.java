package com.findit.model.enums;

public enum NotificationType {
    MATCH_FOUND("Potential Match Detected"),
    CLAIM_RECEIVED("New Claim Submitted"),
    CLAIM_APPROVED("Claim Approved"),
    CLAIM_REJECTED("Claim Rejected"),
    SYSTEM("System Announcement");

    private final String defaultTitle;

    NotificationType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
