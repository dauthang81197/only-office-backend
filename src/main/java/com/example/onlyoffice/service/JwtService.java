package com.example.onlyoffice.service;

import com.example.onlyoffice.config.OnlyOfficeProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Signs OnlyOffice editor configs and verifies tokens on incoming callbacks,
 * using the HS256 shared secret expected by the Document Server.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final OnlyOfficeProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public boolean enabled() {
        return StringUtils.hasText(props.getJwtSecret());
    }

    /** Signs an arbitrary claims map (the editor config payload). */
    public String sign(Map<String, Object> payload) {
        return Jwts.builder()
                .claims(payload)
                .signWith(key())
                .compact();
    }

    /** Parses and validates a token, returning its claims. Throws if invalid. */
    public Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
