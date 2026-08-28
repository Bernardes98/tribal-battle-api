package com.tribalbattle.tribal_battle_api.auth.service;

import com.tribalbattle.tribal_battle_api.auth.dto.AuthResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthSessionInfoResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthUserResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.LoginRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RegisterRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RevokeOtherSessionsResponse;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    /*
     * Compatibility overload retained for existing tests/internal callers.
     */
    @Transactional
    public AuthResponse register(
            RegisterRequest request
    ) {
        return register(
                request,
                null
        );
    }

    @Transactional
    public AuthResponse register(
            RegisterRequest request,
            String userAgent
    ) {
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

        return createSession(
                user,
                userAgent
        );
    }

    /*
     * Compatibility overload retained for existing tests/internal callers.
     */
    @Transactional
    public AuthResponse login(
            LoginRequest request
    ) {
        return login(
                request,
                null
        );
    }

    @Transactional
    public AuthResponse login(
            LoginRequest request,
            String userAgent
    ) {
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

        return createSession(
                user,
                userAgent
        );
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(
            String authorizationHeader
    ) {
        return toResponse(
                requireUser(
                        requireBearerToken(
                                authorizationHeader
                        )
                )
        );
    }

    @Transactional
    public void logout(
            String authorizationHeader
    ) {
        String token =
                requireBearerToken(
                        authorizationHeader
                );

        sessionRepository.deleteByTokenHash(
                sha256(token)
        );
    }

    @Transactional(readOnly = true)
    public List<AuthSessionInfoResponse> sessions(
            String authorizationHeader
    ) {
        String token =
                requireBearerToken(
                        authorizationHeader
                );

        AppSession currentSession =
                requireSession(token);

        Instant now =
                Instant.now();

        return sessionRepository
                .findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(
                        currentSession.getUserId(),
                        now
                )
                .stream()
                .map(
                        session ->
                                new AuthSessionInfoResponse(
                                        session.getId(),
                                        session.getUserAgent(),
                                        session.getCreatedAt(),
                                        session.getExpiresAt(),
                                        session
                                                .getId()
                                                .equals(
                                                        currentSession.getId()
                                                )
                                )
                )
                .toList();
    }

    @Transactional
    public void revokeSession(
            String authorizationHeader,
            UUID sessionId
    ) {
        String token =
                requireBearerToken(
                        authorizationHeader
                );

        AppSession currentSession =
                requireSession(token);

        AppSession target =
                sessionRepository
                        .findByIdAndUserId(
                                sessionId,
                                currentSession.getUserId()
                        )
                        .orElseThrow(
                                this::sessionNotFound
                        );

        sessionRepository.delete(target);
    }

    @Transactional
    public RevokeOtherSessionsResponse revokeOtherSessions(
            String authorizationHeader
    ) {
        String token =
                requireBearerToken(
                        authorizationHeader
                );

        AppSession currentSession =
                requireSession(token);

        long revokedCount =
                sessionRepository
                        .deleteByUserIdAndIdNot(
                                currentSession.getUserId(),
                                currentSession.getId()
                        );

        return new RevokeOtherSessionsResponse(
                revokedCount
        );
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(
            String rawToken
    ) {
        AppSession session =
                requireSession(
                        rawToken
                );

        return userRepository
                .findById(
                        session.getUserId()
                )
                .orElseThrow(
                        this::unauthorized
                );
    }

    @Transactional(readOnly = true)
    public AppSession requireSession(
            String rawToken
    ) {
        return sessionRepository
                .findByTokenHashAndExpiresAtAfter(
                        sha256(rawToken),
                        Instant.now()
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
            AppUser user,
            String userAgent
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
                        .userAgent(
                                normalizeUserAgent(
                                        userAgent
                                )
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

    private String normalizeUserAgent(
            String userAgent
    ) {
        if (
                userAgent == null ||
                userAgent.isBlank()
        ) {
            return null;
        }

        String normalized =
                userAgent.trim();

        return normalized.length() <= 500
                ? normalized
                : normalized.substring(
                        0,
                        500
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

    private ResponseStatusException sessionNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Session was not found for this account."
        );
    }
}
