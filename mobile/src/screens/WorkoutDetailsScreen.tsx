import React, { useEffect, useState } from "react";
import { SafeAreaView } from "react-native-safe-area-context";
import { Alert, Pressable, ScrollView, Text, TextInput, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect, useNavigation, useRoute } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { RouteProp } from "@react-navigation/native";
import { getWorkoutDetails } from "../lib/api";
import { getWeightUnitPreference, WeightUnit } from "../lib/preferences";
import { WorkoutsStackParamList } from "../types/navigation";
import { Exercise, ExerciseEntry, Workout } from "../types/workout";

type Props = {
  workout?: Workout | null;
  selectedExercise?: Exercise | null;
  entries?: ExerciseEntry[];
  onEntriesChange?: (entries: ExerciseEntry[]) => void;
  embedded?: boolean;
  onMinimize?: () => void;
  onClose?: () => void;
  onAddExercise?: () => void;
};

const noopEntriesChange = (_entries: ExerciseEntry[]) => {};

export default function WorkoutDetailScreen({
  workout,
  selectedExercise: selectedExerciseProp,
  entries = [],
  onEntriesChange = noopEntriesChange,
  embedded = false,
  onMinimize,
  onClose,
  onAddExercise,
}: Props) {
  const navigation =
    useNavigation<NativeStackNavigationProp<WorkoutsStackParamList>>();
  const route = useRoute<RouteProp<WorkoutsStackParamList, "WorkoutDetail">>();
  const selectedExercise =
    selectedExerciseProp ?? route.params?.selectedExercise ?? null;
  const [loadingWorkout, setLoadingWorkout] = useState(false);
  const [localEntries, setLocalEntries] = useState<ExerciseEntry[]>([]);
  const [weightUnit, setWeightUnit] = useState<WeightUnit>("kg");

  const resolvedWorkout = workout ?? {
    id: Number(route.params?.workoutId ?? 0),
    name: "Workout",
    date: "Today",
  };

  const isReadOnly = !embedded;
  const displayedEntries = embedded ? entries : localEntries;

  const updateEntries = (nextEntries: ExerciseEntry[]) => {
    if (embedded) {
      onEntriesChange(nextEntries);
    } else {
      setLocalEntries(nextEntries);
    }
  };

  useEffect(() => {
    if (!resolvedWorkout.id) return;

    let active = true;

    (async () => {
      try {
        setLoadingWorkout(true);
        const data = await getWorkoutDetails(resolvedWorkout.id);
        if (!active) return;

        const grouped = new Map<string, ExerciseEntry>();
        for (const set of data.sets) {
          const key = String(set.exerciseId);
          const existing = grouped.get(key);
          const setEntry = {
            reps: set.reps != null ? String(set.reps) : "",
            weight: set.weight != null ? String(set.weight) : "",
            completed: true,
          };

          if (existing) {
            existing.sets.push(setEntry);
          } else {
            grouped.set(key, {
              id: key,
              name: set.exerciseName,
              category: "",
              sets: [setEntry],
            });
          }
        }

        const serverEntries = Array.from(grouped.values());
        if (serverEntries.length > 0) {
          const merged = [...displayedEntries];
          for (const serverEntry of serverEntries) {
            const existingIndex = merged.findIndex(
              (entry) => String(entry.id) === String(serverEntry.id),
            );
            if (existingIndex >= 0) {
              merged[existingIndex] = {
                ...merged[existingIndex],
                ...serverEntry,
              };
            } else {
              merged.push(serverEntry);
            }
          }
          updateEntries(merged);
        }
      } catch (error) {
        if (!active) return;
        Alert.alert(
          "Workout load failed",
          error instanceof Error
            ? error.message
            : "Could not load workout details",
        );
      } finally {
        if (active) setLoadingWorkout(false);
      }
    })();

    return () => {
      active = false;
    };
  }, [resolvedWorkout.id]);

  useFocusEffect(
    React.useCallback(() => {
      let active = true;

      (async () => {
        const preferredUnit = await getWeightUnitPreference();
        if (!active) return;
        setWeightUnit(preferredUnit);
      })();

      return () => {
        active = false;
      };
    }, []),
  );

  const updateSetField = (
    exerciseId: string,
    setIndex: number,
    field: "reps" | "weight",
    value: string,
  ) => {
    updateEntries(
      displayedEntries.map((entry) =>
        String(entry.id) === String(exerciseId)
          ? {
              ...entry,
              sets: entry.sets.map((set, index) =>
                index === setIndex ? { ...set, [field]: value } : set,
              ),
            }
          : entry,
      ),
    );
  };

  const toggleSetCompleted = (exerciseId: string, setIndex: number) => {
    updateEntries(
      displayedEntries.map((entry) =>
        String(entry.id) === String(exerciseId)
          ? {
              ...entry,
              sets: entry.sets.map((set, index) =>
                index === setIndex
                  ? { ...set, completed: !set.completed }
                  : set,
              ),
            }
          : entry,
      ),
    );
  };

  const addLocalSet = (exerciseId: string) => {
    updateEntries(
      displayedEntries.map((entry) =>
        String(entry.id) === String(exerciseId)
          ? {
              ...entry,
              sets: [...entry.sets, { reps: "", weight: "", completed: false }],
            }
          : entry,
      ),
    );
  };

  return (
    <SafeAreaView className="flex-1 bg-[#124559] p-5">
      <View className="flex-row items-center gap-3 mb-4">
        <Pressable
          onPress={() => {
            if (embedded) {
              onMinimize?.();
              return;
            }
            navigation.goBack();
          }}
          className="py-1.5 px-2.5 rounded-full bg-[#01161e]"
          accessibilityLabel={embedded ? "Minimize workout" : "Go back"}
        >
          <Ionicons
            name={embedded ? "chevron-down" : "chevron-back"}
            size={18}
            color="#eff6e0"
          />
        </Pressable>
        <View className="gap-0.5">
          <Text className="text-xl font-bold text-[#eff6e0]">
            {resolvedWorkout.name}
          </Text>
          <Text className="text-xs text-[#aec3b0]">{resolvedWorkout.date}</Text>
        </View>
        {embedded && (
          <Pressable
            onPress={onClose}
            className="ml-auto py-1.5 px-2.5 rounded-full bg-[#01161e]"
            accessibilityLabel="Close workout"
          >
            <Ionicons name="close" size={18} color="#eff6e0" />
          </Pressable>
        )}
      </View>

      {selectedExercise && (
        <View className="mb-3">
          <Text className="text-xs text-[#aec3b0] font-semibold">Selected</Text>
          <Text className="text-base font-bold text-[#eff6e0]">
            {selectedExercise.name}
          </Text>
        </View>
      )}

      {loadingWorkout ? (
        <View className="flex-1 items-center justify-center gap-1.5">
          <Text className="text-base font-semibold text-[#eff6e0]">
            Loading workout...
          </Text>
        </View>
      ) : displayedEntries.length === 0 ? (
        <View className="flex-1 items-center justify-center gap-1.5">
          <Text className="text-base font-semibold text-[#eff6e0]">
            No sets yet
          </Text>
          <Text className="text-xs text-[#aec3b0]">
            Tap “Add Exercise” to start.
          </Text>
        </View>
      ) : (
        <ScrollView className="flex-1" contentContainerClassName="gap-4 pb-4">
          {displayedEntries.map((exercise) => (
            <View
              key={String(exercise.id)}
              className="bg-[#01161e] rounded-xl p-4"
            >
              <Text className="text-base font-bold text-[#eff6e0]">
                {exercise.name}
              </Text>
              <Text className="text-xs text-[#aec3b0] mb-3">
                {exercise.category}
              </Text>

              {exercise.sets.map((set, index) => (
                <View
                  key={`${exercise.id}-set-${index}`}
                  className="flex-row items-center gap-2 mb-2"
                >
                  <Text className="text-[#aec3b0] w-6">{index + 1}</Text>
                  <TextInput
                    placeholder={weightUnit}
                    placeholderTextColor="#aec3b0"
                    value={set.weight}
                    keyboardType="numeric"
                    editable={!isReadOnly}
                    onChangeText={(value) =>
                      updateSetField(exercise.id, index, "weight", value)
                    }
                    className="flex-1 bg-[#124559] text-[#eff6e0] px-3 py-2 rounded-lg"
                  />
                  <TextInput
                    placeholder="reps"
                    placeholderTextColor="#aec3b0"
                    value={set.reps}
                    editable={!isReadOnly}
                    keyboardType="numeric"
                    onChangeText={(value) =>
                      updateSetField(exercise.id, index, "reps", value)
                    }
                    className="flex-1 bg-[#124559] text-[#eff6e0] px-3 py-2 rounded-lg"
                  />
                  {!isReadOnly && (
                    <Pressable
                      className={`px-3 py-2 rounded-lg ${set.completed ? "bg-[#eff6e0]" : "bg-[#598392]"}`}
                      onPress={() => toggleSetCompleted(exercise.id, index)}
                    >
                      <Ionicons
                        name="checkmark"
                        size={16}
                        color={set.completed ? "#01161e" : "#eff6e0"}
                      />
                    </Pressable>
                  )}
                </View>
              ))}

              {!isReadOnly && (
                <Pressable
                  className="mt-2 bg-[#598392] py-2 rounded-lg items-center"
                  onPress={() => addLocalSet(exercise.id)}
                >
                  <Text className="text-[#01161e] font-bold">Add Set</Text>
                </Pressable>
              )}
            </View>
          ))}
        </ScrollView>
      )}

      {!isReadOnly && (
        <Pressable
          className="bg-[#598392] py-3 rounded-xl items-center"
          onPress={() => {
            if (embedded && onAddExercise) {
              onAddExercise();
              return;
            }
            navigation.navigate("ChooseExercise");
          }}
        >
          <Text className="text-[#01161e] font-bold">Add Exercise</Text>
        </Pressable>
      )}
    </SafeAreaView>
  );
}
