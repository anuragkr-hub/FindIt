package com.findit.service;

public interface EmailService {

    /**
     * Dispatches an OTP verification code via real SMTP email to the specified recipient.
     *
     * @param toEmail The destination email address (e.g. kumaranurag7925@gmail.com)
     * @param otpCode The 6-digit one-time password
     * @param purposeDescription Human-readable purpose of the OTP
     * @return true if successfully sent via SMTP, false otherwise
     */
    boolean sendOtpEmail(String toEmail, String otpCode, String purposeDescription);
}
