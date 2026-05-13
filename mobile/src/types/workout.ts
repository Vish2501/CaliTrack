export type Workout = { id: number; name: string; date: string };

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
