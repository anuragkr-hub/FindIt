package com.findit.service;

import com.findit.dto.OtpResponseDto;
import com.findit.model.enums.OtpPurpose;

public interface OtpService {

    OtpResponseDto sendOtp(String target, OtpPurpose purpose);

    boolean verifyOtp(String target, String enteredCode, OtpPurpose purpose);

    void clearOtp(String target, OtpPurpose purpose);
}
