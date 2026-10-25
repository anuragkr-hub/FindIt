package com.findit.service;

import com.findit.dto.OtpResponseDto;
import com.findit.model.enums.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    private record OtpRecord(String code, Instant expiry, OtpPurpose purpose) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
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

        // Simulation log for developers & console oversight
        log.info("\n" +
                "======================================================================\n" +
                " [FINDIT SECURITY GATEWAY] VERIFICATION OTP DISPATCHED\n" +
                " Destination : {}\n" +
                " Purpose     : {}\n" +
                " Code (OTP)  : {}\n" +
                " Valid For   : {} seconds\n" +
                "======================================================================",
                normalizedTarget, purpose.getDescription(), code, OTP_EXPIRY_SECONDS);

        String userMessage = "A 6-digit verification code has been dispatched to " + masked + ".";
        return OtpResponseDto.success(userMessage, masked, code, OTP_EXPIRY_SECONDS);
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
        // Normalize phone number (strip whitespace and common delimiters)
        return trimmed.replaceAll("[^0-9+]", "");
    }

    private String buildKey(String target, OtpPurpose purpose) {
        return (purpose != null ? purpose.name() : "GENERAL") + ":" + target;
    }

    private String maskTarget(String target) {
        if (target == null || target.length() < 4) return "****";
        if (target.contains("@")) {
            String[] parts = target.split("@");
            String name = parts[0];
            String domain = parts[1];
            String maskedName = name.length() > 2 ? name.substring(0, 2) + "***" : name + "***";
            return maskedName + "@" + domain;
        } else {
            String digits = target.replaceAll("[^0-9]", "");
            if (digits.length() > 4) {
                return "***-***-" + digits.substring(digits.length() - 4);
            }
            return "***-" + digits;
        }
    }
}
