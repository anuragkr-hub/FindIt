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
    private final SmsService smsService;
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    private record OtpRecord(String code, Instant expiry, OtpPurpose purpose) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }

    public OtpServiceImpl() {
        this.emailService = null;
        this.smsService = null;
    }

    public OtpServiceImpl(EmailService emailService) {
        this.emailService = emailService;
        this.smsService = null;
    }

    @Autowired
    public OtpServiceImpl(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
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
        boolean isEmail = normalizedTarget.contains("@");
        boolean sentReal = false;

        if (isEmail && emailService != null) {
            sentReal = emailService.sendOtpEmail(normalizedTarget, code, purpose.getDescription());
        } else if (!isEmail && smsService != null) {
            sentReal = smsService.sendOtpSms(normalizedTarget, code);
        }

        log.info("\n" +
                "======================================================================\n" +
                " [FINDIT SECURITY GATEWAY] VERIFICATION OTP DISPATCHED\n" +
                " Destination : {}\n" +
                " Gateway Delivery: {}\n" +
                " Purpose     : {}\n" +
                " Code (OTP)  : {}\n" +
                " Valid For   : {} seconds\n" +
                "======================================================================",
                normalizedTarget, (sentReal ? "DELIVERED TO REAL INBOX/PHONE" : "GATEWAY NOT CONFIGURED / TERMINAL LOGGED"), purpose.getDescription(), code, OTP_EXPIRY_SECONDS);

        String userMessage;
        if (sentReal) {
            if (isEmail) {
                userMessage = "✓ Real OTP sent to your email (" + masked + ")! Please check your inbox and spam folder.";
            } else {
                userMessage = "✓ Real OTP sent to your mobile phone (" + masked + ") via SMS! Please check your messages.";
            }
            // Real delivery successful: NEVER return code in HTTP JSON
            return OtpResponseDto.success(userMessage, masked, OTP_EXPIRY_SECONDS);
        } else {
            if (isEmail) {
                userMessage = "Email Gateway Notice: Real SMTP is not yet configured on this server. For local testing, your verification code is: " + code + ". (Add your Gmail & App Password in application.properties to send real emails to your inbox.)";
            } else {
                userMessage = "SMS Gateway Notice: Real SMS API key (Fast2SMS/Twilio) is not yet configured. For local testing, your verification code is: " + code + ". (Add FAST2SMS_API_KEY to send real SMS to mobile phones.)";
            }
            // Fallback for unconfigured gateways so user is not blocked
            return OtpResponseDto.success(userMessage, masked, code, OTP_EXPIRY_SECONDS);
        }
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
