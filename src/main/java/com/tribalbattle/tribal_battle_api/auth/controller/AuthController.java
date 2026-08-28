package com.tribalbattle.tribal_battle_api.auth.controller;

import com.tribalbattle.tribal_battle_api.auth.dto.AuthResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthUserResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.LoginRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RegisterRequest;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Account registration and opaque bearer-token authentication."
)
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    @Operation(
            summary = "Create account"
    )
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody
            RegisterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.register(
                                request
                        )
                );
    }

    @PostMapping("/login")
    @Operation(
            summary = "Sign in"
    )
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {
        return ResponseEntity.ok(
                service.login(
                        request
                )
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "Read authenticated account"
    )
    public ResponseEntity<AuthUserResponse> me(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.me(
                        authorizationHeader
                )
        );
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Sign out and revoke session"
    )
    public ResponseEntity<Void> logout(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        service.logout(
                authorizationHeader
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
