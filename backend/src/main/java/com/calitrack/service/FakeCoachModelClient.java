package com.calitrack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stand-in for OpenAI when calitrack.coach.fake-model is true, so the coach can be demoed locally
 * without an API key or quota. Replies in the same format the real prompt asks for, built from the
 * most recent exercise in the prompt, after a short delay so the app's "generating" state shows.
 * Every reply is labelled as a demo so it can't be mistaken for real model output.
 */
@Component
@ConditionalOnProperty(name = "calitrack.coach.fake-model", havingValue = "true")
public class FakeCoachModelClient implements CoachModelClient {

    private static final Logger log = LoggerFactory.getLogger(FakeCoachModelClient.class);

    // Matches AICoachService.buildPrompt's exercise lines: "  * Pull-Up (Back): 3 sets, ..."
    private static final Pattern EXERCISE_LINE = Pattern.compile("^\\s*\\* (.+?) \\((.*?)\\): ", Pattern.MULTILINE);
    private static final Pattern GOALS_LINE = Pattern.compile("^- Goals: (.+)$", Pattern.MULTILINE);

    private final long delayMs;

    public FakeCoachModelClient(@Value("${calitrack.coach.fake-model-delay-ms}") long delayMs) {
        this.delayMs = delayMs;
        log.warn("AI coach is using the fake model: replies are canned demo text, not OpenAI");
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String complete(String prompt) {
        pause();

        Matcher exercise = EXERCISE_LINE.matcher(prompt);
        String name = "Push-Up";
        String area = "Full body";
        if (exercise.find()) {
            name = exercise.group(1);
            if (!exercise.group(2).isBlank() && !"null".equals(exercise.group(2))) {
                area = exercise.group(2);
            }
        }

        Matcher goalsLine = GOALS_LINE.matcher(prompt);
        String goals = goalsLine.find() ? goalsLine.group(1).trim() : "general fitness";

        return "RECOMMENDATION: [Demo reply, no OpenAI call] For your " + goals + " goal, add one more set of "
            + name + " next session and keep the effort around RPE 8.\n"
            + "FOCUS_AREA: " + area + "\n"
            + "NEXT_EXERCISE: " + name;
    }

    private void pause() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
