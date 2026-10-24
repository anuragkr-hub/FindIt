package com.findit.model.enums;

public enum ItemCategory {
    ELECTRONICS("Electronics & Laptops"),
    PHONES_TABLETS("Phones & Tablets"),
    KEYS_BADGES("Keys & Badges"),
    WALLETS_BAGS("Wallets, Bags & Backpacks"),
    ID_DOCUMENTS("IDs, Cards & Documents"),
    CLOTHING_ACCESSORIES("Clothing & Accessories"),
    BOOKS_NOTEBOOKS("Books, Notes & Supplies"),
    JEWELRY_WATCHES("Jewelry & Watches"),
    SPORTS_WATERBOTTLES("Sports Gear & Bottles"),
    OTHER("Other Miscellaneous");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
