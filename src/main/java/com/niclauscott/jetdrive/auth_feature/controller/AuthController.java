package com.niclauscott.jetdrive.auth_feature.controller;

import com.niclauscott.jetdrive.auth_feature.model.dtos.*;
import com.niclauscott.jetdrive.auth_feature.services.AuthService;
import com.niclauscott.jetdrive.common.model.dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Api for manging authentication")
@AllArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    @Operation(description = "Register a new")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody CreateUserRequestDTO requestDTO) {
        return ResponseEntity.ok(service.register(requestDTO, "password"));
    }

    @PostMapping("/login")
    @Operation(description = "Login a user")
    public ResponseEntity<TokenPairResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        return ResponseEntity.ok(service.login(requestDTO));
    }

    @PostMapping("/validate")
    @Operation(description = "Validate a user")
    public ResponseEntity<TokenPairResponseDTO> login(@Valid @RequestBody ValidateTokenRequestDTO requestDTO) {
        service.validate(requestDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(description = "Refresh access token")
    public ResponseEntity<TokenPairResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO requestDTO) {
        return ResponseEntity.ok(service.refresh(requestDTO));
    }

}
