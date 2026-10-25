package com.findit.service;

import com.findit.dto.OtpResponseDto;
import com.findit.model.enums.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class OtpServiceTest {

    private OtpService otpService;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = Mockito.mock(EmailService.class);
        Mockito.when(emailService.sendOtpEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        otpService = new OtpServiceImpl(emailService);
    }

    @Test
    @DisplayName("sendOtp generates a 6-digit code, dispatches email, and does NOT leak demoCode in response")
    void testSendOtpGeneratesValidCode() {
        String email = "teststudent@campus.edu";
        OtpResponseDto response = otpService.sendOtp(email, OtpPurpose.REGISTRATION);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        // Security check: raw OTP code must NEVER be returned in response to frontend
        assertThat(response.getDemoCode()).isNull();
        assertThat(response.getMaskedTarget()).contains("***@campus.edu");
        assertThat(response.getExpirySeconds()).isEqualTo(300);

        // Verify that EmailService was invoked with a 6-digit code
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(email), codeCaptor.capture(), anyString());
        assertThat(codeCaptor.getValue()).hasSize(6).matches("\\d{6}");
    }

    @Test
    @DisplayName("verifyOtp succeeds when entered code matches code sent to email")
    void testVerifyOtpSuccess() {
        String target = "student@campus.edu";
        otpService.sendOtp(target, OtpPurpose.REGISTRATION);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(target), codeCaptor.capture(), anyString());
        String sentCode = codeCaptor.getValue();

        boolean verified = otpService.verifyOtp(target, sentCode, OtpPurpose.REGISTRATION);
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
        otpService.sendOtp(target, OtpPurpose.REGISTRATION);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(target), codeCaptor.capture(), anyString());
        String sentCode = codeCaptor.getValue();

        // Try verifying for LOGIN purpose instead of REGISTRATION
        boolean verified = otpService.verifyOtp(target, sentCode, OtpPurpose.LOGIN);
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Phone numbers are normalized and work seamlessly")
    void testPhoneOtpFlow() {
        SmsService mockSms = Mockito.mock(SmsService.class);
        Mockito.when(mockSms.sendOtpSms(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        OtpService phoneOtpService = new OtpServiceImpl(emailService, mockSms);

        String phone = "+1 (555) 019-2834";
        OtpResponseDto sendResponse = phoneOtpService.sendOtp(phone, OtpPurpose.PHONE_VERIFY);
        assertThat(sendResponse.isSuccess()).isTrue();
        assertThat(sendResponse.getDemoCode()).isNull(); // Real SMS: Never leak OTP
    }
}
