package com.tribalbattle.tribal_battle_api.cloud.repository;

import com.tribalbattle.tribal_battle_api.cloud.entity.UserCloudState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserCloudStateRepository
        extends JpaRepository<UserCloudState, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select state " +
            "from UserCloudState state " +
            "where state.userId = :userId"
    )
    Optional<UserCloudState> findForUpdate(
            @Param("userId")
            UUID userId
    );
}
