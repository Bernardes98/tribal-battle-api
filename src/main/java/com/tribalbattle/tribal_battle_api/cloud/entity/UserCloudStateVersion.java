package com.tribalbattle.tribal_battle_api.cloud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_cloud_state_version",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_cloud_state_version_user_revision",
                        columnNames = {
                                "user_id",
                                "revision"
                        }
                )
        }
)
public class UserCloudStateVersion {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(
            name = "user_id",
            nullable = false
    )
    private UUID userId;

    @Column(nullable = false)
    private long revision;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Column(
            name = "snapshot_at",
            nullable = false
    )
    private Instant snapshotAt;

    @Column(
            name = "archived_at",
            nullable = false
    )
    private Instant archivedAt;

    @PrePersist
    public void prepareArchive() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (archivedAt == null) {
            archivedAt = Instant.now();
        }
    }
}
