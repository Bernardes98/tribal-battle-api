package com.tribalbattle.tribal_battle_api.intelligence.entity;

import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistorySource;
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
@Table(name = "intelligence_report")
public class IntelligenceReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "village_id", nullable = false)
    private UUID villageId;

    @Column(name = "simulation_history_id", nullable = false)
    private UUID simulationHistoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SimulationHistorySource source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "result_payload", columnDefinition = "TEXT")
    private String resultPayload;

    @Column(name = "report_metadata", nullable = false, columnDefinition = "TEXT")
    private String reportMetadataPayload;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
