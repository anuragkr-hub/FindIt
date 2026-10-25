package com.findit.model.enums;

public enum OtpPurpose {
    REGISTRATION("Account Registration & Validation"),
    LOGIN("Secure Passwordless Login"),
    PHONE_VERIFY("Mobile Number Verification"),
    EMAIL_VERIFY("Campus Email Verification");

    private final String description;

    OtpPurpose(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
