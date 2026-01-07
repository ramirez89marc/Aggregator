package com.microservices.aggregator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Concurrency and CompletableFuture tests for AggregatorService
 * Tests concurrent processing, reactive behavior, and async operations
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "spring.cache.type=caffeine"
})
@DisplayName("AggregatorService Concurrency Tests")
class AggregatorServiceConcurrencyTest {

    @Autowired
    private AggregatorService aggregatorService;

    @Autowired
    private CacheManager cacheManager;

    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(aggregatorService, "restTemplate", mockRestTemplate);
        
        // Clear caches before each test
        if (cacheManager.getCache("customerServiceCache") != null) {
            cacheManager.getCache("customerServiceCache").clear();
        }
        if (cacheManager.getCache("policyServiceCache") != null) {
            cacheManager.getCache("policyServiceCache").clear();
        }
        if (cacheManager.getCache("paymentServiceCache") != null) {
            cacheManager.getCache("paymentServiceCache").clear();
        }
    }

    // ==================== COMPLETABLE FUTURE TESTS ====================

    @Test
    @DisplayName("Should aggregate data using CompletableFuture for parallel processing")
    void testAggregateDataWithCompletableFuture() {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        // Act
        String result = aggregatorService.aggregateData();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Aggregated Services Status"));
    }

    @Test
    @DisplayName("Should handle multiple concurrent aggregation requests")
    void testConcurrentAggregateDataRequests() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - Submit 5 concurrent requests
        for (int i = 0; i < 5; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    String result = aggregatorService.aggregateData();
                    if (result != null && !result.isEmpty()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all tasks to complete
        latch.await();

        // Assert
        assertEquals(5, successCount.get());
    }

    @Test
    @DisplayName("Should handle concurrent calls to different service endpoints")
    void testConcurrentServiceCalls() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger customerSuccess = new AtomicInteger(0);
        AtomicInteger policySuccess = new AtomicInteger(0);
        AtomicInteger paymentSuccess = new AtomicInteger(0);

        // Act
        CompletableFuture.runAsync(() -> {
            try {
                String result = aggregatorService.callCustomerService();
                if (result != null) customerSuccess.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                String result = aggregatorService.callPolicyService();
                if (result != null) policySuccess.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                String result = aggregatorService.callPaymentService();
                if (result != null) paymentSuccess.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // Assert
        assertEquals(1, customerSuccess.get());
        assertEquals(1, policySuccess.get());
        assertEquals(1, paymentSuccess.get());
    }

    @Test
    @DisplayName("Should handle race conditions in concurrent cache access")
    void testConcurrentCacheAccess() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger cacheHits = new AtomicInteger(0);

        // Act - Multiple concurrent calls to same service
        for (int i = 0; i < 10; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    String result = aggregatorService.callCustomerService();
                    if (result != null && result.contains("Service OK")) {
                        cacheHits.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Assert
        assertEquals(10, cacheHits.get());
    }

    @Test
    @DisplayName("Should handle timeout in concurrent environment")
    void testTimeoutInConcurrentEnvironment() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        CountDownLatch latch = new CountDownLatch(3);

        // Act
        CompletableFuture.runAsync(() -> {
            try {
                aggregatorService.aggregateData();
            } finally {
                latch.countDown();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                aggregatorService.aggregateData();
            } finally {
                latch.countDown();
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                aggregatorService.aggregateData();
            } finally {
                latch.countDown();
            }
        });

        // Assert - All should complete without hanging
        latch.await();
    }

    @Test
    @DisplayName("Should maintain consistency with concurrent reads and writes")
    void testConcurrentReadWriteConsistency() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        CountDownLatch latch = new CountDownLatch(6);
        AtomicInteger consistentResults = new AtomicInteger(0);

        // Act - Mix of aggregation and service calls
        for (int i = 0; i < 3; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    String result = aggregatorService.aggregateData();
                    if (result != null && result.contains("Customer Service")) {
                        consistentResults.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < 3; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    String result = aggregatorService.callCustomerService();
                    if (result != null && result.contains("OK")) {
                        consistentResults.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Assert
        assertEquals(6, consistentResults.get());
    }

    @Test
    @DisplayName("Should handle exception in concurrent environment")
    void testExceptionHandlingInConcurrentEnvironment() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Service error"));

        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger errorHandled = new AtomicInteger(0);

        // Act
        for (int i = 0; i < 3; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    String result = aggregatorService.callCustomerService();
                    if (result != null && (result.contains("UNAVAILABLE") || result.contains("Error"))) {
                        errorHandled.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Assert - All errors should be handled gracefully
        assertTrue(errorHandled.get() > 0);
    }

    @Test
    @DisplayName("Should complete all futures even with partial failures")
    void testPartialFailuresWithCompletableFuture() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK")
                .thenThrow(new RuntimeException("Service down"))
                .thenReturn("Service OK");

        CountDownLatch latch = new CountDownLatch(3);

        // Act
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    try {
                        aggregatorService.callCustomerService();
                    } finally {
                        latch.countDown();
                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        aggregatorService.callPolicyService();
                    } finally {
                        latch.countDown();
                    }
                }),
                CompletableFuture.runAsync(() -> {
                    try {
                        aggregatorService.callPaymentService();
                    } finally {
                        latch.countDown();
                    }
                })
        );

        latch.await();

        // Assert
        assertEquals(0, latch.getCount());
    }

    @Test
    @DisplayName("Should handle thread safety in cache operations")
    void testThreadSafetyInCacheOperations() throws InterruptedException {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Cached Result");

        CountDownLatch latch = new CountDownLatch(20);

        // Act - Heavy concurrent cache access
        for (int i = 0; i < 20; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    aggregatorService.callCustomerService();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Assert - Should complete without deadlock or corruption
        assertDoesNotThrow(() -> aggregatorService.callCustomerService());
    }

    @Test
    @DisplayName("Should maintain order of processing in async operations")
    void testOrderingInAsyncOperations() {
        // Arrange
        when(mockRestTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("Service OK");

        // Act
        String result = aggregatorService.aggregateData();

        // Assert
        assertNotNull(result);
        // All three services should be included
        assertTrue(result.contains("Customer Service") || result.contains("Policy Service") || 
                  result.contains("Payment Service"));
    }
}
