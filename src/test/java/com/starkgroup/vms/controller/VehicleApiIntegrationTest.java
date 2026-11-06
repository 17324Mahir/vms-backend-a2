package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.domain.User;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.repository.UserRepository;
import com.starkgroup.vms.service.JwtService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Stark Group VMS Vehicle REST APIs.
 *
 * <p>Exercises the full request lifecycle through the real HTTP boundary (MockMvc),
 * Spring Security filter chain (stateless JWT Bearer tokens), real controllers,
 * services, repositories, and the isolated PostgreSQL database instance.
 *
 * <p>Validates:
 * <ul>
 *   <li>Real authentication flow via POST /api/auth/login to obtain Bearer JWT tokens.</li>
 *   <li>Complete repeatable CRUD workflow (Create, Read list, Read one, Update, Read again, Delete, Confirm absence).</li>
 *   <li>Direct SQL verification via JdbcTemplate after create, update, and delete.</li>
 *   <li>Duplicate vehicle number business rules and case/whitespace normalization (409 Conflict).</li>
 *   <li>Input validation rules, missing fields, overlength constraints, invalid status, and malformed JSON (400 Bad Request).</li>
 *   <li>Resource ID constraints, non-positive IDs, and 404 Not Found handling.</li>
 *   <li>Security protection ensuring unauthenticated requests return 401 Unauthorized.</li>
 *   <li>Safe test data cleanup via @AfterEach parameterized SQL deletes.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String testUserEmail = "it-test-user@starkgroup.test";

    // Synthetic test password for isolated integration testing
    private static final String TEST_USER_PASSWORD = "SyntheticTestPassword@123";

    private String authToken;
    private final List<Long> createdVehicleIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        // Ensure synthetic test user exists in repository for real auth journey
        if (!userRepository.existsByEmailIgnoreCase(testUserEmail)) {
            userRepository.save(new User("Test Admin", testUserEmail, passwordEncoder.encode(TEST_USER_PASSWORD), "USER"));
        }
        // Authenticate via the real login endpoint to exercise the real auth journey
        authToken = obtainAuthToken();
    }

    @AfterEach
    void tearDown() {
        // Safely clean up only records created during these integration tests
        for (Long id : createdVehicleIds) {
            try {
                jdbcTemplate.update("DELETE FROM vehicles WHERE id = ?", id);
            } catch (Exception ignored) {
            }
        }
        createdVehicleIds.clear();

        try {
            userRepository.findByEmailIgnoreCase(testUserEmail).ifPresent(userRepository::delete);
        } catch (Exception ignored) {
        }
    }

    /**
     * Authenticates through the real POST /api/auth/login endpoint using the configured
     * course test credentials and returns the Bearer authorization header value.
     */
    private String obtainAuthToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest(testUserEmail, TEST_USER_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + responseNode.get("accessToken").asText();
    }

    @Test
    @DisplayName("Complete CRUD lifecycle with direct PostgreSQL state verification")
    void completeVehicleCrudLifecycleWithDirectSqlVerification() throws Exception {
        // A. Create Vehicle
        CreateVehicleRequest createRequest = new CreateVehicleRequest(
                "  it-veh-1001  ",
                "Stark Aviation",
                "Jet",
                "Stark Industries",
                "Mark I",
                VehicleStatus.ACTIVE
        );

        MvcResult createResult = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/vehicles/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.vehicleNumber", is("IT-VEH-1001")))
                .andExpect(jsonPath("$.company", is("Stark Aviation")))
                .andExpect(jsonPath("$.type", is("Jet")))
                .andExpect(jsonPath("$.brand", is("Stark Industries")))
                .andExpect(jsonPath("$.model", is("Mark I")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long vehicleId = createdJson.get("id").asLong();
        createdVehicleIds.add(vehicleId);

        // Direct SQL: Assert row exists in PostgreSQL and all canonical fields match
        Integer countAfterCreate = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vehicles WHERE id = ?", Integer.class, vehicleId);
        assertEquals(1, countAfterCreate);

        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT vehicle_number, company, type, brand, model, status FROM vehicles WHERE id = ?", vehicleId);
        assertEquals("IT-VEH-1001", dbRow.get("vehicle_number"));
        assertEquals("Stark Aviation", dbRow.get("company"));
        assertEquals("Jet", dbRow.get("type"));
        assertEquals("Stark Industries", dbRow.get("brand"));
        assertEquals("Mark I", dbRow.get("model"));
        assertEquals("ACTIVE", dbRow.get("status"));

        // B. Read List: verify created vehicle appears
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)))
                .andExpect(jsonPath("$[*].vehicleNumber", hasItem("IT-VEH-1001")));

        // C. Read One: fetch by captured ID
        mockMvc.perform(get("/api/vehicles/{id}", vehicleId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(vehicleId.intValue())))
                .andExpect(jsonPath("$.vehicleNumber", is("IT-VEH-1001")))
                .andExpect(jsonPath("$.company", is("Stark Aviation")))
                .andExpect(jsonPath("$.type", is("Jet")))
                .andExpect(jsonPath("$.brand", is("Stark Industries")))
                .andExpect(jsonPath("$.model", is("Mark I")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // D. Update Vehicle: modify fields preserving same ID
        UpdateVehicleRequest updateRequest = new UpdateVehicleRequest(
                "IT-VEH-1001",
                "Stark Space",
                "Rocket",
                "Stark Industries",
                "Mark II",
                VehicleStatus.MAINTENANCE
        );

        mockMvc.perform(put("/api/vehicles/{id}", vehicleId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(vehicleId.intValue())))
                .andExpect(jsonPath("$.vehicleNumber", is("IT-VEH-1001")))
                .andExpect(jsonPath("$.company", is("Stark Space")))
                .andExpect(jsonPath("$.type", is("Rocket")))
                .andExpect(jsonPath("$.brand", is("Stark Industries")))
                .andExpect(jsonPath("$.model", is("Mark II")))
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));

        // Direct SQL: Assert row in PostgreSQL was updated and same ID preserved
        Map<String, Object> updatedDbRow = jdbcTemplate.queryForMap(
                "SELECT vehicle_number, company, type, brand, model, status FROM vehicles WHERE id = ?", vehicleId);
        assertEquals("IT-VEH-1001", updatedDbRow.get("vehicle_number"));
        assertEquals("Stark Space", updatedDbRow.get("company"));
        assertEquals("Rocket", updatedDbRow.get("type"));
        assertEquals("Mark II", updatedDbRow.get("model"));
        assertEquals("MAINTENANCE", updatedDbRow.get("status"));

        // E. Read Again: confirm updated values via HTTP
        mockMvc.perform(get("/api/vehicles/{id}", vehicleId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company", is("Stark Space")))
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));

        // F. Delete Vehicle: assert 204 No Content with empty body
        mockMvc.perform(delete("/api/vehicles/{id}", vehicleId)
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Direct SQL: Assert row is absent from PostgreSQL
        Integer countAfterDelete = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vehicles WHERE id = ?", Integer.class, vehicleId);
        assertEquals(0, countAfterDelete);

        // G. Confirm Absence: HTTP GET returns 404 Not Found and absent from list
        mockMvc.perform(get("/api/vehicles/{id}", vehicleId)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")));

        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].vehicleNumber", not(hasItem("IT-VEH-1001"))));
    }

    @Test
    @DisplayName("Duplicate vehicle number business rules and normalization enforcement")
    void duplicateVehicleNumberRules_createAndUpdateConflictsRejected() throws Exception {
        // 1. Create initial vehicle
        CreateVehicleRequest initial = new CreateVehicleRequest(
                "IT-DUP-1001",
                "Stark Logistics",
                "Truck",
                "Volvo",
                "FH16",
                VehicleStatus.ACTIVE
        );

        MvcResult result1 = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asLong();
        createdVehicleIds.add(id1);

        // Direct SQL: Exactly 1 row for IT-DUP-1001
        Integer count1 = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vehicles WHERE vehicle_number = ?", Integer.class, "IT-DUP-1001");
        assertEquals(1, count1);

        // 2. Reject exact duplicate create with 409 Conflict
        CreateVehicleRequest exactDuplicate = new CreateVehicleRequest(
                "IT-DUP-1001",
                "Stark Defense",
                "Armored",
                "General Dynamics",
                "Stryker",
                VehicleStatus.ACTIVE
        );

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exactDuplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("DUPLICATE_VEHICLE_NUMBER")))
                .andExpect(jsonPath("$.message", is("A vehicle with this Vehicle Number already exists.")));

        // Direct SQL: Count is still 1
        Integer countAfterDup = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vehicles WHERE vehicle_number = ?", Integer.class, "IT-DUP-1001");
        assertEquals(1, countAfterDup);

        // 3. Reject case-insensitive & whitespace duplicate create
        CreateVehicleRequest normalizedDuplicate = new CreateVehicleRequest(
                "  it-dup-1001  ",
                "Stark Maritime",
                "Boat",
                "SeaRay",
                "Sundancer",
                VehicleStatus.ACTIVE
        );

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalizedDuplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_VEHICLE_NUMBER")));

        // 4. Updating vehicle keeping its own number succeeds
        UpdateVehicleRequest selfUpdate = new UpdateVehicleRequest(
                "it-dup-1001",
                "Stark Logistics Fleet",
                "Heavy Truck",
                "Volvo",
                "FH16",
                VehicleStatus.ACTIVE
        );

        mockMvc.perform(put("/api/vehicles/{id}", id1)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company", is("Stark Logistics Fleet")));

        // 5. Create a second distinct vehicle
        CreateVehicleRequest second = new CreateVehicleRequest(
                "IT-DUP-2002",
                "Stark Energy",
                "Utility",
                "Ford",
                "F-150 Lightning",
                VehicleStatus.ACTIVE
        );

        MvcResult result2 = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asLong();
        createdVehicleIds.add(id2);

        // 6. Updating second vehicle to first vehicle's number returns 409 Conflict
        UpdateVehicleRequest conflictUpdate = new UpdateVehicleRequest(
                "IT-DUP-1001",
                "Stark Energy",
                "Utility",
                "Ford",
                "F-150 Lightning",
                VehicleStatus.ACTIVE
        );

        mockMvc.perform(put("/api/vehicles/{id}", id2)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_VEHICLE_NUMBER")));

        // Direct SQL: Both vehicles remain unchanged in PostgreSQL
        String v1Number = jdbcTemplate.queryForObject(
                "SELECT vehicle_number FROM vehicles WHERE id = ?", String.class, id1);
        String v2Number = jdbcTemplate.queryForObject(
                "SELECT vehicle_number FROM vehicles WHERE id = ?", String.class, id2);
        assertEquals("IT-DUP-1001", v1Number);
        assertEquals("IT-DUP-2002", v2Number);
    }

    @Test
    @DisplayName("Input validation failures return 400 Bad Request with no database mutations")
    void validationFailures_requestRejectionsAndNoDatabaseMutations() throws Exception {
        Integer initialCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vehicles", Integer.class);

        // 1. Missing required field (empty vehicle number)
        CreateVehicleRequest blankNumber = new CreateVehicleRequest(
                "", "Stark", "Van", "Ford", "Transit", VehicleStatus.ACTIVE);

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("vehicleNumber")));

        // 2. Whitespace-only required field
        CreateVehicleRequest whitespaceNumber = new CreateVehicleRequest(
                "   ", "Stark", "Van", "Ford", "Transit", VehicleStatus.ACTIVE);

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(whitespaceNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 3. Overlength field (> 64 characters)
        CreateVehicleRequest overlengthNumber = new CreateVehicleRequest(
                "A".repeat(65), "Stark", "Van", "Ford", "Transit", VehicleStatus.ACTIVE);

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlengthNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 4. Missing required status (null)
        CreateVehicleRequest missingStatus = new CreateVehicleRequest(
                "IT-VAL-101", "Stark", "Van", "Ford", "Transit", null);

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 5. Invalid status enum value
        String invalidStatusJson = "{\"vehicleNumber\":\"IT-VAL-999\",\"company\":\"Stark\",\"type\":\"Van\",\"brand\":\"Ford\",\"model\":\"Transit\",\"status\":\"FLYING\"}";

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStatusJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));

        // 6. Malformed JSON payload
        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json-payload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));

        // 7. Missing request body
        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));

        // Direct SQL: Assert no rows were inserted into PostgreSQL
        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vehicles", Integer.class);
        assertEquals(initialCount, finalCount);
    }

    @Test
    @DisplayName("Resource ID constraints, non-positive IDs, and 404 handling")
    void resourceIdValidationAndMissingResources() throws Exception {
        // 1. Positive valid missing ID returns 404
        mockMvc.perform(get("/api/vehicles/{id}", 999999999L)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")));

        // 2. Non-numeric ID returns 400 INVALID_ARGUMENT_TYPE
        mockMvc.perform(get("/api/vehicles/invalid-id")
                        .header("Authorization", authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("INVALID_ARGUMENT_TYPE")));

        // 3. Non-positive ID (0 and -1) returns 400 VALIDATION_FAILED with explicit message
        mockMvc.perform(get("/api/vehicles/0")
                        .header("Authorization", authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem(containsString("positive"))));

        mockMvc.perform(get("/api/vehicles/-1")
                        .header("Authorization", authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors[*].message", hasItem(containsString("positive"))));

        // Non-positive ID on PUT returns 400
        UpdateVehicleRequest updateReq = new UpdateVehicleRequest(
                "IT-NEG-1", "Stark", "Van", "Ford", "Transit", VehicleStatus.ACTIVE);

        mockMvc.perform(put("/api/vehicles/-1")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // Non-positive ID on DELETE returns 400
        mockMvc.perform(delete("/api/vehicles/-1")
                        .header("Authorization", authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Spring Security endpoint protection: unauthenticated requests return 401 Unauthorized")
    void securityProtection_unauthenticatedRequestsReturn401() throws Exception {
        // Without Authorization header
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/vehicles/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(put("/api/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(delete("/api/vehicles/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        // Invalid Bearer token
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", "Bearer invalid.signed.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        // Public login endpoint with invalid credentials returns 401 INVALID_CREDENTIALS
        LoginRequest badLogin = new LoginRequest(testUserEmail, "WrongPassword@999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", is("Email or password is incorrect.")));
    }
}
