package com.findit.dto;

import com.findit.model.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;

public class OtpRequestDto {

    @NotBlank(message = "Email or mobile number is required")
    private String target;

    private OtpPurpose purpose = OtpPurpose.REGISTRATION;

    public OtpRequestDto() {
    }

    public OtpRequestDto(String target, OtpPurpose purpose) {
        this.target = target;
        this.purpose = purpose;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(OtpPurpose purpose) {
        this.purpose = purpose;
    }
}
