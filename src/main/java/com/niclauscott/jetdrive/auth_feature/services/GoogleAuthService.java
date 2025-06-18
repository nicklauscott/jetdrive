package com.niclauscott.jetdrive.auth_feature.services;

import com.niclauscott.jetdrive.auth_feature.model.dtos.GoogleLoginRequestDTO;
import com.niclauscott.jetdrive.auth_feature.model.dtos.TokenPairResponseDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GoogleAuthService {

    private final AuthService service;

    public TokenPairResponseDTO login(GoogleLoginRequestDTO requestDTO) {
        // register and generate a token if user doesn't exist

        // generate a token if user exists
        return  null;
    }
}
