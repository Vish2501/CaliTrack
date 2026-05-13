import React, { useState } from "react";
import {
  Alert,
  FlatList,
  Pressable,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useFocusEffect } from "@react-navigation/native";
import {
  createExercise,
  ensureUserExercises,
  ExerciseResponse,
} from "../lib/api";
import { trackProductEvent } from "../lib/telemetry";

export default function ExercisesScreen() {
  const [exercises, setExercises] = useState<ExerciseResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState("");
  const [category, setCategory] = useState("");

  useFocusEffect(
    React.useCallback(() => {
      let active = true;

      (async () => {
        try {
          setLoading(true);
          const data = await ensureUserExercises();
          if (!active) return;
          setExercises(data);
          setError(null);
        } catch (err) {
          if (!active) return;
          setError(
            err instanceof Error ? err.message : "Failed to load exercises",
          );
        } finally {
          if (active) setLoading(false);
        }
      })();

      return () => {
        active = false;
      };
    }, []),
  );

  const handleCreate = async () => {
    if (!name.trim()) return;

    try {
      const created = await createExercise({
        name: name.trim(),
        category: category.trim() || "Uncategorized",
      });
      trackProductEvent("exercise_created");

      setExercises((prev) => [created, ...prev]);
      setName("");
      setCategory("");
      setShowForm(false);
    } catch (err) {
      Alert.alert(
        "Create failed",
        err instanceof Error ? err.message : "Could not create exercise",
      );
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-[#124559]">
      <View className="px-5 pt-3 pb-2 flex-row items-center justify-between">
        <View>
          <Text className="text-2xl font-bold text-[#eff6e0]">Exercises</Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">
            Your exercise library
          </Text>
        </View>

        <View className="flex-row items-center gap-2">
          <Pressable
            className="bg-[#598392] px-4 py-2 rounded-full"
            onPress={() => setShowForm((prev) => !prev)}
          >
            <Text className="text-[#01161e] font-bold">
              {showForm ? "Close" : "New"}
            </Text>
          </Pressable>
        </View>
      </View>

      {showForm && (
        <View className="px-5 gap-2 mb-3">
          <TextInput
            value={name}
            onChangeText={setName}
            placeholder="Exercise name"
            placeholderTextColor="#aec3b0"
            className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl px-3 py-3 text-[#01161e]"
          />
          <TextInput
            value={category}
            onChangeText={setCategory}
            placeholder="Category"
            placeholderTextColor="#aec3b0"
            className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl px-3 py-3 text-[#01161e]"
          />
          <Pressable
            className="bg-[#598392] py-3 rounded-xl items-center"
            onPress={handleCreate}
          >
            <Text className="text-[#01161e] font-bold">Add Exercise</Text>
          </Pressable>
        </View>
      )}

      {loading ? (
        <View className="px-5 pt-4">
          <Text className="text-sm text-[#eff6e0]">Loading exercises...</Text>
        </View>
      ) : error ? (
        <View className="px-5 pt-4">
          <Text className="text-sm font-semibold text-[#eff6e0]">
            Could not load exercises
          </Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">{error}</Text>
        </View>
      ) : exercises.length === 0 ? (
        <View className="px-5 pt-4">
          <Text className="text-sm text-[#aec3b0]">No exercises yet.</Text>
        </View>
      ) : (
        <FlatList
          data={exercises}
          keyExtractor={(item) => String(item.id)}
          contentContainerClassName="px-5 pb-6 gap-3"
          renderItem={({ item }) => (
            <View className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl p-4">
              <View className="flex-row items-start justify-between">
                <View className="flex-1 pr-3">
                  <Text className="text-base font-semibold text-[#01161e]">
                    {item.name}
                  </Text>
                  <Text className="mt-1 text-xs text-[#124559]">
                    {item.category ?? "Uncategorized"}
                  </Text>
                </View>
              </View>
            </View>
          )}
        />
      )}
    </SafeAreaView>
  );
}
