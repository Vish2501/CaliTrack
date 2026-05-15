package com.calitrack.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.calitrack.dto.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatusReturnsConsistentErrorBody() {
        when(request.getRequestURI()).thenReturn("/api/v1/workouts/1");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Workout not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/workouts/1");
    }
}
