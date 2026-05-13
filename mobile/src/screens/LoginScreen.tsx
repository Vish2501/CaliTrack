import React, { useState } from "react";
import { Alert, Pressable, Text, TextInput } from "react-native";
import { supabase } from "../lib/supabase";
import { SafeAreaView } from "react-native-safe-area-context";
export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {
    const { error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });
    if (error) Alert.alert("Login failed", error.message);
  };

  return (
    <SafeAreaView className="flex-1 justify-center px-5 bg-[#124559]">
      <Text className="text-2xl font-bold text-[#eff6e0] mb-3">Login</Text>

      <TextInput
        placeholder="Email"
        placeholderTextColor="#aec3b0"
        value={email}
        onChangeText={setEmail}
        className="border border-[#aec3b0] rounded-lg px-3 py-3 mb-2.5 bg-[#eff6e0] text-[#01161e]"
      />
      <TextInput
        placeholder="Password"
        placeholderTextColor="#aec3b0"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
        className="border border-[#aec3b0] rounded-lg px-3 py-3 mb-2.5 bg-[#eff6e0] text-[#01161e]"
      />

      <Pressable
        className="bg-[#598392] py-3 rounded-lg items-center"
        onPress={handleLogin}
      >
        <Text className="text-[#01161e] font-bold">Sign In</Text>
      </Pressable>
    </SafeAreaView>
  );
}
