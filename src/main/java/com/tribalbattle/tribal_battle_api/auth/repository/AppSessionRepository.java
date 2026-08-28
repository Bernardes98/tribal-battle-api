package com.tribalbattle.tribal_battle_api.auth.repository;

import com.tribalbattle.tribal_battle_api.auth.entity.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppSessionRepository
        extends JpaRepository<AppSession, UUID> {

    Optional<AppSession> findByTokenHash(String tokenHash);

    List<AppSession> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            Instant now
    );

    Optional<AppSession> findByIdAndUserId(
            UUID id,
            UUID userId
    );

    long deleteByUserIdAndIdNot(
            UUID userId,
            UUID id
    );

    long deleteByUserId(UUID userId);

    void deleteByTokenHash(
            String tokenHash
    );

    void deleteByUserIdAndExpiresAtBefore(
            UUID userId,
            Instant now
    );
}
