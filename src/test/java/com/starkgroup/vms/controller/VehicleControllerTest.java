package com.starkgroup.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starkgroup.vms.config.CorsConfig;
import com.starkgroup.vms.domain.VehicleStatus;
import com.starkgroup.vms.dto.CreateVehicleRequest;
import com.starkgroup.vms.dto.UpdateVehicleRequest;
import com.starkgroup.vms.dto.VehicleResponse;
import com.starkgroup.vms.exception.DuplicateVehicleNumberException;
import com.starkgroup.vms.exception.GlobalExceptionHandler;
import com.starkgroup.vms.exception.VehicleNotFoundException;
import com.starkgroup.vms.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CorsConfig.class})
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    // --- Create Vehicle (POST /api/vehicles) ---

    @Test
    @DisplayName("POST /api/vehicles with valid payload returns 201 Created and Location header")
    void createVehicle_withValidPayload_returns201AndLocationHeader() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SG-1001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        VehicleResponse response = new VehicleResponse(
                1L,
                "SG-1001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleService.createVehicle(any(CreateVehicleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/vehicles/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.vehicleNumber", is("SG-1001")))
                .andExpect(jsonPath("$.company", is("Stark Industries")))
                .andExpect(jsonPath("$.type", is("Sedan")))
                .andExpect(jsonPath("$.brand", is("Tesla")))
                .andExpect(jsonPath("$.model", is("Model S")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/vehicles with blank required fields returns 400 Bad Request with field errors")
    void createVehicle_withBlankFields_returns400WithFieldErrors() throws Exception {
        CreateVehicleRequest invalidRequest = new CreateVehicleRequest(
                "",
                "   ",
                "",
                "",
                "",
                null
        );

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))));

        verify(vehicleService, never()).createVehicle(any());
    }

    @Test
    @DisplayName("POST /api/vehicles with invalid status value returns 400 Bad Request")
    void createVehicle_withInvalidStatus_returns400() throws Exception {
        String invalidJson = """
                {
                  "vehicleNumber": "SG-1001",
                  "company": "Stark Industries",
                  "type": "Sedan",
                  "brand": "Tesla",
                  "model": "Model S",
                  "status": "FLYING"
                }
                """;

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));

        verify(vehicleService, never()).createVehicle(any());
    }

    @Test
    @DisplayName("POST /api/vehicles with duplicate Vehicle Number returns 409 Conflict")
    void createVehicle_withDuplicateVehicleNumber_returns409Conflict() throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "SG-1001",
                "Stark Industries",
                "Sedan",
                "Tesla",
                "Model S",
                VehicleStatus.ACTIVE
        );

        when(vehicleService.createVehicle(any(CreateVehicleRequest.class)))
                .thenThrow(new DuplicateVehicleNumberException("A vehicle with this Vehicle Number already exists."));

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("DUPLICATE_VEHICLE_NUMBER")))
                .andExpect(jsonPath("$.message", is("A vehicle with this Vehicle Number already exists.")));
    }

    // --- List Vehicles (GET /api/vehicles) ---

    @Test
    @DisplayName("GET /api/vehicles returns 200 OK with array of vehicles")
    void getAllVehicles_whenVehiclesExist_returns200WithArray() throws Exception {
        List<VehicleResponse> responses = List.of(
                new VehicleResponse(1L, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE),
                new VehicleResponse(2L, "SG-1002", "Stark Logistics", "Van", "Ford", "Transit", VehicleStatus.MAINTENANCE)
        );

        when(vehicleService.getAllVehicles()).thenReturn(responses);

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vehicleNumber", is("SG-1001")))
                .andExpect(jsonPath("$[1].vehicleNumber", is("SG-1002")));
    }

    @Test
    @DisplayName("GET /api/vehicles when empty returns 200 OK with empty array []")
    void getAllVehicles_whenEmpty_returns200WithEmptyArray() throws Exception {
        when(vehicleService.getAllVehicles()).thenReturn(List.of());

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Get Vehicle by ID (GET /api/vehicles/{id}) ---

    @Test
    @DisplayName("GET /api/vehicles/{id} with existing ID returns 200 OK")
    void getVehicleById_withExistingId_returns200() throws Exception {
        VehicleResponse response = new VehicleResponse(
                1L, "SG-1001", "Stark Industries", "Sedan", "Tesla", "Model S", VehicleStatus.ACTIVE);

        when(vehicleService.getVehicleById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.vehicleNumber", is("SG-1001")));
    }

    @Test
    @DisplayName("GET /api/vehicles/{id} with non-existent ID returns 404 Not Found")
    void getVehicleById_withMissingId_returns404() throws Exception {
        when(vehicleService.getVehicleById(999L)).thenThrow(new VehicleNotFoundException(999L));

        mockMvc.perform(get("/api/vehicles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/vehicles/{id} with non-positive ID returns 400 Bad Request")
    void getVehicleById_withNonPositiveId_returns400() throws Exception {
        mockMvc.perform(get("/api/vehicles/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        mockMvc.perform(get("/api/vehicles/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("GET /api/vehicles/{id} with non-numeric ID returns 400 Bad Request")
    void getVehicleById_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/vehicles/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("INVALID_ARGUMENT_TYPE")));
    }

    // --- Update Vehicle (PUT /api/vehicles/{id}) ---

    @Test
    @DisplayName("PUT /api/vehicles/{id} with valid payload returns 200 OK")
    void updateVehicle_withValidPayload_returns200() throws Exception {
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "SG-1001", "Stark Industries", "SUV", "Tesla", "Model X", VehicleStatus.MAINTENANCE);

        VehicleResponse response = new VehicleResponse(
                1L, "SG-1001", "Stark Industries", "SUV", "Tesla", "Model X", VehicleStatus.MAINTENANCE);

        when(vehicleService.updateVehicle(eq(1L), any(UpdateVehicleRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.type", is("SUV")))
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
    }

    @Test
    @DisplayName("PUT /api/vehicles/{id} with duplicate Vehicle Number returns 409 Conflict")
    void updateVehicle_withDuplicateNumber_returns409() throws Exception {
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "SG-9999", "Stark Industries", "SUV", "Tesla", "Model X", VehicleStatus.MAINTENANCE);

        when(vehicleService.updateVehicle(eq(1L), any(UpdateVehicleRequest.class)))
                .thenThrow(new DuplicateVehicleNumberException("A vehicle with this Vehicle Number already exists."));

        mockMvc.perform(put("/api/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_VEHICLE_NUMBER")));
    }

    @Test
    @DisplayName("PUT /api/vehicles/{id} with non-existent ID returns 404 Not Found")
    void updateVehicle_withMissingVehicle_returns404() throws Exception {
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                "SG-1001", "Stark Industries", "SUV", "Tesla", "Model X", VehicleStatus.MAINTENANCE);

        when(vehicleService.updateVehicle(eq(999L), any(UpdateVehicleRequest.class)))
                .thenThrow(new VehicleNotFoundException(999L));

        mockMvc.perform(put("/api/vehicles/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")));
    }

    // --- Delete Vehicle (DELETE /api/vehicles/{id}) ---

    @Test
    @DisplayName("DELETE /api/vehicles/{id} with existing ID returns 204 No Content with empty body")
    void deleteVehicle_withExistingId_returns204NoContent() throws Exception {
        doNothing().when(vehicleService).deleteVehicle(1L);

        mockMvc.perform(delete("/api/vehicles/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(vehicleService).deleteVehicle(1L);
    }

    @Test
    @DisplayName("DELETE /api/vehicles/{id} with non-existent ID returns 404 Not Found")
    void deleteVehicle_withMissingId_returns404() throws Exception {
        doThrow(new VehicleNotFoundException(999L)).when(vehicleService).deleteVehicle(999L);

        mockMvc.perform(delete("/api/vehicles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")));
    }

    // --- Method Not Allowed & Unexpected Server Errors ---

    @Test
    @DisplayName("PATCH /api/vehicles/1 returns 405 Method Not Allowed with Allow header")
    void unsupportedMethod_returns405WithAllowHeader() throws Exception {
        mockMvc.perform(patch("/api/vehicles/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code", is("METHOD_NOT_ALLOWED")));
    }

    @Test
    @DisplayName("Unexpected server error returns safe 500 without leaking internal details")
    void unexpectedError_returnsSafe500Response() throws Exception {
        when(vehicleService.getAllVehicles()).thenThrow(new RuntimeException("Simulated unexpected internal database failure with secret_token=12345"));

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.code", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred. Please try again later.")))
                .andExpect(jsonPath("$.message", not(containsString("secret_token"))));
    }

    @Test
    @DisplayName("OPTIONS /api/vehicles preflight from allowed origin returns 200 with CORS headers")
    void corsPreflight_fromAllowedOrigin_returns200() throws Exception {
        mockMvc.perform(options("/api/vehicles")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }
}
