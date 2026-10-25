package com.findit.dto;

import com.findit.model.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpVerificationDto {

    @NotBlank(message = "Destination is required")
    private String target;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be 6 digits")
    private String code;

    private OtpPurpose purpose = OtpPurpose.REGISTRATION;

    public OtpVerificationDto() {
    }

    public OtpVerificationDto(String target, String code, OtpPurpose purpose) {
        this.target = target;
        this.code = code;
        this.purpose = purpose;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}
