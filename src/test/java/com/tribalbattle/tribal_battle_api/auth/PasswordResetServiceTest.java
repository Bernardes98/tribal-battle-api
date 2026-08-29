package com.tribalbattle.tribal_battle_api.auth;

import com.tribalbattle.tribal_battle_api.auth.dto.ForgotPasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ResetPasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.entity.AppUser;
import com.tribalbattle.tribal_battle_api.auth.entity.PasswordResetToken;
import com.tribalbattle.tribal_battle_api.auth.mail.PasswordResetMailer;
import com.tribalbattle.tribal_battle_api.auth.repository.AppSessionRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.AppUserRepository;
import com.tribalbattle.tribal_battle_api.auth.repository.PasswordResetTokenRepository;
import com.tribalbattle.tribal_battle_api.auth.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AppSessionRepository sessionRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetMailer passwordResetMailer;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userRepository,
                sessionRepository,
                resetTokenRepository,
                passwordEncoder,
                passwordResetMailer
        );

        ReflectionTestUtils.setField(
                service,
                "passwordResetMinutes",
                30L
        );
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com"))
                .thenReturn(Optional.empty());

        var response = service.requestReset(
                new ForgotPasswordRequest("Nobody@Example.com")
        );

        assertThat(response.message())
                .contains("If an account exists");

        verify(passwordResetMailer, never())
                .sendPasswordReset(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void resetPasswordUpdatesHashAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        String rawToken = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";

        AppUser user = AppUser.builder()
                .id(userId)
                .email("player@example.com")
                .displayName("Player")
                .passwordHash("old-hash")
                .build();

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("stored-hash")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(resetTokenRepository.findByTokenHashAndUsedAtIsNull(anyString()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("NewPassword123", "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("new-hash");
        when(sessionRepository.deleteByUserId(userId))
                .thenReturn(3L);

        var response = service.resetPassword(
                new ResetPasswordRequest(
                        rawToken,
                        "NewPassword123"
                )
        );

        assertThat(user.getPasswordHash())
                .isEqualTo("new-hash");
        assertThat(token.getUsedAt())
                .isNotNull();
        assertThat(response.revokedSessions())
                .isEqualTo(3L);

        verify(userRepository).save(user);
        verify(resetTokenRepository).save(token);
        verify(sessionRepository).deleteByUserId(userId);
    }
}
