package com.findit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmsServiceTest {

    private SmsServiceImpl smsService;

    @BeforeEach
    void setUp() {
        smsService = new SmsServiceImpl();
    }

    @Test
    @DisplayName("sendOtpSms returns false when mobile number is blank")
    void testBlankPhoneNumber() {
        boolean sent = smsService.sendOtpSms("", "123456");
        assertThat(sent).isFalse();
    }

    @Test
    @DisplayName("sendOtpSms returns false when no SMS provider credentials are configured")
    void testUnconfiguredGateway() {
        boolean sent = smsService.sendOtpSms("8709977372", "123456");
        assertThat(sent).isFalse();
    }
}
