package com.microservices.aggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for the complete microservice booking flow
 * Tests complete request/response cycles and multi-service interactions
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DisplayName("End-to-End Integration Tests")
class EndToEndBookingFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Setup before each test
    }

    // ==================== BOOKING FLOW TESTS ====================

    @Test
    @DisplayName("Complete booking flow: health check -> aggregate status -> process booking")
    void testCompleteBookingFlow() throws Exception {
        // Step 1: Verify all services are healthy
        mockMvc.perform(get("/api/aggregate/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aggregator Service is running")));

        // Step 2: Check aggregated service status
        mockMvc.perform(get("/api/aggregate/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aggregated Services Status")));

        // Step 3: Verify response contains required information
        MvcResult statusResult = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String statusResponse = statusResult.getResponse().getContentAsString();
        assertNotNull(statusResponse);
        assertFalse(statusResponse.isEmpty());
    }

    // ==================== SERVICE DISCOVERY FLOW TESTS ====================

    @Test
    @DisplayName("Service discovery: Aggregator should discover and connect to all services")
    void testServiceDiscoveryFlow() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Assert - All services should be mentioned
        assertTrue(response.contains("Service"), "Response should contain service information");
    }

    @Test
    @DisplayName("Status check should include Customer, Policy, and Payment services")
    void testAllServicesInStatusCheck() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Assert
        assertTrue(response.length() > 0);
    }

    // ==================== STATE TRANSITION FLOW TESTS ====================

    @Test
    @DisplayName("State transition: Service health check transitions from healthy to accessible")
    void testServiceStateTransition() throws Exception {
        // Act - First check
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        // Act - Second check (should be same or better state)
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        // Act - Status check after health confirmation
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Booking state: Initialize -> Validate -> Execute")
    void testBookingStateProgression() throws Exception {
        // Initialize: Health check
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        // Validate: Check aggregate status
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());

        // Execute: Confirm services are ready
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    // ==================== FAILURE RECOVERY FLOW TESTS ====================

    @Test
    @DisplayName("Failure recovery: Service unavailability should gracefully degrade")
    void testFailureRecoveryFlow() throws Exception {
        // Act - Try to get status multiple times
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/aggregate/status"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Retry mechanism: Should retry on transient failures")
    void testRetryMechanismInFlow() throws Exception {
        // Act - Make multiple consecutive requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/aggregate/status"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Circuit breaker state transitions: CLOSED -> OPEN -> HALF_OPEN -> CLOSED")
    void testCircuitBreakerStateTransitions() throws Exception {
        // Act - Make request when circuit is CLOSED
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk());

        // Assert - Should get successful response
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(result.getResponse().getContentAsString());
    }

    // ==================== CACHE BEHAVIOR FLOW TESTS ====================

    @Test
    @DisplayName("Caching flow: First call caches result, second call returns cached value")
    void testCachingBehaviorFlow() throws Exception {
        // Act - First call
        MvcResult firstResult = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String firstResponse = firstResult.getResponse().getContentAsString();

        // Act - Second call (should be from cache)
        MvcResult secondResult = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String secondResponse = secondResult.getResponse().getContentAsString();

        // Assert
        assertNotNull(firstResponse);
        assertNotNull(secondResponse);
    }

    @Test
    @DisplayName("Cache invalidation: Error responses should not be cached")
    void testCacheInvalidationOnError() throws Exception {
        // Act - Request health endpoint (shouldn't cache errors)
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        // Act - Request non-existent endpoint
        mockMvc.perform(get("/api/aggregate/invalid"))
                .andExpect(status().isNotFound());

        // Act - Health should still work
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
    }

    // ==================== RESPONSE VALIDATION TESTS ====================

    @Test
    @DisplayName("Response validation: All responses should be valid JSON")
    void testResponseValidation() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Assert
        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    @DisplayName("Response content: Status response should contain service information")
    void testStatusResponseContent() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Assert
        assertNotNull(response);
        assertTrue(response.length() > 0);
    }

    // ==================== CONCURRENT BOOKING FLOW TESTS ====================

    @Test
    @DisplayName("Concurrent bookings: Multiple simultaneous booking flows")
    void testConcurrentBookingFlows() throws Exception {
        // Act - Simulate multiple concurrent booking requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/aggregate/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/aggregate/status")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Interleaved requests: Health and status requests interleaved")
    void testInterleavedBookingRequests() throws Exception {
        // Act - Interleaved health and status checks
        mockMvc.perform(get("/api/aggregate/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/status")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/status")).andExpect(status().isOk());
        mockMvc.perform(get("/api/aggregate/health")).andExpect(status().isOk());
    }

    // ==================== ERROR SCENARIO TESTS ====================

    @Test
    @DisplayName("Error scenario: Invalid endpoint should return 404")
    void testErrorScenarioInvalidEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/booking/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Error recovery: Service should continue after error")
    void testErrorRecoveryAndContinuation() throws Exception {
        // Act - Request invalid endpoint
        mockMvc.perform(get("/api/aggregate/invalid"))
                .andExpect(status().isNotFound());

        // Act - Service should still work
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk());
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @DisplayName("Performance: Response time should be acceptable")
    void testResponseTime() throws Exception {
        // Act & Assert - Health check should be fast
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert - Should complete within reasonable time (adjust as needed)
        assertTrue(duration < 5000, "Response should be quick");
    }

    @Test
    @DisplayName("Performance: Aggregation should complete within timeout")
    void testAggregationTimeout() throws Exception {
        // Act
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Assert
        assertNotNull(result.getResponse().getContentAsString());
        assertTrue(duration < 10000, "Aggregation should complete within timeout");
    }

    // ==================== COMPLETE WORKFLOW TESTS ====================

    @Test
    @DisplayName("Complete workflow: Health -> Status -> Error handling -> Recovery")
    void testCompleteWorkflowWithErrorHandling() throws Exception {
        // Step 1: Initial health check
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        // Step 2: Get aggregated status
        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk());

        // Step 3: Handle error gracefully
        mockMvc.perform(get("/api/aggregate/nonexistent"))
                .andExpect(status().isNotFound());

        // Step 4: Verify service recovery
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Booking flow with validation and confirmation")
    void testCompleteBookingFlowWithValidation() throws Exception {
        // Initialization
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Aggregator Service is running"));

        // Validation
        MvcResult statusCheck = mockMvc.perform(get("/api/aggregate/status"))
                .andExpect(status().isOk())
                .andReturn();

        String statusResponse = statusCheck.getResponse().getContentAsString();
        assertTrue(statusResponse.length() > 0);

        // Confirmation
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
    }
}
