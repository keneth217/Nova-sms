package com.novastack.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetMailService {

    private final JavaMailSender mailSender;

    @Value("${novastack.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${novastack.mail.from:no-reply@novasms.local}")
    private String from;

    public void sendResetLink(String email, String fullName, String resetUrl) {
        if (!mailEnabled) {
            log.warn("Password reset email is disabled. Development reset URL for {}: {}", email, resetUrl);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Reset your Nova SMS password");
        message.setText("""
                Hello %s,

                Use the link below to reset your Nova SMS password:

                %s

                This link expires in 30 minutes and can only be used once.
                If you did not request this change, ignore this email.

                Nova SMS
                """.formatted(fullName, resetUrl));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Unable to send password reset email to {}", email, ex);
        }
    }
}
