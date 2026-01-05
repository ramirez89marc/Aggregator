package com.microservices.customer.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private static final Map<String, Map<String, String>> customers = new HashMap<>();
    
    static {
        Map<String, String> cust1 = new HashMap<>();
        cust1.put("name", "Raj Kumar");
        cust1.put("email", "raj@email.com");
        cust1.put("phone", "1234567890");
        customers.put("C1", cust1);
        
        Map<String, String> cust2 = new HashMap<>();
        cust2.put("name", "Marc Ramirez");
        cust2.put("email", "marc@email.com");
        cust2.put("phone", "9876543210");
        customers.put("C2", cust2);
    }
    
    @GetMapping("/{id}")
    public Map<String, String> getCustomer(@PathVariable String id) {
        return customers.getOrDefault(id, Map.of(
            "name", "Unknown",
            "email", "unknown@email.com",
            "phone", "0000000000"
        ));
    }
    
    @GetMapping("/health")
    public String health() {
        return "Customer Service UP";
    }
}