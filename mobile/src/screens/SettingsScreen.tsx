import React, { useCallback, useState } from "react";
import { Alert, Pressable, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useFocusEffect, useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import {
  getWeightUnitPreference,
  setWeightUnitPreference,
  WeightUnit,
} from "../lib/preferences";
import { ProfileStackParamList } from "../types/navigation";

export default function SettingsScreen() {
  const navigation =
    useNavigation<NativeStackNavigationProp<ProfileStackParamList>>();
  const [weightUnit, setWeightUnit] = useState<WeightUnit>("kg");
  const [saving, setSaving] = useState(false);

  useFocusEffect(
    useCallback(() => {
      let active = true;

      (async () => {
        const unit = await getWeightUnitPreference();
        if (!active) return;
        setWeightUnit(unit);
      })();

      return () => {
        active = false;
      };
    }, []),
  );

  const updateUnit = async (unit: WeightUnit) => {
    try {
      setSaving(true);
      await setWeightUnitPreference(unit);
      setWeightUnit(unit);
    } catch (error) {
      Alert.alert(
        "Save failed",
        error instanceof Error ? error.message : "Could not save settings",
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-[#124559]">
      <View className="px-5 pt-3 pb-2 flex-row items-center justify-between">
        <View>
          <Text className="text-2xl font-bold text-[#eff6e0]">Settings</Text>
          <Text className="mt-1 text-xs text-[#aec3b0]">
            Personalize your workout preferences
          </Text>
        </View>
        <Pressable
          className="bg-[#01161e] px-4 py-2 rounded-full"
          onPress={() => navigation.goBack()}
        >
          <Text className="text-[#eff6e0] font-bold">Done</Text>
        </Pressable>
      </View>

      <View className="px-5 pt-4">
        <View className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl p-4">
          <Text className="text-sm font-bold text-[#01161e]">Weight Unit</Text>
          <Text className="mt-1 text-xs text-[#124559]">
            Used for workout weight inputs.
          </Text>

          <View className="mt-3 flex-row gap-2">
            <Pressable
              className={`flex-1 py-2 rounded-lg items-center border ${weightUnit === "kg" ? "bg-[#124559] border-[#124559]" : "bg-[#eff6e0] border-[#aec3b0]"}`}
              disabled={saving}
              onPress={() => updateUnit("kg")}
            >
              <Text
                className={`font-semibold ${weightUnit === "kg" ? "text-[#eff6e0]" : "text-[#124559]"}`}
              >
                kg
              </Text>
            </Pressable>
            <Pressable
              className={`flex-1 py-2 rounded-lg items-center border ${weightUnit === "lb" ? "bg-[#124559] border-[#124559]" : "bg-[#eff6e0] border-[#aec3b0]"}`}
              disabled={saving}
              onPress={() => updateUnit("lb")}
            >
              <Text
                className={`font-semibold ${weightUnit === "lb" ? "text-[#eff6e0]" : "text-[#124559]"}`}
              >
                lb
              </Text>
            </Pressable>
          </View>
        </View>
      </View>
    </SafeAreaView>
  );
}
