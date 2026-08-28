package com.tribalbattle.tribal_battle_api.cloud.repository;

import com.tribalbattle.tribal_battle_api.cloud.entity.UserCloudStateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCloudStateVersionRepository
        extends JpaRepository<UserCloudStateVersion, UUID> {

    List<UserCloudStateVersion> findByUserIdOrderByRevisionDesc(
            UUID userId
    );

    Optional<UserCloudStateVersion> findByUserIdAndRevision(
            UUID userId,
            long revision
    );

    boolean existsByUserIdAndRevision(
            UUID userId,
            long revision
    );

    @Query(
            "select max(version.revision) " +
            "from UserCloudStateVersion version " +
            "where version.userId = :userId"
    )
    Optional<Long> findMaxRevision(
            @Param("userId")
            UUID userId
    );
}
