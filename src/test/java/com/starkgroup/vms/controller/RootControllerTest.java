package com.starkgroup.vms.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RootController.class)
@AutoConfigureMockMvc(addFilters = false)
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / returns 200 OK and service metadata")
    void root_returns200AndMetadata() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("Stark Group Vehicle Management System API")))
                .andExpect(jsonPath("$.endpoints.health", is("/actuator/health")))
                .andExpect(jsonPath("$.endpoints.register", is("POST /api/auth/register")));
    }
}

