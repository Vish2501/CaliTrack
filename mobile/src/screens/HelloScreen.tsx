import React, { useState } from "react";
import { Pressable, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

export default function HelloScreen() {
  const [count, setCount] = useState(0);

  return (
    <SafeAreaView className="flex-1 bg-[#124559] justify-center items-center px-5">
      <View className="bg-[#eff6e0] border border-[#aec3b0] rounded-xl p-5 w-full gap-2">
        <Text className="text-2xl font-bold text-[#01161e]">Hello User</Text>
        <Text className="text-sm text-[#124559]">Your first React Native screen</Text>
        <Text className="text-sm text-[#124559]">Testing</Text>
        <Text className="text-sm text-[#124559]">Count: {count}</Text>
        <Pressable
          className="mt-3 bg-[#598392] py-2.5 rounded-lg items-center"
          onPress={() => setCount(count + 1)}
        >
          <Text className="text-[#01161e] font-bold">Tap me</Text>
        </Pressable>
        <Pressable
          className="bg-[#598392] py-2.5 rounded-lg items-center"
          onPress={() => setCount(0)}
        >
          <Text className="text-[#01161e] font-bold">Reset</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}
