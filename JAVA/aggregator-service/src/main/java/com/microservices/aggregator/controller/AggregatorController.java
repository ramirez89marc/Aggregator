package com.microservices.aggregator.controller;

import com.microservices.aggregator.service.AggregatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/aggregate")
public class AggregatorController {

    @Autowired
    private AggregatorService aggregatorService;

    @GetMapping("/status")
    public String getAggregatedStatus() {
        return aggregatorService.aggregateData();
    }

    @GetMapping("/health")
    public String health() {
        return "Aggregator Service is running";
    }
}
