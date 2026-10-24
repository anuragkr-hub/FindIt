package com.findit.model.enums;

public enum ItemType {
    LOST("Lost"),
    FOUND("Found");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
