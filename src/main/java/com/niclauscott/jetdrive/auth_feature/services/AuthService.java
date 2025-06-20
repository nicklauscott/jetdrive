package com.niclauscott.jetdrive.auth_feature.services;

import com.niclauscott.jetdrive.auth_feature.exception.InvalidOrExpiredTokenException;
import com.niclauscott.jetdrive.auth_feature.model.dtos.*;
import com.niclauscott.jetdrive.auth_feature.model.entities.RefreshToken;
import com.niclauscott.jetdrive.auth_feature.repository.RefreshTokenRepository;
import com.niclauscott.jetdrive.auth_feature.exception.BadCredentialsException;
import com.niclauscott.jetdrive.common.model.dtos.*;
import com.niclauscott.jetdrive.user_feature.model.entities.User;
import com.niclauscott.jetdrive.user_feature.service.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RefreshTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponseDTO register(CreateUserRequestDTO requestDTO, String authType) {
        requestDTO.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        return userService.createUser(requestDTO, authType);
    }

    public void validate(ValidateTokenRequestDTO requestDTO) {
        if (!jwtService.validateAccessToken(requestDTO.getAccess()))
            throw new InvalidOrExpiredTokenException("Invalid or expired token");
        String userEmail = jwtService.getEmailFromToken(requestDTO.getAccess());
        userService.getUserByEmail(userEmail)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired token"));
    }

    @Transactional
    public TokenPairResponseDTO login(@Valid LoginRequestDTO requestDTO) {
       User user = userService.getUserByEmail(requestDTO.getEmail()).orElseThrow(() ->
                 new BadCredentialsException("Bad credentials! check your email and password"));

       if (!user.getAuthType().equals("password"))
           throw new BadCredentialsException("Bad credentials! check your email and password");

       if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPasswordHash()))
           throw new BadCredentialsException("Bad credentials! check your email and password");

       Optional<RefreshToken> optional = tokenRepository.findByEmail(user.getEmail());
       optional.ifPresent(tokenRepository::delete);
       tokenRepository.flush();

       String accessToken = jwtService.generateAccessToken(user.getEmail());
       String refreshToken = jwtService.generateRefreshToken(user.getEmail());
       storeRefreshToken(user.getEmail(), refreshToken);
        return new TokenPairResponseDTO(accessToken, refreshToken);
    }

    public TokenPairResponseDTO refresh(@Valid RefreshTokenRequestDTO requestDTO) {
        if (!jwtService.validateRefreshToken(requestDTO.getRefresh()))
            throw new InvalidOrExpiredTokenException("Invalid or expired token");

        String userEmail = jwtService.getEmailFromToken(requestDTO.getRefresh());
        userService.getUserByEmail(userEmail)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired token"));

        try {
            String hashToken = hashToken(requestDTO.getRefresh());
            RefreshToken oldToken = tokenRepository.findByEmailAndHashedToken(userEmail, hashToken)
                    .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired token"));
            tokenRepository.delete(oldToken);
            tokenRepository.flush();
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidOrExpiredTokenException("Invalid or expired token");
        }

        String accessToken = jwtService.generateAccessToken(userEmail);
        String refreshToken = jwtService.generateRefreshToken(userEmail);
        storeRefreshToken(userEmail, refreshToken);
        return new TokenPairResponseDTO(accessToken, refreshToken);
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
