package com.tribalbattle.tribal_battle_api.sharedsimulation.controller;

import com.tribalbattle.tribal_battle_api.sharedsimulation.dto.CreateSharedSimulationResponse;
import com.tribalbattle.tribal_battle_api.sharedsimulation.service.SharedSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/shared-simulations")
@RequiredArgsConstructor
@Tag(
        name = "Shared Simulations",
        description = "Create and retrieve shared battle simulations."
)
public class SharedSimulationController {

    private final SharedSimulationService service;

    @PostMapping
    @Operation(
            summary = "Create shared simulation",
            description = "Stores a battle simulation and returns a short share code."
    )
    public ResponseEntity<CreateSharedSimulationResponse> create(
            @RequestBody JsonNode simulation
    ) {
        String code =
                service.create(simulation);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        new CreateSharedSimulationResponse(
                                code
                        )
                );
    }

    @GetMapping("/{code}")
    @Operation(
            summary = "Get shared simulation",
            description = "Returns the battle simulation associated with the share code."
    )
    public ResponseEntity<JsonNode> findByCode(
            @PathVariable String code
    ) {
        return ResponseEntity.ok(
                service.findByCode(code)
        );
    }
}