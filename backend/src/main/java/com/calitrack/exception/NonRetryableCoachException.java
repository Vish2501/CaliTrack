package com.calitrack.exception;

/**
 * A coach recommendation failure that retrying won't fix, such as a missing or invalid Gemini key
 * or the free-tier rate limit or daily quota being reached. The Kafka error handler sends these
 * straight to the dead-letter topic.
 */
public class NonRetryableCoachException extends RuntimeException {

    public NonRetryableCoachException(String message, Throwable cause) {
        super(message, cause);
    }
}
