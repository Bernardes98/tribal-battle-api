package com.tribalbattle.tribal_battle_api.intelligence.controller;

import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceAnnotationResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligencePlayerResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceReportResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceVillageResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.IntelligenceWatchlistResponse;
import com.tribalbattle.tribal_battle_api.intelligence.dto.UpdateIntelligenceAnnotationRequest;
import com.tribalbattle.tribal_battle_api.intelligence.dto.UpdateIntelligenceWatchlistRequest;
import com.tribalbattle.tribal_battle_api.intelligence.service.IntelligenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/intelligence")
public class IntelligenceController {

    private final IntelligenceService service;

    @GetMapping("/reports")
    public List<IntelligenceReportResponse> reports(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader
    ) {
        return service.listReports(authorizationHeader);
    }

    @GetMapping("/players")
    public List<IntelligencePlayerResponse> players(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader
    ) {
        return service.listPlayers(authorizationHeader);
    }

    @GetMapping("/villages")
    public List<IntelligenceVillageResponse> villages(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,

            @RequestParam(required = false)
            UUID playerId,

            @RequestParam(required = false)
            @Size(max = 120)
            String search
    ) {
        return service.listVillages(
                authorizationHeader,
                playerId,
                search
        );
    }

    @GetMapping("/watchlist")
    public IntelligenceWatchlistResponse watchlist(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader
    ) {
        return service.getWatchlist(authorizationHeader);
    }

    @PutMapping("/watchlist")
    public IntelligenceWatchlistResponse updateWatchlist(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,

            @Valid
            @RequestBody
            UpdateIntelligenceWatchlistRequest request
    ) {
        return service.updateWatchlist(
                authorizationHeader,
                request
        );
    }

    @GetMapping("/annotations")
    public List<IntelligenceAnnotationResponse> annotations(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader
    ) {
        return service.listAnnotations(authorizationHeader);
    }

    @PutMapping("/annotations")
    public IntelligenceAnnotationResponse updateAnnotation(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,

            @Valid
            @RequestBody
            UpdateIntelligenceAnnotationRequest request
    ) {
        return service.updateAnnotation(
                authorizationHeader,
                request
        );
    }

    @DeleteMapping("/annotations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnotation(
            @RequestHeader(value = "Authorization", required = false)
            String authorizationHeader,

            @RequestParam
            @Size(max = 600)
            String villageKey
    ) {
        service.deleteAnnotation(
                authorizationHeader,
                villageKey
        );
    }
}
