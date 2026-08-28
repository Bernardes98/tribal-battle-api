package com.tribalbattle.tribal_battle_api.simulationhistory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "simulation_history")
public class SimulationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "client_id",
            nullable = false,
            length = 36
    )
    private String clientId;

    @Column(
            name = "user_id"
    )
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 32
    )
    private SimulationHistorySource source;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String payload;

    @Column(
            name = "result_payload",
            columnDefinition = "TEXT"
    )
    private String resultPayload;

    @Column(
            name = "report_metadata",
            columnDefinition = "TEXT"
    )
    private String reportMetadataPayload;

    @Builder.Default
    @Column(
            nullable = false
    )
    private boolean favorite = false;

    @Column(
            name = "attacker_player_name",
            length = 255
    )
    private String attackerPlayerName;

    @Column(
            name = "defender_player_name",
            length = 255
    )
    private String defenderPlayerName;

    @Column(
            name = "attacker_village_name",
            length = 255
    )
    private String attackerVillageName;

    @Column(
            name = "defender_village_name",
            length = 255
    )
    private String defenderVillageName;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
