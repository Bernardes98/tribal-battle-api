package com.tribalbattle.tribal_battle_api.auth.controller;

import com.tribalbattle.tribal_battle_api.auth.dto.AuthResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthSessionInfoResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.AuthUserResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.ChangePasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ChangePasswordResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.ForgotPasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ForgotPasswordResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.ResetPasswordRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.ResetPasswordResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.LoginRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RegisterRequest;
import com.tribalbattle.tribal_battle_api.auth.dto.RevokeAllSessionsResponse;
import com.tribalbattle.tribal_battle_api.auth.dto.RevokeOtherSessionsResponse;
import com.tribalbattle.tribal_battle_api.auth.service.AuthService;
import com.tribalbattle.tribal_battle_api.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication",
        description = "Account registration and opaque bearer-token authentication."
)
public class AuthController {

    private final AuthService service;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(
            summary = "Create account"
    )
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody
            RegisterRequest request,

            @RequestHeader(
                    value = HttpHeaders.USER_AGENT,
                    required = false
            )
            String userAgent
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.register(
                                request,
                                userAgent
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
            LoginRequest request,

            @RequestHeader(
                    value = HttpHeaders.USER_AGENT,
                    required = false
            )
            String userAgent
    ) {
        return ResponseEntity.ok(
                service.login(
                        request,
                        userAgent
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

    @GetMapping("/sessions")
    @Operation(
            summary = "List active account sessions"
    )
    public ResponseEntity<List<AuthSessionInfoResponse>> sessions(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.sessions(
                        authorizationHeader
                )
        );
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
            summary = "Revoke one account session"
    )
    public ResponseEntity<Void> revokeSession(
            @PathVariable
            UUID sessionId,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        service.revokeSession(
                authorizationHeader,
                sessionId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/sessions/revoke-others")
    @Operation(
            summary = "Revoke every session except the current session"
    )
    public ResponseEntity<RevokeOtherSessionsResponse> revokeOtherSessions(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.revokeOtherSessions(
                        authorizationHeader
                )
        );
    }
    @PostMapping("/password/change")
    @Operation(
            summary = "Change account password and revoke other sessions"
    )
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest request,

            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.changePassword(
                        authorizationHeader,
                        request
                )
        );
    }

    @PostMapping("/password/forgot")
    @Operation(
            summary = "Request password reset email"
    )
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid
            @RequestBody
            ForgotPasswordRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(
                        passwordResetService.requestReset(request)
                );
    }

    @PostMapping("/password/reset")
    @Operation(
            summary = "Reset account password using an emailed token"
    )
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(
                passwordResetService.resetPassword(request)
        );
    }

    @PostMapping("/sessions/revoke-all")
    @Operation(
            summary = "Revoke every active account session"
    )
    public ResponseEntity<RevokeAllSessionsResponse> revokeAllSessions(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorizationHeader
    ) {
        return ResponseEntity.ok(
                service.revokeAllSessions(
                        authorizationHeader
                )
        );
    }

}
