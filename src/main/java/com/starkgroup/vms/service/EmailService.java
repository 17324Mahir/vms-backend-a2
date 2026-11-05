package com.starkgroup.vms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending transactional emails (e.g., password reset OTP verification).
 * Gracefully handles environments without active SMTP configurations by logging the OTP code.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@starkgroup.com}")
    private String fromAddress;

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a password reset verification code email.
     *
     * @param toEmail The destination email address
     * @param code    The 6-digit OTP code
     * @return true if the email was successfully dispatched via SMTP, false otherwise
     */
    public boolean sendPasswordResetCode(String toEmail, String code) {
        logger.info("[SECURITY AUDIT] Generated password reset OTP for {}: {}", toEmail, code);

        if (mailSender == null) {
            logger.info("JavaMailSender is not configured; running in fallback mode for {}", toEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Stark Group VMS — Password Reset Verification Code");
            message.setText("""
                    Hello,

                    A password reset was requested for your Stark Group Vehicle Management System (VMS) account.

                    Your 6-digit verification code is:

                        %s

                    This code will expire in 15 minutes.

                    If you did not request a password reset, please ignore this email. Your password will remain unchanged.

                    Regards,
                    Stark Group Fleet Security
                    """.formatted(code));

            mailSender.send(message);
            logger.info("Successfully delivered password reset email to {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.warn("Unable to dispatch email via SMTP to {}: {}. Falling back to demo verification.",
                    toEmail, e.getMessage());
            return false;
        }
    }
}

