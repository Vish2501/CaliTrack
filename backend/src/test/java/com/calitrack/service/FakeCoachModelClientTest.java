package com.calitrack.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FakeCoachModelClientTest {

    private final FakeCoachModelClient client = new FakeCoachModelClient(0);

    @Test
    void buildsAReplyFromTheMostRecentExerciseAndGoals() {
        String prompt = """
            User Profile:
            - Fitness Level: beginner
            - Goals: build strength

            Recent Workouts:
            - Date: 2026-10-14
              * Pull-Up (Back): 3 sets, avg 8.0 reps, 0.0 lbs, RPE: 8.0
              * Dip (Triceps): 2 sets, avg 10.0 reps, 0.0 lbs, RPE: 7.0
            """;

        String reply = client.complete(prompt);

        assertThat(reply)
            .contains("RECOMMENDATION: [Demo reply, no AI call]")
            .contains("build strength goal")
            .contains("FOCUS_AREA: Back")
            .contains("NEXT_EXERCISE: Pull-Up");
    }

    @Test
    void fallsBackWhenThereAreNoWorkoutsOrCategory() {
        assertThat(client.complete("- Goals: general fitness\n"))
            .contains("FOCUS_AREA: Full body")
            .contains("NEXT_EXERCISE: Push-Up");

        assertThat(client.complete("  * Plank (null): 1 sets, avg 1.0 reps"))
            .contains("FOCUS_AREA: Full body")
            .contains("NEXT_EXERCISE: Plank");
    }
}
