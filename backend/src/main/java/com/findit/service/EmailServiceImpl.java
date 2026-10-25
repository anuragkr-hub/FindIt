package com.findit.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:noreply.findit.campus@gmail.com}")
    private String mailFrom;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendOtpEmail(String toEmail, String otpCode, String purposeDescription) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send OTP email: destination email is empty");
            return false;
        }

        String recipient = toEmail.trim();

        // Check if SMTP credentials have been configured
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            log.warn("\n" +
                    "********************************************************************************\n" +
                    " [FINDIT EMAIL GATEWAY WARNING]\n" +
                    " Real SMTP dispatch cannot connect because SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD\n" +
                    " are not configured in your environment or application.properties.\n" +
                    " Destination: {}\n" +
                    " Code (Terminal Only): {}\n" +
                    " To receive real emails in your inbox, set your Gmail address and 16-character\n" +
                    " Google App Password in application.properties or .env:\n" +
                    "   SPRING_MAIL_USERNAME=your.email@gmail.com\n" +
                    "   SPRING_MAIL_PASSWORD=your_16_char_app_password\n" +
                    "********************************************************************************",
                    recipient, otpCode);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(mailFrom, "FindIt Campus Safety");
            helper.setTo(recipient);
            helper.setSubject("Your FindIt Security Verification Code: " + otpCode);

            String htmlBody = buildHtmlTemplate(otpCode, purposeDescription);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[FINDIT EMAIL GATEWAY] Successfully sent real OTP email to {}", recipient);
            return true;
        } catch (Exception ex) {
            log.error("[FINDIT EMAIL GATEWAY ERROR] Failed to deliver real OTP email to {}: {}", recipient, ex.getMessage(), ex);
            return false;
        }
    }

    private String buildHtmlTemplate(String otpCode, String purposeDescription) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>FindIt Security Verification</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #0b1120; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <table width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0b1120; padding: 40px 20px;">
                    <tr>
                        <td align="center">
                            <table width="100%" max-width="520" border="0" cellspacing="0" cellpadding="0" style="max-width: 520px; background-color: #1e293b; border-radius: 16px; border: 1px solid rgba(56, 189, 248, 0.25); box-shadow: 0 20px 40px rgba(0,0,0,0.5); overflow: hidden;">
                                <tr>
                                    <td style="padding: 30px 30px 10px; text-align: center; background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);">
                                        <h1 style="color: #38bdf8; font-size: 24px; margin: 0 0 6px; font-weight: 800; letter-spacing: -0.5px;">FindIt</h1>
                                        <p style="color: #94a3b8; font-size: 13px; margin: 0; text-transform: uppercase; letter-spacing: 1px;">Campus Lost &amp; Found Safety Gateway</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px;">
                                        <h2 style="color: #ffffff; font-size: 18px; margin: 0 0 12px; font-weight: 700;">Identity Verification Required</h2>
                                        <p style="color: #cbd5e1; font-size: 14px; line-height: 1.6; margin: 0 0 24px;">
                                            We received a request for <strong>{{PURPOSE}}</strong> on FindIt. Use the 6-digit One-Time Password (OTP) below to authenticate your campus identity:
                                        </p>
                                        
                                        <div style="background-color: #0f172a; border: 2px dashed #38bdf8; border-radius: 12px; padding: 20px; text-align: center; margin-bottom: 24px;">
                                            <span style="font-family: 'SFMono-Regular', Consolas, monospace; font-size: 36px; font-weight: 800; color: #38bdf8; letter-spacing: 8px; display: inline-block;">{{CODE}}</span>
                                        </div>

                                        <p style="color: #94a3b8; font-size: 13px; line-height: 1.5; margin: 0 0 16px;">
                                            &#9201; This code is strictly valid for <strong>5 minutes (300 seconds)</strong>. If you did not initiate this request, you can safely ignore this email.
                                        </p>
                                        <div style="background-color: rgba(239, 68, 68, 0.1); border-left: 3px solid #ef4444; padding: 10px 14px; border-radius: 6px;">
                                            <p style="color: #fca5a5; font-size: 12px; margin: 0; line-height: 1.4;">
                                                <strong>Security Alert:</strong> Never share this code with anyone. Campus administration will never call or ask you for your verification code.
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 20px 30px; background-color: #0f172a; text-align: center; border-top: 1px solid rgba(255,255,255,0.06);">
                                        <p style="color: #64748b; font-size: 12px; margin: 0;">
                                            &copy; 2026 FindIt Campus Network &bull; Automated Security Protocol
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """
            .replace("{{PURPOSE}}", purposeDescription != null ? purposeDescription : "Campus Authentication")
            .replace("{{CODE}}", otpCode != null ? otpCode : "000000");
    }
}
