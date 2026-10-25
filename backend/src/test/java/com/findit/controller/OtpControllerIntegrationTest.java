package com.findit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findit.dto.OtpRequestDto;
import com.findit.dto.OtpVerificationDto;
import com.findit.model.User;
import com.findit.model.enums.OtpPurpose;
import com.findit.model.enums.Role;
import com.findit.repository.UserRepository;
import com.findit.service.EmailService;
import com.findit.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OtpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @MockBean
    private EmailService emailService;

    @Test
    @DisplayName("POST /api/otp/send returns 200 and NEVER exposes demoCode in JSON")
    void testSendOtpEndpoint() throws Exception {
        org.mockito.Mockito.when(emailService.sendOtpEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        OtpRequestDto dto = new OtpRequestDto();
        dto.setTarget("unregistered@campus.edu");
        dto.setPurpose(OtpPurpose.REGISTRATION);

        mockMvc.perform(post("/api/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.demoCode").isEmpty());

        // Verify email dispatch was triggered with 6-digit code
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("unregistered@campus.edu"), captor.capture(), anyString());
        assertThat(captor.getValue()).hasSize(6).matches("\\d{6}");
    }

    @Test
    @DisplayName("POST /api/otp/send rejects login purpose if target is not registered")
    void testSendOtpLoginTargetNotFound() throws Exception {
        OtpRequestDto dto = new OtpRequestDto();
        dto.setTarget("doesnotexist@campus.edu");
        dto.setPurpose(OtpPurpose.LOGIN);

        mockMvc.perform(post("/api/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No campus account is registered with this email or mobile number."));
    }

    @Test
    @DisplayName("POST /api/otp/verify validates matching code received in email and returns 200")
    void testVerifyOtpEndpoint() throws Exception {
        String target = "verifytest@campus.edu";
        otpService.sendOtp(target, OtpPurpose.REGISTRATION);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(target), captor.capture(), anyString());
        String codeSentToInbox = captor.getValue();

        OtpVerificationDto verifyDto = new OtpVerificationDto();
        verifyDto.setTarget(target);
        verifyDto.setCode(codeSentToInbox);
        verifyDto.setPurpose(OtpPurpose.REGISTRATION);

        mockMvc.perform(post("/api/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /login/otp logs in registered user with valid OTP sent to email and redirects to /dashboard")
    void testLoginWithOtpSuccess() throws Exception {
        // Create user in test DB
        User user = new User();
        user.setEmail("otpuser@campus.edu");
        user.setFullName("OTP User");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.ROLE_USER);
        user.setEnabled(true);
        user.setPhone("555-8899");
        userRepository.save(user);

        // Send OTP
        otpService.sendOtp("otpuser@campus.edu", OtpPurpose.LOGIN);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("otpuser@campus.edu"), captor.capture(), anyString());
        String codeSentToInbox = captor.getValue();

        // Submit login via OTP
        mockMvc.perform(post("/login/otp")
                        .with(csrf())
                        .param("target", "otpuser@campus.edu")
                        .param("otpCode", codeSentToInbox))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
