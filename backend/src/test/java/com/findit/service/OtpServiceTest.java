package com.findit.service;

import com.findit.dto.OtpResponseDto;
import com.findit.model.enums.OtpPurpose;
import com.findit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class OtpServiceTest {

    private OtpService otpService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl();
    }

    @Test
    @DisplayName("sendOtp generates a 6-digit code with expiry and masked target")
    void testSendOtpGeneratesValidCode() {
        OtpResponseDto response = otpService.sendOtp("teststudent@campus.edu", OtpPurpose.REGISTRATION);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getDemoCode()).isNotNull().hasSize(6);
        assertThat(response.getMaskedTarget()).contains("***@campus.edu");
        assertThat(response.getExpirySeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("verifyOtp succeeds with the exact dispatched code")
    void testVerifyOtpSuccess() {
        String target = "student@campus.edu";
        OtpResponseDto sendResponse = otpService.sendOtp(target, OtpPurpose.REGISTRATION);
        String code = sendResponse.getDemoCode();

        boolean verified = otpService.verifyOtp(target, code, OtpPurpose.REGISTRATION);
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("verifyOtp rejects invalid or wrong codes")
    void testVerifyOtpFailsWithWrongCode() {
        String target = "student@campus.edu";
        otpService.sendOtp(target, OtpPurpose.REGISTRATION);

        boolean verified = otpService.verifyOtp(target, "000000", OtpPurpose.REGISTRATION);
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("verifyOtp rejects mismatched purpose")
    void testVerifyOtpFailsWithDifferentPurpose() {
        String target = "student@campus.edu";
        OtpResponseDto sendResponse = otpService.sendOtp(target, OtpPurpose.REGISTRATION);

        // Try verifying for LOGIN purpose instead of REGISTRATION
        boolean verified = otpService.verifyOtp(target, sendResponse.getDemoCode(), OtpPurpose.LOGIN);
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Phone numbers are normalized and work seamlessly")
    void testPhoneOtpFlow() {
        String phone = "+1 (555) 019-2834";
        OtpResponseDto sendResponse = otpService.sendOtp(phone, OtpPurpose.PHONE_VERIFY);
        assertThat(sendResponse.isSuccess()).isTrue();

        // Verify using raw phone
        boolean verified = otpService.verifyOtp(phone, sendResponse.getDemoCode(), OtpPurpose.PHONE_VERIFY);
        assertThat(verified).isTrue();
    }
}
