package com.tribalbattle.tribal_battle_api.simulationhistory.exception;

import java.util.UUID;

public class SimulationHistoryNotFoundException extends RuntimeException {

    public SimulationHistoryNotFoundException(UUID id) {
        super(
                "Simulation history not found for id: " + id
        );
    }
}
