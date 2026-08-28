package com.tribalbattle.tribal_battle_api.cloud.controller;

import com.tribalbattle.tribal_battle_api.cloud.dto.CloudStateResponse;
import com.tribalbattle.tribal_battle_api.cloud.dto.CloudStateVersionResponse;
import com.tribalbattle.tribal_battle_api.cloud.dto.RestoreCloudStateRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/versions")
    @Operation(
            summary = "List current and recoverable cloud snapshot revisions"
    )
    public ResponseEntity<List<CloudStateVersionResponse>> versions(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.versions(
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

    @PostMapping("/versions/{revision}/restore")
    @Operation(
            summary = "Restore an earlier snapshot as a new current revision"
    )
    public ResponseEntity<CloudStateResponse> restore(
            @PathVariable
            long revision,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader,

            @Valid
            @RequestBody
            RestoreCloudStateRequest request
    ) {
        return ResponseEntity.ok(
                service.restore(
                        authorizationHeader,
                        revision,
                        request
                )
        );
    }

    @DeleteMapping
    @Operation(
            summary = "Archive and delete the current account cloud snapshot"
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
