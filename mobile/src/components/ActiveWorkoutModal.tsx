import React, { useState } from "react";
import { Alert, Modal, Pressable, Text, View } from "react-native";
import WorkoutDetailScreen from "../screens/WorkoutDetailsScreen";
import ChooseExerciseScreen from "../screens/ChooseExerciseScreen";
import { commitWorkout } from "../lib/api";
import { ActiveWorkoutUI, Exercise, ExerciseEntry } from "../types/workout";

type Props = {
  ui: ActiveWorkoutUI;
  onMinimize: () => void;
  onClose: () => void;
  onSetView: (view: ActiveWorkoutUI["view"]) => void;
  onSelectExercise: (exercise: Exercise) => void;
  onEntriesChange: (entries: ExerciseEntry[]) => void;
};

export default function ActiveWorkoutModal({
  ui,
  onMinimize,
  onClose,
  onSetView,
  onSelectExercise,
  onEntriesChange,
}: Props) {
  const [finishing, setFinishing] = useState(false);

  const handleFinishWorkout = async () => {
    if (!ui.workout) return;

    setFinishing(true);
    try {
      const savedCount = await commitWorkout(ui.workout.id, ui.entries);
      Alert.alert(
        "Workout Completed",
        `Saved ${savedCount} completed set${savedCount === 1 ? "" : "s"}.`,
      );
      onClose();
    } catch (error) {
      Alert.alert(
        "Finish failed",
        error instanceof Error ? error.message : "Could not finish workout",
      );
    } finally {
      setFinishing(false);
    }
  };
  return (
    <Modal
      visible={ui.isOpen}
      animationType="slide"
      transparent
      onRequestClose={onMinimize}
    >
      <View className="flex-1 bg-[rgba(1,22,30,0.6)] justify-end">
        <View className="bg-[#eff6e0] rounded-t-2xl pb-4 h-[85%] overflow-hidden">
          {ui.view === "chooseExercise" ? (
            <ChooseExerciseScreen
              embedded
              onClose={() => onSetView("details")}
              onSelectExercise={(exercise) => onSelectExercise(exercise)}
            />
          ) : (
            <>
              <WorkoutDetailScreen
                workout={ui.workout}
                selectedExercise={ui.selectedExercise}
                entries={ui.entries}
                onEntriesChange={onEntriesChange}
                embedded
                onMinimize={onMinimize}
                onClose={onClose}
                onAddExercise={() => onSetView("chooseExercise")}
              />
              <View className="mt-2 mx-5 flex-row gap-3">
                <Pressable
                  className="flex-1 bg-[#124559] py-2.5 rounded-xl items-center"
                  onPress={onMinimize}
                >
                  <Text className="text-[#eff6e0] font-bold">Minimize</Text>
                </Pressable>
                <Pressable
                  className={`flex-1 py-2.5 rounded-xl items-center ${finishing ? "bg-[#aec3b0]" : "bg-[#598392]"}`}
                  onPress={handleFinishWorkout}
                  disabled={finishing}
                >
                  <Text className="text-[#01161e] font-bold">
                    {finishing ? "Finishing..." : "Finish Workout"}
                  </Text>
                </Pressable>
              </View>
            </>
          )}
        </View>
      </View>
    </Modal>
  );
}
