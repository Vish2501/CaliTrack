import React, { useCallback, useState } from "react";
import { FlatList, Pressable, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import ActiveWorkoutModal from "../components/ActiveWorkoutModal";
import { ActiveWorkoutUI, Exercise, Workout } from "../types/workout";
import { startWorkout } from "../lib/api";

const TEMPLATES = [
  {
    id: "t1",
    name: "Push Day",
    lastPerformed: "Dec 16, 2025",
    items: [
      { name: "Chest Dip", category: "Chest", sets: 3 },
      { name: "Overhead Press (Barbell)", category: "Shoulders", sets: 3 },
    ],
  },
  {
    id: "t2",
    name: "Pull Day",
    lastPerformed: "Feb 10, 2026",
    items: [
      { name: "Lat Pulldown", category: "Back", sets: 4 },
      { name: "Incline Curl", category: "Biceps", sets: 3 },
    ],
  },
];

export default function WorkoutsScreen() {
  const [selectedTemplate, setSelectedTemplate] = useState<
    (typeof TEMPLATES)[number] | null
  >(null);
  const [startingWorkout, setStartingWorkout] = useState(false);
  const [activeWorkoutUI, setActiveWorkoutUI] = useState<ActiveWorkoutUI>({
    workout: null,
    isOpen: false,
    isMinimized: false,
    view: "details",
    selectedExercise: null,
    entries: [],
  });

  const openWorkout = (workout: Workout) => {
    setActiveWorkoutUI({
      workout,
      isOpen: true,
      isMinimized: false,
      view: "details",
      selectedExercise: null,
      entries: [],
    });
  };

  const minimizeWorkout = () => {
    setActiveWorkoutUI((prev) => {
      if (!prev.workout) return prev;
      return {
        ...prev,
        isOpen: false,
        isMinimized: true,
        view: "details",
        selectedExercise: null,
      };
    });
  };

  const closeWorkout = () => {
    setActiveWorkoutUI({
      workout: null,
      isOpen: false,
      isMinimized: false,
      view: "details",
      selectedExercise: null,
      entries: [],
    });
  };

  const setWorkoutView = useCallback((view: ActiveWorkoutUI["view"]) => {
    setActiveWorkoutUI((prev) => ({ ...prev, view }));
  }, []);

  const selectExercise = useCallback((exercise: Exercise) => {
    // Normalize IDs as strings because the local workout UI stores entry IDs as strings.
    const normalizedExercise = { ...exercise, id: String(exercise.id) };
    setActiveWorkoutUI((prev) => ({
      ...prev,
      selectedExercise: normalizedExercise,
      // Keep one entry per exercise; selecting an existing exercise reuses its current sets.
      entries: prev.entries.some(
        (entry) => String(entry.id) === normalizedExercise.id,
      )
        ? prev.entries
        : [
            ...prev.entries,
            {
              id: normalizedExercise.id,
              name: normalizedExercise.name,
              category: normalizedExercise.category,
              sets: [{ reps: "", weight: "", completed: false }],
            },
          ],
      view: "details",
    }));
  }, []);

  const changeEntries = useCallback((entries: ActiveWorkoutUI["entries"]) => {
    setActiveWorkoutUI((prev) => ({ ...prev, entries }));
  }, []);

  const beginWorkout = async (name: string) => {
    try {
      setStartingWorkout(true);
      const workout = await startWorkout();
      openWorkout({
        id: workout.id,
        name,
        date: new Date(workout.startTime).toLocaleDateString("en-GB", {
          day: "numeric",
          month: "short",
        }),
      });
    } finally {
      setStartingWorkout(false);
    }
  };

  return (
    <View className="flex-1 bg-[#124559]">
      <SafeAreaView className="flex-1">
        <View className="px-5 pt-3 pb-2 flex-row items-center justify-between">
          <Text className="text-2xl font-bold text-[#eff6e0]">Workouts</Text>

          <Pressable
            className="bg-[#598392] px-4 py-2 rounded-full"
            onPress={() => beginWorkout("Workout")}
          >
            <Text className="text-[#01161e] font-bold">
              {startingWorkout ? "Starting..." : "Start Workout"}
            </Text>
          </Pressable>
        </View>
        <View className="px-5 pt-2">
          <Text className="text-xs uppercase tracking-widest text-[#aec3b0]">
            Templates
          </Text>
        </View>

        <FlatList
          data={TEMPLATES}
          keyExtractor={(item) => item.id}
          horizontal
          showsHorizontalScrollIndicator={false}
          className="max-h-[140px]"
          contentContainerClassName="px-5 pt-3 pb-2 gap-3"
          renderItem={({ item }) => (
            <Pressable
              className="bg-[#eff6e0] rounded-xl px-4 py-3 border border-[#aec3b0] min-w-[140px] h-[120px]"
              onPress={() => setSelectedTemplate(item)}
            >
              <Text className="text-sm font-bold text-[#01161e]">
                {item.name}
              </Text>
              <Text className="mt-1 text-xs text-[#124559]">
                {item.items.length} exercises
              </Text>
            </Pressable>
          )}
        />
      </SafeAreaView>

      <ActiveWorkoutModal
        ui={activeWorkoutUI}
        onMinimize={minimizeWorkout}
        onClose={closeWorkout}
        onSetView={setWorkoutView}
        onSelectExercise={selectExercise}
        onEntriesChange={changeEntries}
      />
      {activeWorkoutUI.isMinimized && activeWorkoutUI.workout && (
        <Pressable
          className="absolute bottom-5 left-4 right-4 bg-[#01161e] border border-[#aec3b0] rounded-2xl px-4 py-3 flex-row items-center justify-between"
          onPress={() => {
            setActiveWorkoutUI((prev) => ({
              ...prev,
              isOpen: true,
              isMinimized: false,
            }));
          }}
          accessibilityLabel="Resume workout"
        >
          <View>
            <Text className="text-[#aec3b0] text-xs">Active workout</Text>
            <Text className="text-[#eff6e0] font-bold">
              {activeWorkoutUI.workout.name}
            </Text>
          </View>
          <Text className="text-[#598392] font-semibold">Resume</Text>
        </Pressable>
      )}
      {selectedTemplate && (
        <View className="absolute inset-0 bg-[rgba(1,22,30,0.55)] items-center justify-center px-5">
          <View className="w-full bg-[#124559] rounded-2xl p-4 border border-[#aec3b0]">
            <View className="flex-row items-center justify-between mb-2">
              <Pressable
                onPress={() => setSelectedTemplate(null)}
                className="w-7 h-7 rounded-full bg-[#01161e] items-center justify-center"
              >
                <Text className="text-[#eff6e0] font-bold">✕</Text>
              </Pressable>
              <Text className="text-lg font-bold text-[#eff6e0]">
                {selectedTemplate.name}
              </Text>
              <Pressable>
                <Text className="text-[#598392] font-semibold">Edit</Text>
              </Pressable>
            </View>

            <Text className="text-[#aec3b0] mb-3">
              Last Performed: {selectedTemplate.lastPerformed}
            </Text>

            {selectedTemplate.items.map((item) => (
              <View
                key={item.name}
                className="flex-row items-center gap-3 py-2"
              >
                <View className="w-8 h-8 rounded-full bg-[#01161e] items-center justify-center">
                  <Text className="text-[#eff6e0] font-bold">{item.sets}×</Text>
                </View>
                <View className="flex-1">
                  <Text className="text-[#eff6e0] font-bold">{item.name}</Text>
                  <Text className="text-[#aec3b0] mt-0.5">{item.category}</Text>
                </View>
                <Pressable className="w-6 h-6 rounded-full bg-[#01161e] items-center justify-center">
                  <Text className="text-[#eff6e0] font-bold">?</Text>
                </Pressable>
              </View>
            ))}

            <Pressable
              className="mt-3 bg-[#598392] py-3 rounded-xl items-center"
              onPress={async () => {
                setSelectedTemplate(null);
                await beginWorkout(selectedTemplate.name);
              }}
            >
              <Text className="text-[#01161e] font-bold">Start Workout</Text>
            </Pressable>
          </View>
        </View>
      )}
    </View>
  );
}
