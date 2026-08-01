package com.novastack.sms.service;

import com.novastack.sms.domain.entity.PasswordResetToken;
import com.novastack.sms.domain.entity.User;
import com.novastack.sms.domain.repository.PasswordResetTokenRepository;
import com.novastack.sms.domain.repository.UserRepository;
import com.novastack.sms.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_MESSAGE =
            "If an account exists for that email, a password reset link has been sent.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService mailService;

    @Value("${novastack.frontend.base-url:https://novasms.novastack.co.ke}")
    private String frontendBaseUrl;

    @Transactional
    public String requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .ifPresent(user -> {
                    tokenRepository.deleteByUserId(user.getId());

                    String rawToken = generateToken();
                    tokenRepository.save(PasswordResetToken.builder()
                            .user(user)
                            .tokenHash(hash(rawToken))
                            .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                            .build());

                    String resetUrl = UriComponentsBuilder
                            .fromUriString(frontendBaseUrl)
                            .path("/reset-password")
                            .queryParam("token", rawToken)
                            .build()
                            .toUriString();
                    mailService.sendResetLink(user.getEmail(), user.getFullName(), resetUrl);
                });
        return GENERIC_MESSAGE;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository
                .findByTokenHashAndUsedAtIsNull(hash(rawToken))
                .orElseThrow(() -> new ApiException(
                        "This password reset link is invalid or has already been used",
                        HttpStatus.BAD_REQUEST));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("This password reset link has expired", HttpStatus.BAD_REQUEST);
        }

        User user = token.getUser();
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ApiException(
                    "New password must be different from the current password",
                    HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
