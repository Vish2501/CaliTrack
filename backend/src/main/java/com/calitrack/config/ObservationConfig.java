package com.calitrack.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class ObservationConfig {

    // The Docker and Render health checks call /health every few seconds; tracing them would bury
    // real requests in Jaeger.
    @Bean
    ObservationPredicate skipHealthCheckObservations() {
        return (name, context) -> !(context instanceof ServerRequestObservationContext request
            && "/health".equals(request.getCarrier().getRequestURI()));
    }

    // Spring Security records a span for each stage of its filter chain. They add nothing here, and
    // for skipped requests they would show up in Jaeger as stray traces on their own.
    @Bean
    ObservationPredicate skipSpringSecurityObservations() {
        return (name, context) -> !name.startsWith("spring.security.");
    }
}
