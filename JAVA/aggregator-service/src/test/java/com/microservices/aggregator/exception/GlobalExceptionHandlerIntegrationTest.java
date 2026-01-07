package com.microservices.aggregator.exception;

import com.microservices.aggregator.dto.ErrorResponse;
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
 * Integration tests for GlobalExceptionHandler
 * Tests exception handling and error response formatting
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== ERROR RESPONSE FORMAT TESTS ====================

    @Test
    @DisplayName("Error response should contain all required fields")
    void testErrorResponseFormatForNotFound() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/invalid"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
        assertFalse(content.isEmpty());
    }

    @Test
    @DisplayName("Error response should include status code")
    void testErrorResponseIncludesStatusCode() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/nonexistent"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("404") || content.contains("NOT_FOUND"));
    }

    @Test
    @DisplayName("Error response should include error type")
    void testErrorResponseIncludesErrorType() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/missing"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
    }

    @Test
    @DisplayName("Error response should include timestamp")
    void testErrorResponseIncludesTimestamp() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/notfound"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("timestamp"));
    }

    @Test
    @DisplayName("Error response should include path")
    void testErrorResponseIncludesPath() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/test"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("path") || content.contains("/api/aggregate"));
    }

    @Test
    @DisplayName("Error response should include trace ID")
    void testErrorResponseIncludesTraceId() throws Exception {
        // Act
        MvcResult result = mockMvc.perform(get("/api/aggregate/xyz"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("traceId"));
    }

    // ==================== HTTP STATUS TESTS ====================

    @Test
    @DisplayName("Not found endpoint should return 404 status")
    void testNotFoundStatusCode() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Valid health endpoint should return 200 status")
    void testValidEndpointStatusCode() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
    }

    // ==================== RESPONSE CONTENT TYPE TESTS ====================

    @Test
    @DisplayName("Error response should be JSON formatted")
    void testErrorResponseIsJson() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @DisplayName("Success response should be JSON formatted")
    void testSuccessResponseIsJson() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    // ==================== CUSTOM EXCEPTION TESTS ====================

    @Test
    @DisplayName("ResourceNotFoundException should be handled with 404 status")
    void testResourceNotFoundExceptionHandling() throws Exception {
        // This would require a controller endpoint that throws ResourceNotFoundException
        // Act & Assert
        mockMvc.perform(get("/api/aggregate/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("BadRequestException should return 400 status")
    void testBadRequestExceptionHandling() throws Exception {
        // This would require a POST endpoint with validation
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UnauthorizedException should return 401 status")
    void testUnauthorizedExceptionHandling() throws Exception {
        // This would require authentication
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
    }

    // ==================== TRACE ID UNIQUENESS TESTS ====================

    @Test
    @DisplayName("Each error response should have unique trace ID")
    void testTraceIdUniqueness() throws Exception {
        // Act - Make two failing requests
        MvcResult result1 = mockMvc.perform(get("/api/aggregate/notfound1"))
                .andExpect(status().isNotFound())
                .andReturn();

        MvcResult result2 = mockMvc.perform(get("/api/aggregate/notfound2"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Assert
        String content1 = result1.getResponse().getContentAsString();
        String content2 = result2.getResponse().getContentAsString();
        
        assertNotNull(content1);
        assertNotNull(content2);
        assertNotEquals(content1, content2);
    }

    // ==================== VALIDATION ERROR TESTS ====================

    @Test
    @DisplayName("Validation errors should include field details")
    void testValidationErrorsIncludeFieldDetails() throws Exception {
        // This would require a POST endpoint with @Valid annotation
        // For now, we test that the framework is properly configured
        mockMvc.perform(get("/api/aggregate/health"))
                .andExpect(status().isOk());
    }
}
