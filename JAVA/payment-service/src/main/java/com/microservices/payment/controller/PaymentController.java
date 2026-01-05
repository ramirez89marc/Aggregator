package com.microservices.payment.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Map<String, Map<String, String>> payments = new HashMap<>();
    
    static {
        Map<String, String> pay1 = new HashMap<>();
        pay1.put("amount", "1200");
        pay1.put("method", "Credit Card");
        pay1.put("status", "Completed");
        payments.put("PY1", pay1);
        
        Map<String, String> pay2 = new HashMap<>();
        pay2.put("amount", "2500");
        pay2.put("method", "Bank Transfer");
        pay2.put("status", "Completed");
        payments.put("PY2", pay2);
    }
    
    @GetMapping("/{id}")
    public Map<String, String> getPayment(@PathVariable String id) {
        return payments.getOrDefault(id, Map.of(
            "amount", "0",
            "method", "Unknown",
            "status", "Pending"
        ));
    }
    
    @GetMapping("/health")
    public String health() {
        return "Payment Service UP";
    }
}