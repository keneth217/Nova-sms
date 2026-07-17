package com.novastack.sms.security;

import com.novastack.sms.config.AppProperties;
import com.novastack.sms.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + appProperties.getJwt().getExpirationMs());

        var builder = Jwts.builder()
                .subject(principal.getEmail())
                .claim("uid", principal.getId() != null ? principal.getId().toString() : null)
                .claim("role", principal.getRole().name())
                .claim("ver", principal.getTokenVersion())
                .issuedAt(now)
                .expiration(expiry);

        if (principal.getOrganizationId() != null) {
            builder.claim("orgId", principal.getOrganizationId().toString());
        }

        return builder.signWith(signingKey()).compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public UserRole extractRole(String token) {
        return UserRole.valueOf(parseClaims(token).get("role", String.class));
    }

    public UUID extractOrganizationId(String token) {
        String orgId = parseClaims(token).get("orgId", String.class);
        return orgId != null ? UUID.fromString(orgId) : null;
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        Number versionClaim = claims.get("ver", Number.class);
        long tokenVersion = versionClaim != null ? versionClaim.longValue() : 0L;
        return email.equals(principal.getEmail())
                && tokenVersion == principal.getTokenVersion()
                && !claims.getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        String secret = appProperties.getJwt().getSecret();
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(padKey(keyBytes));
    }

    private byte[] padKey(byte[] keyBytes) {
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
        return padded;
    }
}
