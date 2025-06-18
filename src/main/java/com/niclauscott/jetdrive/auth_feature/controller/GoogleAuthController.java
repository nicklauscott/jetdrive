package com.niclauscott.jetdrive.auth_feature.controller;

import com.niclauscott.jetdrive.auth_feature.services.GoogleAuthService;
import com.niclauscott.jetdrive.auth_feature.model.dtos.GoogleLoginRequestDTO;
import com.niclauscott.jetdrive.auth_feature.model.dtos.TokenPairResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/google")
@Tag(name = "Google OAuth", description = "Api for managing google oauth login")
@AllArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService service;

    @PostMapping("/register")
    @Operation(description = "Login with google account")
    public ResponseEntity<TokenPairResponseDTO> login(@Valid @RequestBody GoogleLoginRequestDTO requestDTO) {
        return ResponseEntity.ok(service.login(requestDTO));
    }

}
