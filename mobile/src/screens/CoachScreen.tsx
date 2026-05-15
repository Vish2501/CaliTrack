import React, { useState } from "react";
import {
  View,
  Text,
  ScrollView,
  Pressable,
  ActivityIndicator,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { getCoachRecommendation, CoachRecommendationResponse } from "../lib/api";

export default function CoachScreen() {
  const [recommendation, setRecommendation] = useState<CoachRecommendationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [fitnessLevel, setFitnessLevel] = useState("intermediate");
  const [userGoals, setUserGoals] = useState("general fitness");

  const fetchRecommendation = async () => {
    setLoading(true);
    try {
      const result = await getCoachRecommendation(5, userGoals, fitnessLevel);
      setRecommendation(result);
    } catch (error) {
      Alert.alert(
        "Error",
        error instanceof Error ? error.message : "Failed to get recommendation"
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
                  onPress={() => setFitnessLevel(level)}
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
                  onPress={() => setUserGoals(goal)}
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

        {/* Recommendation Display */}
        {recommendation && (
          <View className="gap-4">
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
        {!recommendation && !loading && (
          <View className="mt-8 items-center">
            <Text className="text-gray-500 text-center">
              Click "Get Recommendation" to see AI-powered coaching advice based on your
              recent workouts
            </Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
