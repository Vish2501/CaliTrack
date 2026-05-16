import { ExerciseEntry } from "../types/workout";
import { supabase } from "./supabase";
import { trackApiTiming, trackProductEvent } from "./telemetry";

const BASE_URL = "https://calitrack-api.onrender.com";
const API_V1 = `${BASE_URL}/api/v1`;

const DEFAULT_EXERCISES = [
  { name: "Push-Up", category: "Chest" },
  { name: "Pull-Up", category: "Back" },
  { name: "Chin-Up", category: "Biceps" },
  { name: "Dip", category: "Triceps" },
  { name: "Inverted Row", category: "Back" },
  { name: "Pike Push-Up", category: "Shoulders" },
  { name: "Bodyweight Squat", category: "Quads" },
  { name: "Hanging Knee Raise", category: "Core" },
  { name: "Hanging Leg Raise", category: "Core" },
  { name: "Plank", category: "Core" },
  { name: "Cable Lateral Raise", category: "Shoulders" },
  { name: "Overhead Tricep Extension", category: "Triceps" },
  { name: "Bayesian Cable Curl", category: "Biceps" },
  { name: "Face Pull", category: "Upper Back" },
  { name: "Squat", category: "Quads" },
  { name: "Romanian Deadlift", category: "Hamstrings" },
  { name: "Bulgarian Split Squat", category: "Quads" },
  { name: "Standing Calf Raise", category: "Calves" },
] as const;

export type WorkoutResponse = {
  id: number;
  userId: string;
  startTime: string;
  endTime: string | null;
  notes: string | null;
};

export type SetResponse = {
  id: number;
  workoutId: number;
  exerciseId: number;
  exerciseName: string;
  reps: number | null;
  weight: number | null;
  rpe: number | null;
  timestamp: string;
};

export type WorkoutDetailsResponse = WorkoutResponse & {
  sets: SetResponse[];
};

export type WorkoutFrequencyResponse = {
  weekStart: string;
  workoutCount: number;
};

export type ExerciseResponse = {
  id: number;
  name: string;
  category: string | null;
  userId: string;
};

export type CoachRecommendationResponse = {
  recommendation: string;
  focusArea: string;
  suggestedNextExercise: string;
};

async function authHeaders(includeJson = false) {
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  if (!token) throw new Error("No session");

  return {
    Authorization: `Bearer ${token}`,
    ...(includeJson ? { "Content-Type": "application/json" } : {}),
  };
}

async function parseApiError(res: Response): Promise<string> {
  const text = await res.text();
  if (!text) {
    return `Request failed (${res.status})`;
  }
  try {
    const body = JSON.parse(text) as { message?: string; error?: string };
    return body.message ?? body.error ?? text;
  } catch {
    return text;
  }
}

async function fetchWithTiming(
  metricKey: string,
  input: string,
  init?: RequestInit,
) {
  const start = globalThis.performance?.now?.() ?? Date.now();
  let status: number | undefined;

  try {
    const res = await fetch(input, init);
    status = res.status;
    return res;
  } finally {
    const end = globalThis.performance?.now?.() ?? Date.now();
    trackApiTiming(metricKey, end - start, status);
  }
}

export async function commitWorkout(
  workoutId: number,
  entries: ExerciseEntry[],
) {
  // Only persist sets the user explicitly completed with valid numeric inputs.
  const completedSets = entries.flatMap((exercise) =>
    exercise.sets
      .filter(
        (set) =>
          set.completed &&
          set.reps.trim().length > 0 &&
          set.weight.trim().length > 0,
      )
      .map((set) => ({
        exerciseId: Number(exercise.id),
        reps: Number(set.reps),
        weight: Number(set.weight),
      })),
  );

  // Send sets sequentially so server-side ordering stays predictable.
  let successfulSetLogs = 0;
  for (const set of completedSets) {
    await addWorkoutSet(workoutId, set);
    successfulSetLogs += 1;
  }
  if (successfulSetLogs > 0) {
    trackProductEvent("set_logged", successfulSetLogs);
  }

  await finishWorkout(workoutId);
  trackProductEvent("workout_completed");

  return completedSets.length;
}

export async function getWorkouts() {
  const res = await fetchWithTiming("get_workouts", `${API_V1}/workouts`, {
    headers: await authHeaders(),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutResponse[];
}

export async function getWorkoutFrequency(start: string, end: string) {
  const res = await fetchWithTiming(
    "get_workout_frequency",
    `${API_V1}/analytics/workout-frequency?start=${start}&end=${end}`,
    {
      headers: await authHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutFrequencyResponse[];
}

export async function startWorkout() {
  const res = await fetchWithTiming("start_workout", `${API_V1}/workouts`, {
    method: "POST",
    headers: await authHeaders(),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  const data = (await res.json()) as WorkoutResponse;
  trackProductEvent("workout_started");
  return data;
}

export async function getWorkoutDetails(workoutId: number) {
  const res = await fetchWithTiming(
    "get_workout_details",
    `${API_V1}/workouts/${workoutId}`,
    {
      headers: await authHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutDetailsResponse;
}

export async function addWorkoutSet(
  workoutId: number,
  payload: {
    exerciseId: number;
    reps: number;
    weight: number;
    rpe?: number | null;
  },
) {
  const res = await fetchWithTiming(
    "add_workout_set",
    `${API_V1}/workouts/${workoutId}/sets`,
    {
      method: "POST",
      headers: await authHeaders(true),
      body: JSON.stringify(payload),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as SetResponse;
}

export async function finishWorkout(workoutId: number) {
  const res = await fetchWithTiming(
    "finish_workout",
    `${API_V1}/workouts/${workoutId}/finish`,
    {
      method: "PATCH",
      headers: await authHeaders(true),
      body: JSON.stringify({}),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutResponse;
}

export async function getExercises() {
  const res = await fetchWithTiming("get_exercises", `${API_V1}/exercises`, {
    headers: await authHeaders(),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as ExerciseResponse[];
}

export async function createExercise(payload: {
  name: string;
  category: string;
}) {
  const res = await fetchWithTiming("create_exercise", `${API_V1}/exercises`, {
    method: "POST",
    headers: await authHeaders(true),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as ExerciseResponse;
}

export async function deleteExercise(exerciseId: number) {
  const res = await fetchWithTiming(
    "delete_exercise",
    `${API_V1}/exercises/${exerciseId}`,
    {
      method: "DELETE",
      headers: await authHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }
}

export async function ensureUserExercises() {
  const existingExercises = await getExercises();
  const existingNames = new Set(
    existingExercises.map((exercise) => exercise.name.trim().toLowerCase()),
  );

  const missingExercises = DEFAULT_EXERCISES.filter(
    (exercise) => !existingNames.has(exercise.name.toLowerCase()),
  );

  if (missingExercises.length === 0) {
    return existingExercises;
  }

  const createdExercises: ExerciseResponse[] = [];
  for (const exercise of missingExercises) {
    const created = await createExercise(exercise);
    createdExercises.push(created);
  }

  return [...existingExercises, ...createdExercises];
}
export async function getCoachRecommendation(
  numWorkouts: number = 5,
  userGoals: string = "general fitness",
  fitnessLevel: string = "intermediate",
) {
  const res = await fetchWithTiming(
    "get_coach_recommendation",
    `${API_V1}/coach/recommend?numWorkouts=${numWorkouts}&userGoals=${encodeURIComponent(userGoals)}&fitnessLevel=${fitnessLevel}`,
    {
      headers: await authHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(await parseApiError(res));
  }

  return (await res.json()) as CoachRecommendationResponse;
}
