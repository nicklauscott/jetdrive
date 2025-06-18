package com.niclauscott.jetdrive.auth_feature.repository;

import com.niclauscott.jetdrive.auth_feature.model.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByEmailAndHashedToken(String email, String hashedToken);
    Optional<RefreshToken> findByEmail(String email);
    void deleteByEmailAndHashedToken(String email, String hashedToken);
}
