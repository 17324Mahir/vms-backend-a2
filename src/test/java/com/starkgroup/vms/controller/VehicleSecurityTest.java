package com.starkgroup.vms.controller;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.starkgroup.vms.service.JwtService;
import com.starkgroup.vms.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    @DisplayName("GET /api/vehicles without token returns 401 Unauthorized with JSON ErrorResponse")
    void getVehicles_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Full authentication is required")));
    }

    @Test
    @DisplayName("POST /api/vehicles without token returns 401 Unauthorized")
    void postVehicle_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("PUT /api/vehicles/1 without token returns 401 Unauthorized")
    void putVehicle_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("DELETE /api/vehicles/1 without token returns 401 Unauthorized")
    void deleteVehicle_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/vehicles/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/vehicles with valid JWT returns 200 OK")
    void getVehicles_withValidToken_returns200() throws Exception {
        String validToken = jwtService.generateToken("test@starkgroup.test");
        when(vehicleService.getAllVehicles()).thenReturn(List.of());

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/vehicles with tampered JWT signature returns 401 Unauthorized")
    void getVehicles_withTamperedToken_returns401() throws Exception {
        String validToken = jwtService.generateToken("test@starkgroup.test");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/vehicles with expired JWT returns 401 Unauthorized")
    void getVehicles_withExpiredToken_returns401() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test@starkgroup.test")
                .issuer(jwtService.getIssuer())
                .audience(List.of(jwtService.getAudience()))
                .issueTime(Date.from(past.minusSeconds(60)))
                .expirationTime(Date.from(past))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claims
        );
        JWSSigner signer = new MACSigner(jwtService.getSecretKey());
        signedJWT.sign(signer);
        String expiredToken = signedJWT.serialize();

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/vehicles with wrong issuer returns 401 Unauthorized")
    void getVehicles_withWrongIssuer_returns401() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test@starkgroup.test")
                .issuer("malicious-issuer")
                .audience(List.of(jwtService.getAudience()))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(900)))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claims
        );
        JWSSigner signer = new MACSigner(jwtService.getSecretKey());
        signedJWT.sign(signer);
        String wrongIssuerToken = signedJWT.serialize();

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + wrongIssuerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/vehicles with wrong audience returns 401 Unauthorized")
    void getVehicles_withWrongAudience_returns401() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test@starkgroup.test")
                .issuer(jwtService.getIssuer())
                .audience(List.of("wrong-audience"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(900)))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claims
        );
        JWSSigner signer = new MACSigner(jwtService.getSecretKey());
        signedJWT.sign(signer);
        String wrongAudienceToken = signedJWT.serialize();

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer " + wrongAudienceToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("OPTIONS /api/vehicles CORS preflight from allowed origin succeeds with 200")
    void optionsVehicles_preflight_succeedsWith200() throws Exception {
        mockMvc.perform(options("/api/vehicles")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));
    }
}
