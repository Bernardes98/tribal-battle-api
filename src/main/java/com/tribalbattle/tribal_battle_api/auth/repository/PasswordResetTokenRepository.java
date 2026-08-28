package com.tribalbattle.tribal_battle_api.auth.repository;

import com.tribalbattle.tribal_battle_api.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(
            String tokenHash
    );

    void deleteByUserId(UUID userId);

    void deleteByExpiresAtBefore(Instant now);
}
