package com.niclauscott.jetdrive.auth_feature.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.niclauscott.jetdrive.auth_feature.exception.InvalidOrExpiredTokenException;
import com.niclauscott.jetdrive.auth_feature.model.dtos.GoogleLoginRequestDTO;
import com.niclauscott.jetdrive.auth_feature.model.dtos.TokenPairResponseDTO;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class GoogleAuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final String googleClientId;

    public GoogleAuthService(
            @Value("${google.clientId}") String googleClientId, UserService userService, JwtService jwtService
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId;
    }

    public TokenPairResponseDTO login(GoogleLoginRequestDTO requestDTO) {
        try {
            String email = verifyIdToken(requestDTO.getAccessToken());
            String accessToken = jwtService.generateAccessToken(email);
            String refreshToken = jwtService.generateRefreshToken(email);
            return new TokenPairResponseDTO(accessToken, refreshToken);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new InvalidOrExpiredTokenException("Invalid Google auth token");
        }
    }

    private String verifyIdToken(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                .Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singleton(googleClientId))
                .build();
        if (idTokenString == null) throw new InvalidOrExpiredTokenException("Invalid Google auth token");
        GoogleIdToken idToken = verifier.verify(idTokenString);
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        Optional<User> dbUser = userService.getUserByEmail(email);
        if (dbUser.isPresent()) return email;

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFirstName(name);
        newUser.setPicture(picture);
        newUser.setAuthType("google");
        return userService.createUser(newUser).getEmail();
    }
}
