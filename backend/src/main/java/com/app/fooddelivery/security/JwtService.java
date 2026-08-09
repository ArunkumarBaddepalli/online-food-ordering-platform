package com.app.fooddelivery.security;

import com.app.fooddelivery.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and reads the signed tokens that identify a caller.
 *
 * The token carries the user id, email and role. It is signed with a server
 * secret, so a client can read it but cannot alter it — changing the role
 * inside a token invalidates the signature.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiry-hours:24}") long expiryHours) {

        // A development fallback keeps the app runnable straight after cloning.
        // Set JWT_SECRET in the environment for anything beyond local use.
        String effective = (secret == null || secret.isBlank())
                ? "local-development-only-secret-key-change-me-in-production"
                : secret;

        this.key = Keys.hmacShaKeyFor(effective.getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = expiryHours * 60 * 60 * 1000;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(key)
                .compact();
    }

    /** Returns the claims, or null when the token is missing, altered or expired. */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpirySeconds() {
        return expiryMillis / 1000;
    }
}
