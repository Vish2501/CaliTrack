package com.calitrack.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class CoachModelSelectionTest {

    @Test
    void geminiIsTheDefault() {
        assertThat(CoachModelSelection.useFakeModel(new MockEnvironment())).isFalse();
    }

    @Test
    void trueAndFalseAreExplicit() {
        assertThat(CoachModelSelection.useFakeModel(env("true", "a-key"))).isTrue();
        assertThat(CoachModelSelection.useFakeModel(env("false", ""))).isFalse();
    }

    @Test
    void autoUsesGeminiOnlyWhenAKeyIsSet() {
        assertThat(CoachModelSelection.useFakeModel(env("auto", "a-key"))).isFalse();
        assertThat(CoachModelSelection.useFakeModel(env("auto", ""))).isTrue();
        assertThat(CoachModelSelection.useFakeModel(env("auto", "  "))).isTrue();
        assertThat(CoachModelSelection.useFakeModel(new MockEnvironment()
            .withProperty("calitrack.coach.fake-model", "auto"))).isTrue();
    }

    private static MockEnvironment env(String fakeModel, String geminiKey) {
        return new MockEnvironment()
            .withProperty("calitrack.coach.fake-model", fakeModel)
            .withProperty("gemini.api-key", geminiKey);
    }
}
