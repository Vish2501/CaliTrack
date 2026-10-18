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
};
export type ExerciseEntry = {
  id: string;
  name: string;
  category: string;
  sets: SetEntry[];
};

export type ActiveWorkoutUI = {
  workout: Workout | null;
  isOpen: boolean;
  isMinimized: boolean;
  view: "details" | "chooseExercise";
  selectedExercise: Exercise | null;
  entries: ExerciseEntry[];
};
