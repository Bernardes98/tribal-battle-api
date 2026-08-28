package com.tribalbattle.tribal_battle_api.simulationhistory.controller;

import com.tribalbattle.tribal_battle_api.simulationhistory.dto.BulkDeleteSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.BulkDeleteSimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.ClaimSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.ClaimSimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.CreateSimulationHistoryRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryPageResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.SimulationHistoryResponse;
import com.tribalbattle.tribal_battle_api.simulationhistory.dto.UpdateSimulationHistoryFavoriteRequest;
import com.tribalbattle.tribal_battle_api.simulationhistory.entity.SimulationHistorySource;
import com.tribalbattle.tribal_battle_api.simulationhistory.service.SimulationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulation-history")
@RequiredArgsConstructor
@Tag(
        name = "Simulation History",
        description = "Save, filter and manage battle simulation history."
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
            CreateSimulationHistoryRequest request,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.create(
                                request,
                                authorizationHeader
                        )
                );
    }

    @GetMapping
    @Operation(
            summary = "List recent simulations",
            description = "Compatibility endpoint that returns the latest 50 simulations. Use /search for History V2 pagination and filters."
    )
    public ResponseEntity<List<SimulationHistoryResponse>> list(
            @RequestParam
            String clientId,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.list(
                        clientId,
                        authorizationHeader
                )
        );
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search simulation history with server-side pagination"
    )
    public ResponseEntity<SimulationHistoryPageResponse> search(
            @RequestParam
            String clientId,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size,

            @RequestParam(
                    required = false
            )
            SimulationHistorySource source,

            @RequestParam(
                    required = false
            )
            String player,

            @RequestParam(
                    required = false
            )
            String village,

            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant from,

            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            Instant to,

            @RequestParam(
                    required = false
            )
            Boolean favorite,

            @RequestParam(
                    required = false
            )
            String search,

            @RequestParam(
                    defaultValue = "createdAt"
            )
            String sort,

            @RequestParam(
                    defaultValue = "desc"
            )
            String direction,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.search(
                        clientId,
                        authorizationHeader,
                        page,
                        size,
                        source,
                        player,
                        village,
                        from,
                        to,
                        favorite,
                        search,
                        sort,
                        direction
                )
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
            String clientId,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.findById(
                        id,
                        clientId,
                        authorizationHeader
                )
        );
    }

    @PatchMapping("/{id}/favorite")
    @Operation(
            summary = "Favorite or unfavorite a history item"
    )
    public ResponseEntity<SimulationHistoryResponse> updateFavorite(
            @PathVariable
            UUID id,

            @RequestParam
            String clientId,

            @Valid
            @RequestBody
            UpdateSimulationHistoryFavoriteRequest request,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.updateFavorite(
                        id,
                        request.favorite(),
                        clientId,
                        authorizationHeader
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
            String clientId,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        service.delete(
                id,
                clientId,
                authorizationHeader
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/bulk-delete")
    @Operation(
            summary = "Delete multiple history items"
    )
    public ResponseEntity<BulkDeleteSimulationHistoryResponse> bulkDelete(
            @RequestParam
            String clientId,

            @Valid
            @RequestBody
            BulkDeleteSimulationHistoryRequest request,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.bulkDelete(
                        request.ids(),
                        clientId,
                        authorizationHeader
                )
        );
    }

    @PostMapping("/claim")
    @Operation(
            summary = "Claim anonymous history from the current browser"
    )
    public ResponseEntity<ClaimSimulationHistoryResponse> claim(
            @Valid
            @RequestBody
            ClaimSimulationHistoryRequest request,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.claim(
                        request.clientId(),
                        authorizationHeader
                )
        );
    }
}
