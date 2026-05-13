import React, { useState } from "react";
import { Pressable, ScrollView, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { getTelemetrySnapshot } from "../lib/telemetry";

export default function MetricsScreen() {
  const [snapshot, setSnapshot] = useState(getTelemetrySnapshot());

  const refresh = () => {
    setSnapshot(getTelemetrySnapshot());
  };

  return (
    <SafeAreaView className="flex-1 bg-[#124559]">
      <View className="px-5 pt-3 pb-2 flex-row items-center justify-between">
        <View>
          <Text className="text-2xl font-bold text-[#eff6e0]">Metrics</Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">
            Local telemetry snapshot
          </Text>
        </View>
        <Pressable
          className="bg-[#598392] px-4 py-2 rounded-full"
          onPress={refresh}
        >
          <Text className="text-[#01161e] font-bold">Refresh</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerClassName="px-5 pb-6 gap-4">
        <View className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl p-4">
          <Text className="text-sm font-bold text-[#01161e]">
            Product Events
          </Text>
          {Object.entries(snapshot.productEvents).map(([name, count]) => (
            <View key={name} className="mt-2 flex-row items-center justify-between">
              <Text className="text-xs text-[#124559]">{name}</Text>
              <Text className="text-xs font-semibold text-[#01161e]">{count}</Text>
            </View>
          ))}
        </View>

        <View className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl p-4">
          <Text className="text-sm font-bold text-[#01161e]">API Timings</Text>
          {snapshot.api.length === 0 ? (
            <Text className="mt-2 text-xs text-[#124559]">
              No API telemetry yet. Use the app and refresh.
            </Text>
          ) : (
            snapshot.api.map((item) => (
              <View key={item.endpoint} className="mt-3">
                <Text className="text-xs font-semibold text-[#01161e]">
                  {item.endpoint}
                </Text>
                <Text className="mt-1 text-xs text-[#124559]">
                  count={item.count} p50={item.p50Ms.toFixed(1)}ms p95=
                  {item.p95Ms.toFixed(1)}ms lastStatus={item.lastStatus ?? "n/a"}
                </Text>
              </View>
            ))
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
