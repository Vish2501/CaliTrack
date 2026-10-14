package com.calitrack.service;

/** The language model behind the AI coach: Gemini, or a canned stand-in for local demos. */
public interface CoachModelClient {

    boolean isConfigured();

    /** Returns the model's reply to the prompt. */
    String complete(String prompt);
}
