package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateDriverRequest;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.service.JwtService;
import com.starkgroup.vms.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Newly registered user has empty fleet and isolated dashboard, separate from test account")
    void newRegisteredUser_hasEmptyFleetAndIsolatedDashboard() throws Exception {
        String newUserEmail = "brand_new_user_" + System.currentTimeMillis() + "@stark.test";
        String newUserToken = "Bearer " + jwtService.generateToken(newUserEmail);

        // 1. New user has empty vehicle list
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 2. New user has empty dashboard stats (0 total, 0 active, 0 drivers, 0 cost)
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles", is(0)))
                .andExpect(jsonPath("$.activeVehicles", is(0)))
                .andExpect(jsonPath("$.totalDrivers", is(0)))
                .andExpect(jsonPath("$.upcomingExpiries", hasSize(0)))
                .andExpect(jsonPath("$.recentMaintenance", hasSize(0)));

        // 3. New user creates a vehicle
        CreateVehicleRequest createRequest = new CreateVehicleRequest(
                "USER-ISOLATE-101",
                "New User Logistics",
                "Electric",
                "Tesla",
                "Model 3",
                VehicleStatus.ACTIVE
        );

        String location = mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleNumber", is("USER-ISOLATE-101")))
                .andReturn().getResponse().getHeader("Location");

        // 4. New user now sees their 1 vehicle and updated dashboard
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vehicleNumber", is("USER-ISOLATE-101")));

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVehicles", is(1)))
                .andExpect(jsonPath("$.activeVehicles", is(1)));

        // 5. Test account does NOT see the new user's vehicle
        String testUserToken = "Bearer " + jwtService.generateToken(SecurityUtils.DEFAULT_USER_EMAIL);
        mockMvc.perform(get("/api/vehicles")
                        .header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].vehicleNumber", not(hasItem("USER-ISOLATE-101"))));
        mockMvc.perform(get(location).header("Authorization", testUserToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/vehicles/companies").header("Authorization", newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("New User Logistics")));
        mockMvc.perform(get("/api/vehicles/companies").header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasItem("New User Logistics"))));

        CreateDriverRequest driverRequest = new CreateDriverRequest();
        driverRequest.setName("Private Test Driver");
        driverRequest.setLicenseNumber("PRIVATE-DRIVER-1");
        driverRequest.setPhone("01000000001");
        String driver = mockMvc.perform(post("/api/drivers")
                        .header("Authorization", newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(driverRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long driverId = objectMapper.readTree(driver).path("id").asLong();
        mockMvc.perform(get("/api/drivers/{id}", driverId).header("Authorization", testUserToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/drivers/available").header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("Private Test Driver"))));
    }
}
