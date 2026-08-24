package com.tribalbattle.tribal_battle_api.simulationhistory.controller;

import com.tribalbattle.tribal_battle_api.simulationhistory.dto.CreateSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.service.SimulationHistoryService;
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
@RequestMapping("/api/v1/simulation-history")
@RequiredArgsConstructor
@Tag(
        name = "Simulation History",
        description = "Save and retrieve recent battle simulations."
)
public class SimulationHistoryController {

    private final SimulationHistoryService service;

    @PostMapping
    @Operation(
            summary = "Save simulation history"
    )
    public ResponseEntity<SimulationHistoryResponse> create(
            @Valid
            @RequestBody
            CreateSimulationHistoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(request)
                );
    }

    @GetMapping
    @Operation(
            summary = "List recent simulations"
    )
    public ResponseEntity<List<SimulationHistoryResponse>> list(
            @RequestParam
            String clientId
    ) {
        return ResponseEntity.ok(
                service.list(clientId)
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get simulation history"
    )
    public ResponseEntity<SimulationHistoryResponse> findById(
            @PathVariable
            UUID id,

            @RequestParam
            String clientId
    ) {
        return ResponseEntity.ok(
                service.findById(
                        id,
                        clientId
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete simulation history"
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
