package com.calitrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AICoachConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
