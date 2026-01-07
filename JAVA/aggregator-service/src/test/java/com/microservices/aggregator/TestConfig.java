package com.microservices.aggregator;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for aggregator service tests
 * Provides mock beans and test configurations
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public RestTemplate testRestTemplate() {
        return mock(RestTemplate.class);
    }
}
