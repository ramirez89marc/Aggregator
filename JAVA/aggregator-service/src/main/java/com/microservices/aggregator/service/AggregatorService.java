package com.microservices.aggregator.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AggregatorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final long TIMEOUT_SECONDS = 5;

    /**
     * Aggregates data from all microservices with circuit breaker and fallback support
     */
    public String aggregateData() {
        try {
            // Create async tasks for each microservice call
            CompletableFuture<String> customerFuture = CompletableFuture.supplyAsync(
                    this::callCustomerService
            );

            CompletableFuture<String> policyFuture = CompletableFuture.supplyAsync(
                    this::callPolicyService
            );

            CompletableFuture<String> paymentFuture = CompletableFuture.supplyAsync(
                    this::callPaymentService
            );

            // Wait for all futures to complete with timeout
            CompletableFuture.allOf(customerFuture, policyFuture, paymentFuture)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String customerHealth = customerFuture.join();
            String policyHealth = policyFuture.join();
            String paymentHealth = paymentFuture.join();

            return "Aggregated Services Status (Parallel Calls with Circuit Breaker):\n" +
                    "Customer Service: " + customerHealth + "\n" +
                    "Policy Service: " + policyHealth + "\n" +
                    "Payment Service: " + paymentHealth;
        } catch (TimeoutException e) {
            return "Error: Request timeout - one or more services took too long to respond (" + TIMEOUT_SECONDS + "s)";
        } catch (Exception e) {
            return "Error aggregating data: " + e.getMessage();
        }
    }

    /**
     * Call customer service with circuit breaker, retry, and caching
     */
    @CircuitBreaker(name = "customerService", fallbackMethod = "customerServiceFallback")
    @Retry(name = "customerService")
    @Cacheable(value = "customerServiceCache", unless = "#result.contains('Error')")
    public String callCustomerService() {
        return callService("Customer", "http://localhost:8001/api/customers/health");
    }

    /**
     * Fallback method for customer service
     */
    public String customerServiceFallback(Exception e) {
        return "Customer Service: UNAVAILABLE (Fallback) - " + e.getClass().getSimpleName();
    }

    /**
     * Call policy service with circuit breaker, retry, and caching
     */
    @CircuitBreaker(name = "policyService", fallbackMethod = "policyServiceFallback")
    @Retry(name = "policyService")
    @Cacheable(value = "policyServiceCache", unless = "#result.contains('Error')")
    public String callPolicyService() {
        return callService("Policy", "http://localhost:8002/api/policies/health");
    }

    /**
     * Fallback method for policy service
     */
    public String policyServiceFallback(Exception e) {
        return "Policy Service: UNAVAILABLE (Fallback) - " + e.getClass().getSimpleName();
    }

    /**
     * Call payment service with circuit breaker, retry, and caching
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentServiceFallback")
    @Retry(name = "paymentService")
    @Cacheable(value = "paymentServiceCache", unless = "#result.contains('Error')")
    public String callPaymentService() {
        return callService("Payment", "http://localhost:8003/api/payments/health");
    }

    /**
     * Fallback method for payment service
     */
    public String paymentServiceFallback(Exception e) {
        return "Payment Service: UNAVAILABLE (Fallback) - " + e.getClass().getSimpleName();
    }

    /**
     * Generic method to call any service
     */
    private String callService(String serviceName, String url) {
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            return serviceName + " Service: Error - " + e.getMessage();
        }
    }
}