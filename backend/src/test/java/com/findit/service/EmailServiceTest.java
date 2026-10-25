package com.findit.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "mailUsername", "findit.campus.alert@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailFrom", "findit.campus.alert@gmail.com");

        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @DisplayName("sendOtpEmail creates MimeMessage and dispatches via JavaMailSender")
    void testSendOtpEmailSuccess() {
        boolean sent = emailService.sendOtpEmail("student@campus.edu", "849201", "Registration Verification");

        assertThat(sent).isTrue();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendOtpEmail returns false when recipient is blank")
    void testSendOtpEmailEmptyRecipient() {
        boolean sent = emailService.sendOtpEmail("", "849201", "Registration Verification");

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
