package com.tribalbattle.tribal_battle_api.auth.service;

import com.tribalbattle.tribal_battle_api.auth.dto.ForgotPasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ForgotPasswordResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.ResetPasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ResetPasswordResponse;
import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.entity.PasswordResetToken;
import com.tribalbattle.tribal_battle_api.auth.exception.AuthException;
import com.tribalbattle.tribal_battle_api.auth.mail.PasswordResetMailer;
import com.tribalbattle.tribal_battle_api.auth.repository.AppSessionRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.AppUserRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final AppSessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailer passwordResetMailer;

    @Value("${app.auth.password-reset-minutes:30}")
    private long passwordResetMinutes;

    @Transactional
    public ForgotPasswordResponse requestReset(
            ForgotPasswordRequest request
    ) {
        Instant now = Instant.now();

        resetTokenRepository.deleteByExpiresAtBefore(now);

        userRepository
                .findByEmail(normalizeEmail(request.email()))
                .ifPresent(user -> createAndSendToken(user, now));

        return new ForgotPasswordResponse(
                "If an account exists for that email, a password reset link has been sent."
        );
    }

    @Transactional
    public ResetPasswordResponse resetPassword(
            ResetPasswordRequest request
    ) {
        Instant now = Instant.now();

        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenHashAndUsedAtIsNull(
                        sha256(request.token().trim())
                )
                .orElseThrow(this::invalidResetToken);

        if (!resetToken.getExpiresAt().isAfter(now)) {
            resetToken.setUsedAt(now);
            resetTokenRepository.save(resetToken);
            throw invalidResetToken();
        }

        AppUser user = userRepository
                .findById(resetToken.getUserId())
                .orElseThrow(this::invalidResetToken);

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "NEW_PASSWORD_REUSED",
                    "New password must be different from the current password."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );
        userRepository.save(user);

        resetToken.setUsedAt(now);
        resetTokenRepository.save(resetToken);

        long revokedSessions = sessionRepository.deleteByUserId(
                user.getId()
        );

        return new ResetPasswordResponse(
                revokedSessions,
                "Password updated. Sign in again with your new password."
        );
    }

    private void createAndSendToken(
            AppUser user,
            Instant now
    ) {
        resetTokenRepository.deleteByUserId(user.getId());

        String rawToken = createOpaqueToken();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(sha256(rawToken))
                .createdAt(now)
                .expiresAt(
                        now.plus(
                                Duration.ofMinutes(
                                        Math.max(5, passwordResetMinutes)
                                )
                        )
                )
                .build();

        resetTokenRepository.save(resetToken);

        try {
            passwordResetMailer.sendPasswordReset(
                    user.getEmail(),
                    user.getDisplayName(),
                    rawToken
            );
        } catch (RuntimeException exception) {
            resetTokenRepository.delete(resetToken);

            log.error(
                    "password_reset_delivery_failed userId={}",
                    user.getId(),
                    exception
            );
        }
    }

    private String normalizeEmail(
            String email
    ) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String createOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String sha256(
            String value
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }

    private AuthException invalidResetToken() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_RESET_TOKEN_INVALID",
                "This password reset link is invalid or has expired."
        );
    }
}
