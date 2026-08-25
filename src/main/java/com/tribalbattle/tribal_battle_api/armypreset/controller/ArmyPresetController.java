package com.tribalbattle.tribal_battle_api.armypreset.controller;

import com.tribalbattle.tribal_battle_api.armypreset.dto.ArmyPresetResponse;
import com.tribalbattle.tribal_battle_api.armypreset.dto.CreateArmyPresetRequest;
import com.tribalbattle.tribal_battle_api.armypreset.service.ArmyPresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/army-presets")
@RequiredArgsConstructor
@Tag(
        name = "Army Presets",
        description = "Save and reuse attacker and defender army compositions."
)
public class ArmyPresetController {

    private final ArmyPresetService service;

    @PostMapping
    @Operation(
            summary = "Save army preset"
    )
    public ResponseEntity<ArmyPresetResponse> create(
            @Valid
            @RequestBody
            CreateArmyPresetRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(request)
                );
    }

    @GetMapping
    @Operation(
            summary = "List army presets"
    )
    public ResponseEntity<List<ArmyPresetResponse>> list(
            @RequestParam
            String clientId
    ) {
        return ResponseEntity.ok(
                service.list(clientId)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete army preset"
    )
    public ResponseEntity<Void> delete(
            @PathVariable
            UUID id,

            @RequestParam
            String clientId
    ) {
        service.delete(
                id,
                clientId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
