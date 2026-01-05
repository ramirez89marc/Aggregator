package com.microservices.policy.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    
    private static final Map<String, Map<String, String>> policies = new HashMap<>();
    
    static {
        Map<String, String> pol1 = new HashMap<>();
        pol1.put("type", "Auto Insurance");
        pol1.put("premium", "1200");
        pol1.put("status", "Active");
        policies.put("P1", pol1);
        
        Map<String, String> pol2 = new HashMap<>();
        pol2.put("type", "Home Insurance");
        pol2.put("premium", "2500");
        pol2.put("status", "Active");
        policies.put("P2", pol2);
    }
    
    @GetMapping("/{id}")
    public Map<String, String> getPolicy(@PathVariable String id) {
        return policies.getOrDefault(id, Map.of(
            "type", "Unknown",
            "premium", "0",
            "status", "Inactive"
        ));
    }
    
    @GetMapping("/health")
    public String health() {
        return "Policy Service UP";
    }
}