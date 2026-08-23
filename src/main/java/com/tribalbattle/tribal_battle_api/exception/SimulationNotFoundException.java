package com.tribalbattle.tribal_battle_api.exception;

public class SimulationNotFoundException extends RuntimeException {

    public SimulationNotFoundException(String code) {
        super(
                "Simulation not found for code: " + code
        );
    }
}