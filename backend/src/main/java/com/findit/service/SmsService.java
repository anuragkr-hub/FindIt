package com.findit.service;

public interface SmsService {

    /**
     * Dispatches a real SMS with the 6-digit OTP code to the recipient mobile number.
     *
     * @param phoneNumber Destination mobile phone number (e.g. 8709977372)
     * @param otpCode 6-digit OTP code
     * @return true if successfully delivered via SMS gateway, false otherwise
     */
    boolean sendOtpSms(String phoneNumber, String otpCode);
}
