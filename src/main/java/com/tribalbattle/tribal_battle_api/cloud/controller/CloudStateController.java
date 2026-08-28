package com.tribalbattle.tribal_battle_api.cloud.controller;

import com.tribalbattle.tribal_battle_api.cloud.dto.CloudStateResponse;
import com.tribalbattle.tribal_battle_api.cloud.dto.SaveCloudStateRequest;
import com.tribalbattle.tribal_battle_api.cloud.service.CloudStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cloud/state")
@RequiredArgsConstructor
@Tag(
        name = "Cloud Intelligence",
        description = "Authenticated local-first Tribal Battle cloud snapshot."
)
public class CloudStateController {

    private final CloudStateService service;

    @GetMapping
    @Operation(
            summary = "Read current account cloud snapshot"
    )
    public ResponseEntity<CloudStateResponse> get(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.get(
                        authorizationHeader
                )
        );
    }

    @PutMapping
    @Operation(
            summary = "Create or replace cloud snapshot using optimistic revision"
    )
    public ResponseEntity<CloudStateResponse> save(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader,

            @Valid
            @RequestBody
            SaveCloudStateRequest request
    ) {
        return ResponseEntity.ok(
                service.save(
                        authorizationHeader,
                        request
                )
        );
    }

    @DeleteMapping
    @Operation(
            summary = "Delete current account cloud snapshot"
    )
    public ResponseEntity<Void> delete(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        service.delete(
                authorizationHeader
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
