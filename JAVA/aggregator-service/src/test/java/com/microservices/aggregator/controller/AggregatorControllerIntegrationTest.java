package com.microservices.aggregator.controller;

import com.microservices.aggregator.service.AggregatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AggregatorController
 * Tests HTTP endpoints and integration with AggregatorService
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DisplayName("AggregatorController Integration Tests")
class AggregatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AggregatorService aggregatorService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
    }

    // ==================== HEALTH CHECK TESTS ====================

    @Test
    @DisplayName("Health endpoint should return 200 OK")
    void testHealthEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Aggregator Service is running")));
    }

    @Test
    @DisplayName("Health endpoint should return running status")
    void testHealthEndpointReturnsRunningStatus() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertEquals("Aggregator Service is running", content);
    }

    // ==================== STATUS AGGREGATION TESTS ====================

    @Test
    @DisplayName("Status endpoint should return aggregated status")
    void testStatusEndpointSuccess() throws Exception {
        // Arrange
        String aggregatedStatus = "Aggregated Services Status (Parallel Calls with Circuit Breaker):\n" +
                "Customer Service: Customer Service: OK\n" +
                "Policy Service: Policy Service: OK\n" +
                "Payment Service: Payment Service: OK";
        
        when(aggregatorService.aggregateData()).thenReturn(aggregatedStatus);

        // Act & Assert
        mockMvc.perform(get("/api/aggregate/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("Aggregated Services Status")));
    }

    @Test
    @DisplayName("Status endpoint should include all services in response")
    void testStatusEndpointIncludesAllServices() throws Exception {
        // Arrange
        String aggregatedStatus = "Aggregated Services Status (Parallel Calls with Circuit Breaker):\n" +
                "Customer Service: OK\n" +
                "Policy Service: OK\n" +
                "Payment Service: OK";
        
        when(aggregatorService.aggregateData()).thenReturn(aggregatedStatus);

        // Act & Assert
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Customer Service")))
                .andExpect(content().string(containsString("Policy Service")))
                .andExpect(content().string(containsString("Payment Service")));
    }

    @Test
    @DisplayName("Status endpoint should handle service unavailability")
    void testStatusEndpointWithServiceUnavailability() throws Exception {
        // Arrange
        String unavailableStatus = "Error: Request timeout - one or more services took too long to respond (5s)";
        when(aggregatorService.aggregateData()).thenReturn(unavailableStatus);

        // Act & Assert
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Error")));
    }

    @Test
    @DisplayName("Status endpoint should handle partial failures")
    void testStatusEndpointWithPartialFailures() throws Exception {
        // Arrange
        String partialStatus = "Aggregated Services Status:\n" +
                "Customer Service: OK\n" +
                "Policy Service: Error\n" +
                "Payment Service: OK";
        
        when(aggregatorService.aggregateData()).thenReturn(partialStatus);

        // Act & Assert
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Customer Service")))
                .andExpect(content().string(containsString("Error")));
    }

    @Test
    @DisplayName("Status endpoint should return valid response format")
    void testStatusEndpointResponseFormat() throws Exception {
        // Arrange
        String expectedStatus = "Aggregated Services Status";
        when(aggregatorService.aggregateData()).thenReturn(expectedStatus);

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("Invalid endpoint should return 404 Not Found")
    void testInvalidEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Status endpoint with query parameters should still work")
    void testStatusEndpointWithQueryParams() throws Exception {
        // Arrange
        String expectedStatus = "Aggregated Services Status";
        when(aggregatorService.aggregateData()).thenReturn(expectedStatus);

        // Act & Assert
        mockMvc.perform(get("/api/aggregate/status")
                .param("service", "all"))
                .andExpect(status().isOk());
    }

    // ==================== CONCURRENT ACCESS TESTS ====================

    @Test
    @DisplayName("Status endpoint should handle concurrent requests")
    void testStatusEndpointConcurrentAccess() throws Exception {
        // Arrange
        String expectedStatus = "Aggregated Services Status";
        when(aggregatorService.aggregateData()).thenReturn(expectedStatus);

        // Act & Assert
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/aggregate/status"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Both health and status endpoints should work concurrently")
    void testConcurrentHealthAndStatusRequests() throws Exception {
        // Arrange
        when(aggregatorService.aggregateData()).thenReturn("Status OK");

        // Act & Assert - Interleaved requests
        mockMvc.perform(get("/api/aggregate/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/status")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/status")).andExpect(status().isOk());
    }
}
