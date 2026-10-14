package com.calitrack.service;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Picks the AI coach's model from calitrack.coach.fake-model: "true" always uses the canned demo
 * model, "auto" uses it only when no Gemini API key is set, and anything else uses Gemini.
 */
public final class CoachModelSelection {

    private CoachModelSelection() {
    }

    static boolean useFakeModel(Environment environment) {
        String mode = environment.getProperty("calitrack.coach.fake-model", "false").trim();
        if ("auto".equalsIgnoreCase(mode)) {
            return !StringUtils.hasText(environment.getProperty("gemini.api-key"));
        }
        return Boolean.parseBoolean(mode);
    }

    static class FakeModel implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return useFakeModel(context.getEnvironment());
        }
    }

    static class GeminiModel implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !useFakeModel(context.getEnvironment());
        }
    }
}
