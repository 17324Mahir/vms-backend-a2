package com.starkgroup.vms.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Service managing JWT creation, validation parameters, and secret key lifecycle.
 * Uses official HMAC-SHA256 (HS256) signing with Spring Security and Nimbus JOSE.
 */
@Service
public class JwtService {

    @Value("${app.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration-seconds:900}")
    private long expirationSeconds;

    @Value("${app.security.jwt.issuer:stark-vms-backend}")
    private String issuer;

    @Value("${app.security.jwt.audience:stark-vms-frontend}")
    private String audience;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "Missing JWT secret configuration. app.security.jwt.secret (JWT_SECRET) must be set."
            );
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "Invalid JWT secret configuration. Secret must be at least 32 bytes (256 bits) for HMAC-SHA256."
            );
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(String email) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(expirationSeconds);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .issuer(issuer)
                    .audience(List.of(audience))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.HS256)
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claimsSet
            );

            JWSSigner signer = new MACSigner(secretKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign access token", e);
        }
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAudience() {
        return audience;
    }
}
