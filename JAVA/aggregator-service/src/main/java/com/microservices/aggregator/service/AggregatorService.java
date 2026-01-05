package com.microservices.aggregator.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AggregatorService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String aggregateData() {
        try {
            String customerHealth = restTemplate.getForObject("http://localhost:8001/api/customers/health", String.class);
            String policyHealth = restTemplate.getForObject("http://localhost:8002/api/policies/health", String.class);
            String paymentHealth = restTemplate.getForObject("http://localhost:8003/api/payments/health", String.class);

            return "Aggregated Services Status:\n" +
                    "Customer Service: " + customerHealth + "\n" +
                    "Policy Service: " + policyHealth + "\n" +
                    "Payment Service: " + paymentHealth;
        } catch (Exception e) {
            return "Error aggregating data: " + e.getMessage();
        }
    }

    public Map<String, Object> aggregate(String customerId, String policyId, String paymentId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'aggregate'");
    }
}
