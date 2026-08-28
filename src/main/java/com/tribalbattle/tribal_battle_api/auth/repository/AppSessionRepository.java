package com.tribalbattle.tribal_battle_api.auth.repository;

import com.tribalbattle.tribal_battle_api.auth.entity.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppSessionRepository
        extends JpaRepository<AppSession, UUID> {

    Optional<AppSession> findByTokenHashAndExpiresAtAfter(
            String tokenHash,
            Instant now
    );

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

    void deleteByTokenHash(
            String tokenHash
    );

    void deleteByExpiresAtBefore(
            Instant now
    );
}
