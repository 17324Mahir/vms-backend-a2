package com.starkgroup.vms.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.domain.DocumentType;
import com.starkgroup.vms.domain.MaintenanceType;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateDocumentRequest;
import com.starkgroup.vms.dto.CreateMaintenanceRequest;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test-Driven Specification for ADR-002: Data Ownership Contract & Multi-Tenant Isolation.
 * Verifies that Cross-Tenant access attempts across vehicles, documents, and maintenance
 * are strictly denied (404/Not Found or 403/Forbidden) to prevent IDOR and data leakage.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DataOwnershipSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("TDD Contract: Tenant B cannot read, update, mutate, or delete Tenant A's assets")
    void crossTenantDataOwnershipContract_enforcesStrictZeroTrustIsolation() throws Exception {
        String tenantAEmail = "tenant_a_" + System.currentTimeMillis() + "@acme-logistics.test";
        String tenantBEmail = "tenant_b_" + System.currentTimeMillis() + "@apex-freight.test";

        String tokenA = "Bearer " + jwtService.generateToken(tenantAEmail);
        String tokenB = "Bearer " + jwtService.generateToken(tenantBEmail);

        // 1. Tenant A creates a Vehicle
        CreateVehicleRequest vReqA = new CreateVehicleRequest(
                "T-ALPHA-" + (System.currentTimeMillis() % 100000),
                "Acme Logistics Corp",
                "Diesel",
                "Volvo",
                "FH16",
                VehicleStatus.ACTIVE
        );

        MvcResult vehicleResult = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vReqA)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode vehicleJson = objectMapper.readTree(vehicleResult.getResponse().getContentAsString());
        long vehicleId = vehicleJson.get("id").asLong();

        // 2. Tenant A creates a Document under this vehicle
        CreateDocumentRequest docReq = new CreateDocumentRequest(
                DocumentType.REGISTRATION,
                "DOC-ACME-" + System.currentTimeMillis(),
                LocalDate.now().minusDays(30),
                LocalDate.now().plusMonths(12),
                "registration.pdf",
                "application/pdf",
                "https://secure.vault.test/acme-reg.pdf",
                "Initial registration"
        );

        MvcResult docResult = mockMvc.perform(post("/api/vehicles/" + vehicleId + "/documents")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode docJson = objectMapper.readTree(docResult.getResponse().getContentAsString());
        long documentId = docJson.get("id").asLong();

        // 3. Tenant A creates a Maintenance record under this vehicle
        CreateMaintenanceRequest maintReq = new CreateMaintenanceRequest(
                LocalDate.now().minusDays(2),
                MaintenanceType.ROUTINE_SERVICE,
                new BigDecimal("850.00"),
                "Acme Workshop HQ",
                45000,
                "Annual inspection and brake disc replacement",
                VehicleStatus.ACTIVE
        );

        MvcResult maintResult = mockMvc.perform(post("/api/vehicles/" + vehicleId + "/maintenance")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode maintJson = objectMapper.readTree(maintResult.getResponse().getContentAsString());
        long maintenanceId = maintJson.get("id").asLong();

        // =========================================================================
        // ZERO-TRUST TENANT ISOLATION ASSERTIONS (Tenant B attempts to access A)
        // =========================================================================

        // [VIOLATION ATTEMPT 1]: Tenant B attempts to read Tenant A's vehicle by ID
        mockMvc.perform(get("/api/vehicles/" + vehicleId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 2]: Tenant B attempts to update Tenant A's vehicle
        CreateVehicleRequest maliciousUpdate = new CreateVehicleRequest(
                vReqA.vehicleNumber(),
                "Hacked Logistics",
                "Electric",
                "Tesla",
                "Semi",
                VehicleStatus.INACTIVE
        );
        mockMvc.perform(put("/api/vehicles/" + vehicleId)
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 3]: Tenant B attempts to attach a document to Tenant A's vehicle
        CreateDocumentRequest maliciousDoc = new CreateDocumentRequest(
                DocumentType.INSURANCE,
                "DOC-HACK-001",
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                "exploit.pdf",
                "application/pdf",
                "https://malicious.test/exploit.pdf",
                "Unauthorized injection"
        );
        mockMvc.perform(post("/api/vehicles/" + vehicleId + "/documents")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousDoc)))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 4]: Tenant B attempts to fetch Tenant A's document by ID
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 5]: Tenant B attempts to delete Tenant A's document
        mockMvc.perform(delete("/api/documents/" + documentId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 6]: Tenant B attempts to read Tenant A's maintenance record by ID
        mockMvc.perform(get("/api/maintenance/" + maintenanceId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 7]: Tenant B attempts to update Tenant A's maintenance record
        CreateMaintenanceRequest maliciousMaintUpdate = new CreateMaintenanceRequest(
                LocalDate.now(),
                MaintenanceType.ENGINE_REPAIR,
                new BigDecimal("999999.00"),
                "Fake Workshop",
                99999,
                "Tampered record",
                VehicleStatus.MAINTENANCE
        );
        mockMvc.perform(put("/api/maintenance/" + maintenanceId)
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousMaintUpdate)))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 8]: Tenant B attempts to delete Tenant A's maintenance record
        mockMvc.perform(delete("/api/maintenance/" + maintenanceId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 9]: Tenant B attempts to delete Tenant A's vehicle
        mockMvc.perform(delete("/api/vehicles/" + vehicleId)
                        .header("Authorization", tokenB))
                .andExpect(status().isNotFound());

        // [VIOLATION ATTEMPT 10]: Tenant B queries vehicle listing and dashboard stats
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles", is(0)))
                .andExpect(jsonPath("$.totalMaintenanceCost", is(0)));
    }
}
