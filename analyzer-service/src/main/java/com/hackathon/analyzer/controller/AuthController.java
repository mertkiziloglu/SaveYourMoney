package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.model.AuthRequest;
import com.hackathon.analyzer.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoint for issuing and validating JWT tokens.
 * Accepts validated credentials and returns stateless Bearer tokens.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT token issuance and validation")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    @Operation(summary = "Authenticate and get JWT token", description = "Submit credentials to receive a Bearer token for API access")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request — validation failed")
    })
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> authenticate(
            @Valid @RequestBody AuthRequest request) {

        String token = tokenProvider.generateToken(request.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "expiresIn", 86400,
                "username", request.getUsername()));
    }

    @Operation(summary = "Validate JWT token", description = "Check if a Bearer token is still valid and return the associated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed Authorization header"),
            @ApiResponse(responseCode = "401", description = "Token expired or invalid")
    })
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        boolean valid = tokenProvider.validateToken(token);

        if (valid) {
            String username = tokenProvider.getUsernameFromToken(token);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", username));
        }

        return ResponseEntity.status(401).body(Map.of(
                "valid", false,
                "error", "Token expired or invalid"));
    }
}
