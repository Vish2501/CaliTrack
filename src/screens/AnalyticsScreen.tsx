import React, { useEffect, useState } from "react";
import { FlatList, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { getWorkoutFrequency, WorkoutFrequencyResponse } from "../lib/api";

export default function AnalyticsScreen() {
  const [data, setData] = useState<WorkoutFrequencyResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    const today = new Date();
    const fourWeeksAgo = new Date();
    fourWeeksAgo.setDate(today.getDate() - 28);

    const start = fourWeeksAgo.toISOString().slice(0, 10);
    const end = today.toISOString().slice(0, 10);

    (async () => {
      try {
        setLoading(true);
        const result = await getWorkoutFrequency(start, end);
        if (!active) return;
        setData(result);
        setError(null);
      } catch (err) {
        if (!active) return;
        setError(
          err instanceof Error ? err.message : "Failed to load analytics",
        );
      } finally {
        if (active) setLoading(false);
      }
    })();

    return () => {
      active = false;
    };
  }, []);

  return (
    <SafeAreaView className="flex-1 bg-[#124559]">
      <View className="px-5 pt-3 pb-2">
        <Text className="text-2xl font-bold text-[#eff6e0]">Analytics</Text>
        <Text className="mt-1 text-xs text-[#aec3b0]">
          Workout frequency over the last 4 weeks
        </Text>
      </View>

      {loading ? (
        <View className="px-5 pt-4">
          <Text className="text-sm text-[#eff6e0]">Loading analytics...</Text>
        </View>
      ) : error ? (
        <View className="px-5 pt-4">
          <Text className="text-sm font-semibold text-[#eff6e0]">
            Could not load analytics
          </Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">{error}</Text>
        </View>
      ) : data.length === 0 ? (
        <View className="px-5 pt-4">
          <Text className="text-sm text-[#aec3b0]">
            No workout analytics yet.
          </Text>
        </View>
      ) : (
        <FlatList
          data={data}
          keyExtractor={(item) => item.weekStart}
          contentContainerClassName="px-5 pb-6 gap-3"
          renderItem={({ item }) => (
            <View className="bg-[#eff6e0] rounded-xl px-4 py-3 border border-[#aec3b0]">
              <Text className="text-sm font-bold text-[#01161e]">
                Week of {item.weekStart}
              </Text>
              <Text className="mt-1 text-xs text-[#124559]">
                {item.workoutCount} workout{item.workoutCount === 1 ? "" : "s"}
              </Text>
            </View>
          )}
        />
      )}
    </SafeAreaView>
  );
}
