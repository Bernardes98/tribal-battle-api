package com.tribalbattle.tribal_battle_api.exception;

public class InvalidSimulationException extends RuntimeException {

    public InvalidSimulationException(String message) {
        super(message);
    }

    public InvalidSimulationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}