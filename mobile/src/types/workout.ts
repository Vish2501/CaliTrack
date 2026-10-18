import type { Suggestion } from "../lib/suggestions";

// The active workout. It lives only on the phone until it is finished.
export type Workout = {
  // Generated on the phone, so retrying Finish can't save the workout twice.
  id: string;
  name: string;
  date: string;
  startedAt: string;
};

export type Exercise = { id: string; name: string; category: string };
export type SetEntry = {
  reps: string;
  weight: string;
  completed: boolean;
  // True when ticking filled the values in from the suggestions, until the user edits the set.
  // Filled values don't carry down to the sets below; only values the user typed do.
  fromSuggestion?: boolean;
};
export type ExerciseEntry = {
  id: string;
  name: string;
  category: string;
  sets: SetEntry[];
  // This exercise's sets from the last time it was done, once loaded. Used for suggestions.
  lastSession?: Suggestion[];
};

export type ActiveWorkoutUI = {
  workout: Workout | null;
  isOpen: boolean;
  isMinimized: boolean;
  view: "details" | "chooseExercise";
  selectedExercise: Exercise | null;
  entries: ExerciseEntry[];
};
