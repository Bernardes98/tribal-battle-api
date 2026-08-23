package com.tribalbattle.tribal_battle_api.sharedsimulation.repository;

import com.tribalbattle.tribal_battle_api.sharedsimulation.entity.SharedSimulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SharedSimulationRepository
        extends JpaRepository<SharedSimulation, UUID> {

    Optional<SharedSimulation> findByCode(String code);

    boolean existsByCode(String code);
}
