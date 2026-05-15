import React, { useCallback, useState } from "react";
import { Alert, Pressable, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useFocusEffect, useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { getWorkoutFrequency, WorkoutFrequencyResponse } from "../lib/api";
import { supabase } from "../lib/supabase";
import { ProfileStackParamList } from "../types/navigation";

function getWeekStart(date: Date) {
  const value = new Date(date);
  const day = value.getDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  value.setDate(value.getDate() + diffToMonday);
  value.setHours(0, 0, 0, 0);
  return value;
}

function toIsoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

export default function ProfileScreen() {
  const navigation =
    useNavigation<NativeStackNavigationProp<ProfileStackParamList>>();
  const [data, setData] = useState<WorkoutFrequencyResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const chartHeight = 170;
  const today = new Date();
  const fourWeeksAgo = new Date();
  fourWeeksAgo.setDate(today.getDate() - 28);
  const rangeStartWeek = getWeekStart(fourWeeksAgo);
  const rangeEndWeek = getWeekStart(today);

  const weekStarts: string[] = [];
  for (
    let cursor = new Date(rangeStartWeek);
    cursor <= rangeEndWeek;
    cursor.setDate(cursor.getDate() + 7)
  ) {
    weekStarts.push(toIsoDate(cursor));
  }

  const countByWeek = new Map(
    data.map((item) => [item.weekStart.slice(0, 10), item.workoutCount] as const),
  );
  const chartData = weekStarts.map((weekStart) => ({
    weekStart,
    workoutCount: countByWeek.get(weekStart) ?? 0,
  }));
  const maxCount = Math.max(1, ...chartData.map((item) => item.workoutCount));
  const yAxisStep = maxCount <= 6 ? 1 : Math.ceil(maxCount / 6);
  const yAxisTicks: number[] = [];
  for (let value = maxCount; value >= 0; value -= yAxisStep) {
    yAxisTicks.push(value);
  }
  if (yAxisTicks[yAxisTicks.length - 1] !== 0) {
    yAxisTicks.push(0);
  }

  const formatWeekLabel = (weekStart: string) => {
    const parsed = new Date(weekStart);
    if (Number.isNaN(parsed.getTime())) return weekStart;
    return `${parsed.getDate()}/${parsed.getMonth() + 1}`;
  };

  const handleLogout = async () => {
    try {
      const { error: signOutError } = await supabase.auth.signOut();
      if (signOutError) {
        throw signOutError;
      }
    } catch (err) {
      Alert.alert(
        "Logout failed",
        err instanceof Error ? err.message : "Could not log out",
      );
    }
  };

  const loadInsights = useCallback(async (activeRef: { active: boolean }) => {
    const now = new Date();
    const fourWeeksBack = new Date();
    fourWeeksBack.setDate(now.getDate() - 28);
    const start = toIsoDate(getWeekStart(fourWeeksBack));
    const end = toIsoDate(now);

    try {
      setLoading(true);
      const result = await getWorkoutFrequency(start, end);
      if (!activeRef.active) return;
      setData(result);
      setError(null);
    } catch (err) {
      if (!activeRef.active) return;
      setError(err instanceof Error ? err.message : "Failed to load insights");
    } finally {
      if (activeRef.active) setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      const activeRef = { active: true };
      void loadInsights(activeRef);

      return () => {
        activeRef.active = false;
      };
    }, [loadInsights]),
  );

  return (
    <SafeAreaView className="flex-1 bg-[#124559]">
      <View className="px-5 pt-3 pb-2 flex-row items-start justify-between">
        <View>
          <Text className="text-2xl font-bold text-[#eff6e0]">Profile</Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">
            Account and training insights
          </Text>
        </View>
        <View className="items-end gap-2">
          <Pressable
            className="bg-[#598392] px-4 py-2 rounded-full"
            onPress={() => navigation.navigate("Coach")}
          >
            <Text className="text-[#01161e] font-bold">AI Coach</Text>
          </Pressable>
          <Pressable
            className="bg-[#598392] px-4 py-2 rounded-full"
            onPress={() => navigation.navigate("Settings")}
          >
            <Text className="text-[#01161e] font-bold">Settings</Text>
          </Pressable>
          <Pressable
            className="bg-[#01161e] px-4 py-2 rounded-full"
            onPress={handleLogout}
          >
            <Text className="text-[#eff6e0] font-bold">Log Out</Text>
          </Pressable>
        </View>
      </View>
      <View className="px-5 pb-2">
        <Text className="mt-1 text-lg font-bold text-[#eff6e0]">
          Workouts per week
        </Text>
      </View>

      {loading ? (
        <View className="px-5 pt-4">
          <Text className="text-sm text-[#eff6e0]">Loading insights...</Text>
        </View>
      ) : error ? (
        <View className="px-5 pt-4">
          <Text className="text-sm font-semibold text-[#eff6e0]">
            Could not load insights
          </Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">{error}</Text>
        </View>
      ) : (
        <View className="px-5 pb-6">
          <View className="bg-[#eff6e0] rounded-2xl px-4 pt-4 pb-3">
            <View className="flex-row">
              <View
                className="justify-between pr-2"
                style={{ height: chartHeight, width: 24 }}
              >
                {yAxisTicks.map((tick, index) => (
                  <Text key={`${tick}-${index}`} className="text-[10px] text-[#124559]">
                    {tick}
                  </Text>
                ))}
              </View>
              <View className="flex-1 relative" style={{ height: chartHeight }}>
                {yAxisTicks.map((tick, index) => (
                  <View
                    key={index}
                    className="absolute left-0 right-0 border-t border-[#d4dfc8]"
                    style={{
                      bottom: (tick / maxCount) * chartHeight,
                    }}
                  />
                ))}
                <View className="absolute inset-0 flex-row items-end justify-around px-1">
                  {chartData.map((item) => {
                    const barHeight = Math.max(
                      6,
                      (item.workoutCount / maxCount) * (chartHeight - 4),
                    );
                    return (
                      <View key={item.weekStart} className="items-center">
                        <View
                          className="w-3 rounded-full bg-[#124559]"
                          style={{ height: barHeight }}
                        />
                      </View>
                    );
                  })}
                </View>
              </View>
            </View>
            <View className="mt-2 flex-row items-center justify-around px-1">
              {chartData.map((item) => (
                <Text key={item.weekStart} className="text-[10px] text-[#124559]">
                  {formatWeekLabel(item.weekStart)}
                </Text>
              ))}
            </View>
          </View>
        </View>
      )}
    </SafeAreaView>
  );
}
