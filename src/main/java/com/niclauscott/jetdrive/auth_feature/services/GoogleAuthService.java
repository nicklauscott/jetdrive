package com.niclauscott.jetdrive.auth_feature.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.niclauscott.jetdrive.auth_feature.exception.InvalidOrExpiredTokenException;
import com.niclauscott.jetdrive.auth_feature.model.dtos.GoogleLoginRequestDTO;
import com.niclauscott.jetdrive.auth_feature.model.dtos.TokenPairResponseDTO;
import com.niclauscott.jetdrive.auth_feature.model.entities.RefreshToken;
import com.niclauscott.jetdrive.auth_feature.repository.RefreshTokenRepository;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${google.clientId}")
    private String googleClientId;

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository tokenRepository;

    public TokenPairResponseDTO login(GoogleLoginRequestDTO requestDTO) {
        try {
            String email = verifyIdToken(requestDTO.getAccessToken());
            String accessToken = jwtService.generateAccessToken(email);
            String refreshToken = jwtService.generateRefreshToken(email);
            storeRefreshToken(email, refreshToken);
            return new TokenPairResponseDTO(accessToken, refreshToken);
        }
        catch (GeneralSecurityException | IOException e) {
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
        if (idToken == null) {
            throw new InvalidOrExpiredTokenException("Invalid Google auth token");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        Optional<User> dbUser;
        try {
            dbUser = userService.getUserByEmail(email);
        } catch (SQLException e) {
            throw new InvalidOrExpiredTokenException("Error login in with Google");
        }
        if (dbUser.isPresent()) return email;

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFirstName(name);
        newUser.setPicture(picture);
        newUser.setAuthType("google");
        return userService.createUser(newUser).getEmail();
    }

    private void storeRefreshToken(String email, String token) {
        try {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setHashedToken(hashToken(token));
            refreshToken.setEmail(email);
            refreshToken.setExpiresAt(Instant.now().plus(jwtService.refreshTokenValidityInMs, ChronoUnit.MILLIS));
            tokenRepository.save(refreshToken);
        } catch (NoSuchAlgorithmException e) {
            log.info("Error hashing and saving refresh token");
        }
    }

    private String hashToken(String token) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

}
