package com.tribalbattle.tribal_battle_api.auth.service;

import com.tribalbattle.tribal_battle_api.auth.dto.AuthResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthUserResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.LoginRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RegisterRequest;
import com.tribalbattle.tribal_battle_api.auth.entity.AppSession;
import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.repository.AppSessionRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final AppUserRepository userRepository;
    private final AppSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.session-hours:168}")
    private long sessionHours;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(
                request.email()
        );

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account already exists for this email."
            );
        }

        AppUser user = AppUser.builder()
                .email(email)
                .displayName(
                        request
                                .displayName()
                                .trim()
                )
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .build();

        userRepository.save(user);

        return createSession(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(
                request.email()
        );

        AppUser user = userRepository
                .findByEmail(email)
                .orElseThrow(
                        this::invalidCredentials
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        return createSession(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(String authorizationHeader) {
        return toResponse(
                requireUser(
                        requireBearerToken(
                                authorizationHeader
                        )
                )
        );
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token =
                requireBearerToken(
                        authorizationHeader
                );

        sessionRepository.deleteByTokenHash(
                sha256(token)
        );
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(String rawToken) {
        Instant now =
                Instant.now();

        AppSession session =
                sessionRepository
                        .findByTokenHashAndExpiresAtAfter(
                                sha256(rawToken),
                                now
                        )
                        .orElseThrow(
                                this::unauthorized
                        );

        return userRepository
                .findById(
                        session.getUserId()
                )
                .orElseThrow(
                        this::unauthorized
                );
    }

    public String requireBearerToken(
            String authorizationHeader
    ) {
        if (
                authorizationHeader == null ||
                !authorizationHeader.startsWith(
                        "Bearer "
                )
        ) {
            throw unauthorized();
        }

        String token =
                authorizationHeader
                        .substring(
                                "Bearer ".length()
                        )
                        .trim();

        if (token.isBlank()) {
            throw unauthorized();
        }

        return token;
    }

    private AuthResponse createSession(
            AppUser user
    ) {
        Instant now =
                Instant.now();

        sessionRepository.deleteByExpiresAtBefore(
                now
        );

        String token =
                createOpaqueToken();

        Instant expiresAt =
                now.plus(
                        Duration.ofHours(
                                Math.max(
                                        1,
                                        sessionHours
                                )
                        )
                );

        AppSession session =
                AppSession.builder()
                        .userId(
                                user.getId()
                        )
                        .tokenHash(
                                sha256(token)
                        )
                        .createdAt(now)
                        .expiresAt(
                                expiresAt
                        )
                        .build();

        sessionRepository.save(session);

        return new AuthResponse(
                token,
                expiresAt,
                toResponse(user)
        );
    }

    private AuthUserResponse toResponse(
            AppUser user
    ) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }

    private String normalizeEmail(
            String email
    ) {
        return email
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String createOpaqueToken() {
        byte[] bytes =
                new byte[32];

        SECURE_RANDOM.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }

    private String sha256(
            String value
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return java.util.HexFormat
                    .of()
                    .formatHex(hash);
        } catch (
                NoSuchAlgorithmException exception
        ) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    exception
            );
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required."
        );
    }
}
