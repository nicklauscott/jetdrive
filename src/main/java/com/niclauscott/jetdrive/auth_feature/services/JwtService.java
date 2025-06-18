package com.niclauscott.jetdrive.auth_feature.services;

import com.niclauscott.jetdrive.auth_feature.exception.InvalidOrExpiredTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long accessTokenValidityInMs = 24 * 60 * 60 * 1000L;
    public final long refreshTokenValidityInMs = 30L * 24 * 60 * 60 * 1000L;

    public JwtService(@Value("${jwt.secret}") String jwtSecret) {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
    }

    public String generateAccessToken(String userEmail) {
        return generateToken(userEmail, "access", accessTokenValidityInMs);
    }

    public String generateRefreshToken(String userEmail) {
        return generateToken(userEmail, "refresh", refreshTokenValidityInMs);
    }

    private String generateToken(String userEmail, String type, long expiry) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim("type", type)
                .subject(userEmail)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry, ChronoUnit.MILLIS)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public boolean validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        return Objects.equals(claims.get("type"), "access");
    }

    public boolean validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return Objects.equals(claims.get("type"), "refresh");
    }


    public Claims parseClaims(String token) {
        String rawToken = token.startsWith("Bearer ") ? token.replace("Bearer ", "") : token;
        try {
            return Jwts.parser()
                    .verifyWith(secretKey).build()
                    .parseSignedClaims(rawToken).getPayload();
        } catch (Exception e) {
            throw new InvalidOrExpiredTokenException("Invalid or expired token");
        }
    }

}
