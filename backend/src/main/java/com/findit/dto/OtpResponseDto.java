package com.findit.dto;

public class OtpResponseDto {

    private boolean success;
    private String message;
    private String maskedTarget;
    private String demoCode;
    private int expirySeconds;

    public OtpResponseDto() {
    }

    public OtpResponseDto(boolean success, String message, String maskedTarget, String demoCode, int expirySeconds) {
        this.success = success;
        this.message = message;
        this.maskedTarget = maskedTarget;
        this.demoCode = demoCode;
        this.expirySeconds = expirySeconds;
    }

    public static OtpResponseDto success(String message, String maskedTarget, int expirySeconds) {
        return new OtpResponseDto(true, message, maskedTarget, null, expirySeconds);
    }

    public static OtpResponseDto success(String message, String maskedTarget, String demoCode, int expirySeconds) {
        return new OtpResponseDto(true, message, maskedTarget, demoCode, expirySeconds);
    }

    public static OtpResponseDto error(String message) {
        return new OtpResponseDto(false, message, null, null, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMaskedTarget() {
        return maskedTarget;
    }

    public void setMaskedTarget(String maskedTarget) {
        this.maskedTarget = maskedTarget;
    }

    public String getDemoCode() {
        return demoCode;
    }

    public void setDemoCode(String demoCode) {
        this.demoCode = demoCode;
    }

    public int getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(int expirySeconds) {
        this.expirySeconds = expirySeconds;
    }
}
