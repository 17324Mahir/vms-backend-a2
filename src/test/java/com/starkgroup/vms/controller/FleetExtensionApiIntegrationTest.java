package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.domain.DocumentType;
import com.starkgroup.vms.domain.DriverStatus;
import com.starkgroup.vms.domain.MaintenanceType;
import com.starkgroup.vms.domain.User;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateDocumentRequest;
import com.starkgroup.vms.dto.CreateDriverRequest;
import com.starkgroup.vms.dto.CreateMaintenanceRequest;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FleetExtensionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_USER_EMAIL = "ext-tester@starkgroup.test";
    private static final String TEST_USER_PASSWORD = "SyntheticTestPassword@123";

    private String authHeader;

    @BeforeEach
    void setUp() throws Exception {
        if (!userRepository.existsByEmailIgnoreCase(TEST_USER_EMAIL)) {
            userRepository.save(new User("Extension Tester", TEST_USER_EMAIL, passwordEncoder.encode(TEST_USER_PASSWORD), "ADMIN"));
        }

        LoginRequest loginRequest = new LoginRequest(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        authHeader = "Bearer " + responseNode.get("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM vehicle_maintenance_records WHERE workshop_vendor LIKE 'Test%' OR workshop_vendor LIKE 'Stark%'");
        jdbcTemplate.execute("DELETE FROM vehicle_documents WHERE document_number LIKE 'DOC-TEST%'");
        jdbcTemplate.execute("DELETE FROM vehicles WHERE vehicle_number LIKE 'EXT-%'");
        jdbcTemplate.execute("DELETE FROM drivers WHERE license_number LIKE 'DL-TEST-%'");
        try {
            userRepository.findByEmailIgnoreCase(TEST_USER_EMAIL).ifPresent(userRepository::delete);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Driver CRUD, duplicate license check, and available drivers endpoint")
    void testDriverCrudAndDuplicateValidation() throws Exception {
        // 1. Create Driver
        CreateDriverRequest driverReq = new CreateDriverRequest(
                "Pepper Potts",
                "DL-TEST-998811",
                "+1-555-0199",
                "pepper@starkgroup.com",
                DriverStatus.AVAILABLE,
                LocalDate.now().plusYears(2)
        );

        MvcResult createResult = mockMvc.perform(post("/api/drivers")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name", is("Pepper Potts")))
                .andExpect(jsonPath("$.licenseNumber", is("DL-TEST-998811")))
                .andExpect(jsonPath("$.expiryStatus", is("VALID")))
                .andReturn();

        // 2. Duplicate License Number check (Expect 409 Conflict)
        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_LICENSE_NUMBER")));

        // 3. Search Drivers with Pagination
        mockMvc.perform(get("/api/drivers?page=0&size=5&search=Pepper")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].name", is("Pepper Potts")));

        // 4. Get available drivers
        mockMvc.perform(get("/api/drivers/available")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Driver Assignment & Unassignment to Vehicle")
    void testDriverAssignmentWorkflow() throws Exception {
        // 1. Create Driver
        CreateDriverRequest driverReq = new CreateDriverRequest(
                "Happy Hogan",
                "DL-TEST-445566",
                "+1-555-0144",
                "happy@starkgroup.com",
                DriverStatus.AVAILABLE,
                LocalDate.now().plusMonths(6)
        );
        MvcResult driverRes = mockMvc.perform(post("/api/drivers")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long driverId = objectMapper.readTree(driverRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Create Vehicle
        CreateVehicleRequest vehicleReq = new CreateVehicleRequest(
                "EXT-9001",
                "Stark Security",
                "SUV",
                "Chevrolet",
                "Suburban",
                VehicleStatus.ACTIVE
        );
        MvcResult vehicleRes = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long vehicleId = objectMapper.readTree(vehicleRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Assign Driver to Vehicle
        mockMvc.perform(post("/api/vehicles/" + vehicleId + "/assign-driver/" + driverId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedDriver.name", is("Happy Hogan")))
                .andExpect(jsonPath("$.assignedDriver.id", is((int) driverId)));

        // 4. Verify Driver status is now ASSIGNED
        mockMvc.perform(get("/api/drivers/" + driverId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ASSIGNED")))
                .andExpect(jsonPath("$.assignedVehicleNumber", is("EXT-9001")));

        // 5. Unassign Driver
        mockMvc.perform(post("/api/vehicles/" + vehicleId + "/unassign-driver")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedDriver").doesNotExist());

        // 6. Verify Driver status returned to AVAILABLE
        mockMvc.perform(get("/api/drivers/" + driverId)
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    @DisplayName("Vehicle Documents Upload, Expiry Calculation, and Deletion")
    void testVehicleDocumentWorkflow() throws Exception {
        // Create Vehicle
        CreateVehicleRequest vehicleReq = new CreateVehicleRequest(
                "EXT-DOC-01",
                "Stark Industries",
                "Sedan",
                "Audi",
                "e-tron GT",
                VehicleStatus.ACTIVE
        );
        MvcResult vehicleRes = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long vehicleId = objectMapper.readTree(vehicleRes.getResponse().getContentAsString()).get("id").asLong();

        // Upload Document
        CreateDocumentRequest docReq = new CreateDocumentRequest(
                DocumentType.INSURANCE,
                "DOC-TEST-INS-001",
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusMonths(6),
                "insurance_policy.pdf",
                "application/pdf",
                "JVBERi0xLjQKJUZpbGUgY29udGVudHM=",
                "Commercial fleet insurance policy"
        );

        MvcResult docRes = mockMvc.perform(post("/api/vehicles/" + vehicleId + "/documents")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentType", is("INSURANCE")))
                .andExpect(jsonPath("$.fileName", is("insurance_policy.pdf")))
                .andExpect(jsonPath("$.expiryStatus", is("VALID")))
                .andReturn();

        long docId = objectMapper.readTree(docRes.getResponse().getContentAsString()).get("id").asLong();

        // List Documents for Vehicle
        mockMvc.perform(get("/api/vehicles/" + vehicleId + "/documents")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentNumber", is("DOC-TEST-INS-001")));

        // Delete Document
        mockMvc.perform(delete("/api/vehicles/" + vehicleId + "/documents/" + docId)
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        // Verify Empty List
        mockMvc.perform(get("/api/vehicles/" + vehicleId + "/documents")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Vehicle Maintenance Recording, History, and Dashboard Aggregation")
    void testMaintenanceAndDashboardWorkflow() throws Exception {
        // Create Vehicle
        CreateVehicleRequest vehicleReq = new CreateVehicleRequest(
                "EXT-MAINT-01",
                "Stark Aviation",
                "Cargo Van",
                "Mercedes-Benz",
                "Sprinter",
                VehicleStatus.ACTIVE
        );
        MvcResult vehicleRes = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicleReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long vehicleId = objectMapper.readTree(vehicleRes.getResponse().getContentAsString()).get("id").asLong();

        // Record Maintenance
        CreateMaintenanceRequest maintReq = new CreateMaintenanceRequest(
                LocalDate.now(),
                MaintenanceType.ROUTINE_SERVICE,
                new BigDecimal("1250.50"),
                "Stark Precision Workshop",
                45000,
                "50,000 km periodic service & brake pad check",
                VehicleStatus.ACTIVE
        );

        mockMvc.perform(post("/api/vehicles/" + vehicleId + "/maintenance")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maintReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cost", is(1250.50)))
                .andExpect(jsonPath("$.workshopVendor", is("Stark Precision Workshop")));

        // Verify Maintenance History for Vehicle
        mockMvc.perform(get("/api/vehicles/" + vehicleId + "/maintenance")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].odometerReading", is(45000)));

        // Verify Dashboard Stats
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalMaintenanceRecords", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalMaintenanceCost", greaterThanOrEqualTo(1250.50)));
    }
}
