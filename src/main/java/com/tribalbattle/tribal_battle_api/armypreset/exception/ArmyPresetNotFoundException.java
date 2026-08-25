package com.tribalbattle.tribal_battle_api.armypreset.exception;

import java.util.UUID;

public class ArmyPresetNotFoundException
        extends RuntimeException {

    public ArmyPresetNotFoundException(
            UUID id
    ) {
        super(
                "Army preset not found: " + id
        );
    }
}
