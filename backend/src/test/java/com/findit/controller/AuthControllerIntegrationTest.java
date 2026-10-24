package com.findit.controller;

import com.findit.model.User;
import com.findit.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Login page is publicly accessible and returns 200 OK")
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("Registration page is publicly accessible")
    void testRegisterPageAccessible() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("userDto"));
    }

    @Test
    @DisplayName("Successful registration saves user with BCrypt hash and redirects to login")
    void testSuccessfulRegistration() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "New Student")
                        .param("email", "newstudent@campus.edu")
                        .param("phone", "555-9999")
                        .param("password", "SecurePassword123!")
                        .param("confirmPassword", "SecurePassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        Optional<User> savedUser = userRepository.findByEmailIgnoreCase("newstudent@campus.edu");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getFullName()).isEqualTo("New Student");
        // Verify password is not stored in plaintext and matches BCrypt
        assertThat(savedUser.get().getPassword()).isNotEqualTo("SecurePassword123!");
        assertThat(passwordEncoder.matches("SecurePassword123!", savedUser.get().getPassword())).isTrue();
    }

    @Test
    @DisplayName("Registration rejects mismatched passwords with field error")
    void testPasswordMismatchRegistration() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Mismatch Student")
                        .param("email", "mismatch@campus.edu")
                        .param("password", "Password123!")
                        .param("confirmPassword", "DifferentPassword123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());

        assertThat(userRepository.findByEmailIgnoreCase("mismatch@campus.edu")).isNotPresent();
    }
}
