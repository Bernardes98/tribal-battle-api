package com.tribalbattle.tribal_battle_api.auth.repository;

import com.tribalbattle.tribal_battle_api.auth.entity.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AppSessionRepository extends JpaRepository<AppSession, UUID> {

    Optional<AppSession> findByTokenHashAndExpiresAtAfter(
            String tokenHash,
            Instant now
    );

    void deleteByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(Instant now);
}
