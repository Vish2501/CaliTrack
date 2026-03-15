export type WorkoutsStackParamList = {
  WorkoutsHome: undefined;
  WorkoutDetail:
    | {
        workoutId?: string;
        selectedExercise?: {
          id: string;
          name: string;
          category: string;
        };
      }
    | undefined;
  ChooseExercise: undefined;
};

export type ProfileStackParamList = {
  ProfileHome: undefined;
  Metrics: undefined;
};
