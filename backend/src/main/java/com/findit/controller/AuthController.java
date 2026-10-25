package com.findit.controller;

import com.findit.dto.UserRegistrationDto;
import com.findit.model.enums.OtpPurpose;
import com.findit.service.OtpService;
import com.findit.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;

    public AuthController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("userDto")) {
            model.addAttribute("userDto", new UserRegistrationDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                                      BindingResult bindingResult,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        if (!userDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.userDto", "Passwords do not match");
        }

        // Validate OTP if code was submitted or session verification required
        String emailKey = "OTP_VERIFIED_REGISTRATION_" + (userDto.getEmail() != null ? userDto.getEmail().trim().toLowerCase() : "");
        boolean verifiedInSession = Boolean.TRUE.equals(session.getAttribute(emailKey));
        boolean codeSupplied = userDto.getOtpCode() != null && !userDto.getOtpCode().trim().isEmpty();

        if (codeSupplied && !verifiedInSession) {
            boolean valid = otpService.verifyOtp(userDto.getEmail(), userDto.getOtpCode(), OtpPurpose.REGISTRATION)
                    || (userDto.getPhone() != null && otpService.verifyOtp(userDto.getPhone(), userDto.getOtpCode(), OtpPurpose.REGISTRATION));
            if (!valid) {
                bindingResult.rejectValue("otpCode", "error.userDto", "Invalid or expired OTP code. Please request a new code.");
            }
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(userDto);
            session.removeAttribute(emailKey);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully with verified credentials! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }
}
