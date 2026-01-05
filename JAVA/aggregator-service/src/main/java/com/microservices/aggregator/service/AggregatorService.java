package com.microservices.aggregator.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AggregatorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final long TIMEOUT_SECONDS = 5;

    public String aggregateData() {
        try {
            // Create async tasks for each microservice call
            CompletableFuture<String> customerFuture = CompletableFuture.supplyAsync(() ->
                    callService("Customer", "http://localhost:8001/api/customers/health")
            );

            CompletableFuture<String> policyFuture = CompletableFuture.supplyAsync(() ->
                    callService("Policy", "http://localhost:8002/api/policies/health")
            );

            CompletableFuture<String> paymentFuture = CompletableFuture.supplyAsync(() ->
                    callService("Payment", "http://localhost:8003/api/payments/health")
            );

            // Wait for all futures to complete with timeout
            CompletableFuture.allOf(customerFuture, policyFuture, paymentFuture)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String customerHealth = customerFuture.join();
            String policyHealth = policyFuture.join();
            String paymentHealth = paymentFuture.join();

            return "Aggregated Services Status (Parallel Calls):\n" +
                    "Customer Service: " + customerHealth + "\n" +
                    "Policy Service: " + policyHealth + "\n" +
                    "Payment Service: " + paymentHealth;
        } catch (TimeoutException e) {
            return "Error: Request timeout - one or more services took too long to respond (" + TIMEOUT_SECONDS + "s)";
        } catch (Exception e) {
            return "Error aggregating data: " + e.getMessage();
        }
    }

    private String callService(String serviceName, String url) {
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            return serviceName + " Service: Error - " + e.getMessage();
        }
    }
}
