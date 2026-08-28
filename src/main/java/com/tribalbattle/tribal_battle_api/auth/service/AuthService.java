package com.tribalbattle.tribal_battle_api.auth.service;

import com.tribalbattle.tribal_battle_api.auth.dto.AuthResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthSessionInfoResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthUserResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.ChangePasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ChangePasswordResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.LoginRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RegisterRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RevokeAllSessionsResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.RevokeOtherSessionsResponse;
import com.tribalbattle.tribal_battle_api.auth.entity.AppSession;
import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.exception.AuthException;
import com.tribalbattle.tribal_battle_api.auth.repository.AppSessionRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
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
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "ACCOUNT_ALREADY_EXISTS",
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

    @Transactional
    public ChangePasswordResponse changePassword(
            String authorizationHeader,
            ChangePasswordRequest request
    ) {
        String token =
                requireBearerToken(
                        authorizationHeader
                );

        AppSession currentSession =
                requireSession(token);

        AppUser user =
                userRepository
                        .findById(
                                currentSession.getUserId()
                        )
                        .orElseThrow(
                                this::sessionRevoked
                        );

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "CURRENT_PASSWORD_INVALID",
                    "Current password is incorrect."
            );
        }

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
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        userRepository.save(user);

        long revokedSessions =
                sessionRepository
                        .deleteByUserIdAndIdNot(
                                currentSession.getUserId(),
                                currentSession.getId()
                        );

        return new ChangePasswordResponse(
                revokedSessions
        );
    }

    @Transactional
    public RevokeAllSessionsResponse revokeAllSessions(
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
                        .deleteByUserId(
                                currentSession.getUserId()
                        );

        return new RevokeAllSessionsResponse(
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
                        this::sessionRevoked
                );
    }

    @Transactional(readOnly = true)
    public AppSession requireSession(
            String rawToken
    ) {
        AppSession session =
                sessionRepository
                        .findByTokenHash(
                                sha256(rawToken)
                        )
                        .orElseThrow(
                                this::sessionRevoked
                        );

        if (!session.getExpiresAt().isAfter(
                Instant.now()
        )) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED,
                    "SESSION_EXPIRED",
                    "Your session has expired. Please sign in again."
            );
        }

        return session;
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
            throw authenticationRequired();
        }

        String token =
                authorizationHeader
                        .substring(
                                "Bearer ".length()
                        )
                        .trim();

        if (token.isBlank()) {
            throw authenticationRequired();
        }

        return token;
    }

    private AuthResponse createSession(
            AppUser user,
            String userAgent
    ) {
        Instant now =
                Instant.now();

        sessionRepository.deleteByUserIdAndExpiresAtBefore(
                user.getId(),
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

    private AuthException invalidCredentials() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid email or password."
        );
    }

    private AuthException authenticationRequired() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Sign in to continue."
        );
    }

    private AuthException sessionRevoked() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "SESSION_REVOKED",
                "This session is no longer active. Please sign in again."
        );
    }

    private AuthException sessionNotFound() {
        return new AuthException(
                HttpStatus.NOT_FOUND,
                "SESSION_NOT_FOUND",
                "Session was not found for this account."
        );
    }
}
