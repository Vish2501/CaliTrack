import AsyncStorage from "@react-native-async-storage/async-storage";

export type WeightUnit = "kg" | "lb";

const WEIGHT_UNIT_KEY = "@calitrack:weight_unit";

export async function getWeightUnitPreference(): Promise<WeightUnit> {
  try {
    const value = await AsyncStorage.getItem(WEIGHT_UNIT_KEY);
    return value === "lb" ? "lb" : "kg";
  } catch {
    return "kg";
  }
}

export async function setWeightUnitPreference(unit: WeightUnit) {
  await AsyncStorage.setItem(WEIGHT_UNIT_KEY, unit);
}
