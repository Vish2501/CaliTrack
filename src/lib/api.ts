import { supabase } from "./supabase";

const BASE_URL = "http://localhost:8080";

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
import { ExerciseEntry } from "../types/workout";

export async function commitWorkout(
  workoutId: number,
  entries: ExerciseEntry[],
) {
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

  for (const set of completedSets) {
    await addWorkoutSet(workoutId, set);
  }

  await finishWorkout(workoutId);

  return completedSets.length;
}

export type WorkoutDetailsResponse = WorkoutResponse & {
  sets: SetResponse[];
};

export async function getWorkouts() {
  const res = await fetch(`${BASE_URL}/api/workouts`, {
    headers: await authHeaders(),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutResponse[];
}

export type WorkoutFrequencyResponse = {
  weekStart: string;
  workoutCount: number;
};

export async function getWorkoutFrequency(start: string, end: string) {
  const res = await fetch(
    `${BASE_URL}/api/analytics/workout-frequency?start=${start}&end=${end}`,
    {
      headers: await authHeaders(),
    },
  );

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutFrequencyResponse[];
}

async function authHeaders(includeJson = false) {
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  if (!token) throw new Error("No session");

  return {
    Authorization: `Bearer ${token}`,
    ...(includeJson ? { "Content-Type": "application/json" } : {}),
  };
}

export async function startWorkout() {
  const res = await fetch(`${BASE_URL}/api/workouts`, {
    method: "POST",
    headers: await authHeaders(),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutResponse;
}

export async function getWorkoutDetails(workoutId: number) {
  const res = await fetch(`${BASE_URL}/api/workouts/${workoutId}`, {
    headers: await authHeaders(),
  });

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
  const res = await fetch(`${BASE_URL}/api/workouts/${workoutId}/sets`, {
    method: "POST",
    headers: await authHeaders(true),
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as SetResponse;
}

export async function finishWorkout(workoutId: number) {
  const res = await fetch(`${BASE_URL}/api/workouts/${workoutId}/finish`, {
    method: "PATCH",
    headers: await authHeaders(true),
    body: JSON.stringify({}),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }

  return (await res.json()) as WorkoutResponse;
}

export type ExerciseResponse = {
  id: number;
  name: string;
  category: string | null;
  userId: string;
};

export async function getExercises() {
  const res = await fetch(`${BASE_URL}/api/exercises`, {
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
  const res = await fetch(`${BASE_URL}/api/exercises`, {
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
  const res = await fetch(`${BASE_URL}/api/exercises/${exerciseId}`, {
    method: "DELETE",
    headers: await authHeaders(),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }
}
