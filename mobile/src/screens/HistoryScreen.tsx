import React, { useCallback, useState } from "react";
import { FlatList, Pressable, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useFocusEffect, useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { getWorkouts } from "../lib/api";
import { WorkoutsStackParamList } from "../types/navigation";
import { Workout } from "../types/workout";

export default function HistoryScreen() {
  const navigation =
    useNavigation<NativeStackNavigationProp<WorkoutsStackParamList>>();
  const [workouts, setWorkouts] = useState<Workout[]>([]);
  const [loadingWorkouts, setLoadingWorkouts] = useState(false);

  useFocusEffect(
    useCallback(() => {
      let active = true;

      (async () => {
        try {
          setLoadingWorkouts(true);
          const data = await getWorkouts();
          if (!active) return;

          setWorkouts(
            data
              .filter((workout) => workout.endTime !== null)
              .map((workout) => ({
                id: workout.id,
                name: workout.notes ?? "Workout",
                date: new Date(workout.startTime).toLocaleDateString("en-GB", {
                  weekday: "short",
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                }),
              })),
          );
        } finally {
          if (active) setLoadingWorkouts(false);
        }
      })();

      return () => {
        active = false;
      };
    }, []),
  );

  return (
    <SafeAreaView className="flex-1 bg-[#124559]">
      <View className="px-5 pt-3 pb-2">
        <Text className="text-2xl font-bold text-[#eff6e0]">History</Text>
      </View>

      {loadingWorkouts ? (
        <View className="px-5">
          <Text className="text-sm text-[#eff6e0]">Loading workouts...</Text>
        </View>
      ) : workouts.length === 0 ? (
        <View className="px-5">
          <Text className="text-sm text-[#aec3b0]">
            No completed workouts yet.
          </Text>
        </View>
      ) : (
        <FlatList
          data={workouts}
          keyExtractor={(item) => String(item.id)}
          contentContainerClassName="px-5 pb-6 gap-3"
          renderItem={({ item }) => (
            <Pressable
              className="bg-[#eff6e0] rounded-xl px-4 py-3 border border-[#aec3b0]"
              onPress={() =>
                navigation.navigate("WorkoutDetail", {
                  workoutId: String(item.id),
                })
              }
            >
              <Text className="text-sm font-bold text-[#01161e]">
                {item.name}
              </Text>
              <Text className="mt-1 text-xs text-[#124559]">{item.date}</Text>
            </Pressable>
          )}
        />
      )}
    </SafeAreaView>
  );
}
