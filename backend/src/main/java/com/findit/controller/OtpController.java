package com.findit.controller;

import com.findit.dto.OtpRequestDto;
import com.findit.dto.OtpResponseDto;
import com.findit.dto.OtpVerificationDto;
import com.findit.model.User;
import com.findit.model.enums.OtpPurpose;
import com.findit.repository.UserRepository;
import com.findit.security.CustomUserDetailsService;
import com.findit.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class OtpController {

    private static final Logger log = LoggerFactory.getLogger(OtpController.class);

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    public OtpController(OtpService otpService,
                         UserRepository userRepository,
                         CustomUserDetailsService userDetailsService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/api/otp/send")
    @ResponseBody
    public ResponseEntity<OtpResponseDto> sendOtpAjax(@Valid @RequestBody OtpRequestDto requestDto) {
        String target = requestDto.getTarget() != null ? requestDto.getTarget().trim() : "";
        OtpPurpose purpose = requestDto.getPurpose() != null ? requestDto.getPurpose() : OtpPurpose.REGISTRATION;

        // If purpose is LOGIN, verify that the email or phone is already registered
        if (purpose == OtpPurpose.LOGIN) {
            Optional<User> existingUser = findUserByTarget(target);
            if (existingUser.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        OtpResponseDto.error("No campus account is registered with this email or mobile number.")
                );
            }
        }

        // If purpose is REGISTRATION, warn if email or phone already registered
        if (purpose == OtpPurpose.REGISTRATION) {
            if (target.contains("@") && userRepository.existsByEmailIgnoreCase(target)) {
                return ResponseEntity.badRequest().body(
                        OtpResponseDto.error("An account with this email address already exists. Please log in.")
                );
            } else if (!target.contains("@") && userRepository.existsByPhone(target)) {
                return ResponseEntity.badRequest().body(
                        OtpResponseDto.error("An account with this mobile number already exists. Please log in.")
                );
            }
        }

        OtpResponseDto response = otpService.sendOtp(target, purpose);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/otp/verify")
    @ResponseBody
    public ResponseEntity<OtpResponseDto> verifyOtpAjax(@Valid @RequestBody OtpVerificationDto verifyDto,
                                                        HttpSession session) {
        String target = verifyDto.getTarget() != null ? verifyDto.getTarget().trim() : "";
        String code = verifyDto.getCode() != null ? verifyDto.getCode().trim() : "";
        OtpPurpose purpose = verifyDto.getPurpose() != null ? verifyDto.getPurpose() : OtpPurpose.REGISTRATION;

        boolean valid = otpService.verifyOtp(target, code, purpose);
        if (valid) {
            // Record verification in session for tamper-proof registration submission
            session.setAttribute("OTP_VERIFIED_" + purpose.name() + "_" + target.toLowerCase(), true);
            return ResponseEntity.ok(OtpResponseDto.success(
                    "Identity verified successfully! You can now proceed.",
                    target, code, 0
            ));
        } else {
            return ResponseEntity.badRequest().body(
                    OtpResponseDto.error("Invalid or expired 6-digit verification code. Please check and try again.")
            );
        }
    }

    @PostMapping("/login/otp")
    public String loginWithOtp(@RequestParam("target") String target,
                               @RequestParam("otpCode") String otpCode,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes) {
        String trimmedTarget = target != null ? target.trim() : "";
        String trimmedOtp = otpCode != null ? otpCode.trim() : "";

        if (trimmedTarget.isEmpty() || trimmedOtp.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Both email/mobile number and 6-digit OTP code are required.");
            return "redirect:/login";
        }

        boolean validOtp = otpService.verifyOtp(trimmedTarget, trimmedOtp, OtpPurpose.LOGIN);
        if (!validOtp) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired OTP code. Please request a new code.");
            return "redirect:/login";
        }

        Optional<User> userOpt = findUserByTarget(trimmedTarget);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Account not found for this email or phone number.");
            return "redirect:/login";
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your account has been deactivated. Please contact campus security.");
            return "redirect:/login";
        }

        // Authenticate user in Spring Security context
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
        securityContextRepository.saveContext(context, request, response);

        log.info("User {} successfully logged in via OTP verification.", user.getEmail());
        return "redirect:/dashboard";
    }

    private Optional<User> findUserByTarget(String target) {
        if (target.contains("@")) {
            return userRepository.findByEmailIgnoreCase(target);
        } else {
            // Find by phone
            String digits = target.replaceAll("[^0-9]", "");
            return userRepository.findAll().stream()
                    .filter(u -> u.getPhone() != null && u.getPhone().replaceAll("[^0-9]", "").equals(digits))
                    .findFirst();
        }
    }
}
