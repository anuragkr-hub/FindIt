package com.findit.service;

import com.findit.dto.OtpResponseDto;
import com.findit.model.enums.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes
    private static final SecureRandom secureRandom = new SecureRandom();

    private final EmailService emailService;
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    private record OtpRecord(String code, Instant expiry, OtpPurpose purpose) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

    public OtpServiceImpl() {
        this.emailService = null;
    }

    @Autowired
    public OtpServiceImpl(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public OtpResponseDto sendOtp(String target, OtpPurpose purpose) {
        if (target == null || target.trim().isEmpty()) {
            return OtpResponseDto.error("Destination email or phone number is required.");
        }

        String normalizedTarget = normalizeTarget(target);
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        Instant expiry = Instant.now().plusSeconds(OTP_EXPIRY_SECONDS);

        String cacheKey = buildKey(normalizedTarget, purpose);
        otpStore.put(cacheKey, new OtpRecord(code, expiry, purpose));

        String masked = maskTarget(target);

        // If target is an email address, trigger real SMTP email dispatch
        boolean isEmail = normalizedTarget.contains("@");
        boolean sentViaSmtp = false;

        if (isEmail && emailService != null) {
            sentViaSmtp = emailService.sendOtpEmail(normalizedTarget, code, purpose.getDescription());
        }

        // Server terminal log for administrator monitoring
        log.info("\n" +
                "======================================================================\n" +
                " [FINDIT SECURITY GATEWAY] VERIFICATION OTP DISPATCHED\n" +
                " Destination : {}\n" +
                " Real Email  : {}\n" +
                " Purpose     : {}\n" +
                " Code (OTP)  : {}\n" +
                " Valid For   : {} seconds\n" +
                "======================================================================",
                normalizedTarget, (sentViaSmtp ? "SENT TO INBOX" : "SMTP Credentials Not Set / Check Console"), purpose.getDescription(), code, OTP_EXPIRY_SECONDS);

        String channel = isEmail ? "your email (" + masked + ")" : "your mobile phone (" + masked + ")";
        String userMessage = "A 6-digit verification code has been dispatched to " + channel + ". Please check your inbox or spam folder.";

        // Security: NEVER expose the raw OTP in the HTTP response JSON
        return OtpResponseDto.success(userMessage, masked, OTP_EXPIRY_SECONDS);
    }

    @Override
    public boolean verifyOtp(String target, String enteredCode, OtpPurpose purpose) {
        if (target == null || enteredCode == null) {
            return false;
        }

        String normalizedTarget = normalizeTarget(target);
        String cacheKey = buildKey(normalizedTarget, purpose);
        OtpRecord record = otpStore.get(cacheKey);

        if (record == null) {
            log.warn("No OTP found for target: {} and purpose: {}", normalizedTarget, purpose);
            return false;
        }

        if (record.isExpired()) {
            otpStore.remove(cacheKey);
            log.warn("OTP expired for target: {}", normalizedTarget);
            return false;
        }

        boolean matches = record.code().equals(enteredCode.trim());
        if (matches) {
            otpStore.remove(cacheKey);
            log.info("OTP verified successfully for: {} [{}]", normalizedTarget, purpose);
            return true;
        } else {
            log.warn("Invalid OTP entered for: {}", normalizedTarget);
            return false;
        }
    }

    @Override
    public void clearOtp(String target, OtpPurpose purpose) {
        if (target != null) {
            otpStore.remove(buildKey(normalizeTarget(target), purpose));
        }
    }

    private String normalizeTarget(String target) {
        String trimmed = target.trim();
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase();
        }
        // Normalize phone number to digits only
        String digits = trimmed.replaceAll("[^0-9+]", "");
        return digits;
    }

    private String maskTarget(String target) {
        String trimmed = target.trim();
        if (trimmed.contains("@")) {
            int atIndex = trimmed.indexOf('@');
            String name = trimmed.substring(0, atIndex);
            String domain = trimmed.substring(atIndex);
            if (name.length() <= 2) {
                return name + "***" + domain;
            }
            return name.substring(0, 2) + "***" + domain;
        } else {
            String digits = trimmed.replaceAll("[^0-9]", "");
            if (digits.length() <= 4) {
                return "***" + digits;
            }
            return "***-***-" + digits.substring(digits.length() - 4);
        }
    }

    private String buildKey(String target, OtpPurpose purpose) {
        return purpose.name() + ":" + target;
    }
}
