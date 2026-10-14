import React, { useCallback, useEffect, useState } from "react";
import {
  View,
  Text,
  ScrollView,
  Pressable,
  ActivityIndicator,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useFocusEffect } from "@react-navigation/native";
import {
  getCoachProfile,
  getCoachRecommendation,
  getLatestCoachRecommendation,
  updateCoachProfile,
  CoachRecommendationResponse,
} from "../lib/api";

const PENDING_POLL_MS = 3000;

type DisplayedRecommendation = CoachRecommendationResponse & {
  createdAt?: string;
};

export default function CoachScreen() {
  const [recommendation, setRecommendation] =
    useState<DisplayedRecommendation | null>(null);
  const [loading, setLoading] = useState(false);
  const [pending, setPending] = useState(false);
  const [fitnessLevel, setFitnessLevel] = useState("intermediate");
  const [userGoals, setUserGoals] = useState("general fitness");

  useFocusEffect(
    useCallback(() => {
      let active = true;

      (async () => {
        try {
          const [profile, latest] = await Promise.all([
            getCoachProfile(),
            getLatestCoachRecommendation(),
          ]);
          if (!active) return;
          setUserGoals(profile.goals);
          setFitnessLevel(profile.fitnessLevel);
          if (latest.recommendation) setRecommendation(latest.recommendation);
          setPending(latest.pending);
        } catch {
          // Offline, or an API without these endpoints yet: keep the defaults and the
          // manual "Get Recommendation" button.
        }
      })();

      return () => {
        active = false;
      };
    }, []),
  );

  // While the backend generates a recommendation for a just-finished workout, check back until
  // it lands. The server stops reporting pending after a couple of minutes, which ends the loop.
  useEffect(() => {
    if (!pending) return;

    const timer = setInterval(async () => {
      try {
        const latest = await getLatestCoachRecommendation();
        if (latest.recommendation) setRecommendation(latest.recommendation);
        setPending(latest.pending);
      } catch {
        // Try again on the next tick.
      }
    }, PENDING_POLL_MS);

    return () => clearInterval(timer);
  }, [pending]);

  const saveProfile = async (goals: string, level: string) => {
    setUserGoals(goals);
    setFitnessLevel(level);
    try {
      await updateCoachProfile({ goals, fitnessLevel: level });
    } catch (error) {
      Alert.alert(
        "Couldn't save coach settings",
        error instanceof Error ? error.message : "Please try again",
      );
    }
  };

  const fetchRecommendation = async () => {
    setLoading(true);
    try {
      const result = await getCoachRecommendation(5, userGoals, fitnessLevel);
      setRecommendation(result);
    } catch (error) {
      Alert.alert(
        "Error",
        error instanceof Error ? error.message : "Failed to get recommendation",
      );
    } finally {
      setLoading(false);
    }
  };

  const fitnessLevels = ["beginner", "intermediate", "advanced"];
  const goalOptions = [
    "general fitness",
    "build strength",
    "build muscle",
    "improve endurance",
    "lose weight",
  ];

  return (
    <SafeAreaView className="flex-1 bg-white">
      <ScrollView className="flex-1 px-6 py-4">
        {/* Header */}
        <View className="mb-6">
          <Text className="text-3xl font-bold text-gray-900">AI Coach</Text>
          <Text className="text-gray-600 mt-2">
            Get personalized workout recommendations based on your history
          </Text>
        </View>

        {/* Settings */}
        <View className="bg-gray-50 rounded-lg p-4 mb-6">
          <View className="mb-4">
            <Text className="text-sm font-semibold text-gray-700 mb-2">
              Fitness Level
            </Text>
            <View className="flex-row gap-2">
              {fitnessLevels.map((level) => (
                <Pressable
                  key={level}
                  onPress={() => saveProfile(userGoals, level)}
                  className={`flex-1 py-2 px-3 rounded-md ${
                    fitnessLevel === level
                      ? "bg-blue-500"
                      : "bg-white border border-gray-300"
                  }`}
                >
                  <Text
                    className={`text-center text-sm font-medium ${
                      fitnessLevel === level ? "text-white" : "text-gray-700"
                    }`}
                  >
                    {level}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <View>
            <Text className="text-sm font-semibold text-gray-700 mb-2">
              Primary Goal
            </Text>
            <View className="gap-2">
              {goalOptions.map((goal) => (
                <Pressable
                  key={goal}
                  onPress={() => saveProfile(goal, fitnessLevel)}
                  className={`py-2 px-3 rounded-md ${
                    userGoals === goal
                      ? "bg-blue-500"
                      : "bg-white border border-gray-300"
                  }`}
                >
                  <Text
                    className={`text-sm font-medium ${
                      userGoals === goal ? "text-white" : "text-gray-700"
                    }`}
                  >
                    {goal}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>
        </View>

        {/* Get Recommendation Button */}
        <Pressable
          onPress={fetchRecommendation}
          disabled={loading}
          className="bg-blue-600 py-3 px-4 rounded-lg mb-6 active:bg-blue-700"
        >
          {loading ? (
            <ActivityIndicator color="white" />
          ) : (
            <Text className="text-white text-center font-semibold">
              Get Recommendation
            </Text>
          )}
        </Pressable>

        {/* Background generation in progress */}
        {pending && (
          <View className="flex-row items-center gap-3 bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
            <ActivityIndicator />
            <Text className="flex-1 text-blue-900">
              Generating a recommendation from your latest workout…
            </Text>
          </View>
        )}

        {/* Recommendation Display */}
        {recommendation && (
          <View className="gap-4">
            {recommendation.createdAt && (
              <Text className="text-xs text-gray-500">
                Based on your workout on{" "}
                {new Date(recommendation.createdAt).toLocaleDateString()}
              </Text>
            )}

            {/* Main Recommendation */}
            <View className="bg-blue-50 border border-blue-200 rounded-lg p-4">
              <Text className="text-sm font-semibold text-blue-900 mb-2">
                💡 Recommendation
              </Text>
              <Text className="text-base text-blue-900 leading-6">
                {recommendation.recommendation}
              </Text>
            </View>

            {/* Focus Area */}
            <View className="bg-amber-50 border border-amber-200 rounded-lg p-4">
              <Text className="text-sm font-semibold text-amber-900 mb-2">
                🎯 Focus Area
              </Text>
              <Text className="text-base text-amber-900 font-medium">
                {recommendation.focusArea}
              </Text>
            </View>

            {/* Next Exercise */}
            <View className="bg-green-50 border border-green-200 rounded-lg p-4">
              <Text className="text-sm font-semibold text-green-900 mb-2">
                💪 Next Exercise
              </Text>
              <Text className="text-base text-green-900 font-medium">
                {recommendation.suggestedNextExercise}
              </Text>
            </View>
          </View>
        )}

        {/* Empty State */}
        {!recommendation && !loading && !pending && (
          <View className="mt-8 items-center">
            <Text className="text-gray-500 text-center">
              Finish a workout and your coach will suggest what to do next, or
              tap "Get Recommendation" for advice based on your recent workouts
            </Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
