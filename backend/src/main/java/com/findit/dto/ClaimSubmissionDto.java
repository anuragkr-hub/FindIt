package com.findit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClaimSubmissionDto {

    @NotBlank(message = "Detailed proof of ownership is required")
    @Size(min = 15, max = 2000, message = "Proof description must be between 15 and 2000 characters with identifying specifics")
    private String proofDescription;

    @NotBlank(message = "Contact phone number is required")
    @Size(min = 7, max = 25, message = "Please provide a valid phone number")
    private String contactPhone;

    public ClaimSubmissionDto() {
    }

    public ClaimSubmissionDto(String proofDescription, String contactPhone) {
        this.proofDescription = proofDescription;
        this.contactPhone = contactPhone;
    }

    public String getProofDescription() {
        return proofDescription;
    }

    public void setProofDescription(String proofDescription) {
        this.proofDescription = proofDescription;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
