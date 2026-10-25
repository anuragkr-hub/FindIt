package com.findit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.sms.fast2sms.api-key:}")
    private String fast2SmsApiKey;

    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.from-number:}")
    private String twilioFromNumber;

    @Override
    public boolean sendOtpSms(String phoneNumber, String otpCode) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        String rawDigits = phoneNumber.replaceAll("[^0-9]", "");
        // Clean Indian 10-digit number if prefixed with 91
        String mobileNumber = rawDigits;
        if (mobileNumber.length() == 12 && mobileNumber.startsWith("91")) {
            mobileNumber = mobileNumber.substring(2);
        }

        // 1. Try Fast2SMS (Indian Gateway)
        if (fast2SmsApiKey != null && !fast2SmsApiKey.trim().isEmpty()) {
            try {
                String url = "https://www.fast2sms.com/dev/bulkV2?authorization=" + fast2SmsApiKey.trim()
                        + "&route=otp&variables_values=" + otpCode
                        + "&numbers=" + mobileNumber;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body().contains("\"return\":true")) {
                    log.info("[FINDIT SMS GATEWAY] Real SMS dispatched via Fast2SMS to {}", mobileNumber);
                    return true;
                } else {
                    log.warn("[FINDIT SMS GATEWAY] Fast2SMS response: {}", response.body());
                }
            } catch (Exception ex) {
                log.error("[FINDIT SMS GATEWAY ERROR] Fast2SMS failed: {}", ex.getMessage());
            }
        }

        // 2. Try Twilio
        if (twilioAccountSid != null && !twilioAccountSid.trim().isEmpty() && twilioAuthToken != null && !twilioAuthToken.trim().isEmpty()) {
            try {
                String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + "/Messages.json";
                String targetPhone = phoneNumber.startsWith("+") ? phoneNumber : ("+91" + mobileNumber);
                String messageBody = "Your FindIt Campus Security verification code is: " + otpCode + ". Valid for 5 minutes.";

                String form = "To=" + URLEncoder.encode(targetPhone, StandardCharsets.UTF_8)
                        + "&From=" + URLEncoder.encode(twilioFromNumber, StandardCharsets.UTF_8)
                        + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

                String auth = twilioAccountSid.trim() + ":" + twilioAuthToken.trim();
                String basicAuth = "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(twilioUrl))
                        .header("Authorization", basicAuth)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .timeout(Duration.ofSeconds(6))
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("[FINDIT SMS GATEWAY] Real SMS dispatched via Twilio to {}", targetPhone);
                    return true;
                } else {
                    log.warn("[FINDIT SMS GATEWAY] Twilio response: {}", response.body());
                }
            } catch (Exception ex) {
                log.error("[FINDIT SMS GATEWAY ERROR] Twilio failed: {}", ex.getMessage());
            }
        }

        log.warn("\n" +
                "********************************************************************************\n" +
                " [FINDIT SMS GATEWAY WARNING]\n" +
                " Real SMS dispatch cannot connect because no SMS provider is configured.\n" +
                " Destination: {}\n" +
                " Code: {}\n" +
                " To send real SMS to mobile phones, add Fast2SMS API key in application.properties:\n" +
                "   app.sms.fast2sms.api-key=YOUR_FAST2SMS_KEY\n" +
                "********************************************************************************",
                phoneNumber, otpCode);

        return false;
    }
}
