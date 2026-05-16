import React, { useState } from "react";
import { Alert, Pressable, Text, TextInput, View } from "react-native";
import { supabase } from "../lib/supabase";
import { SafeAreaView } from "react-native-safe-area-context";

export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSignUp, setIsSignUp] = useState(false);
  const [busy, setBusy] = useState(false);

  const handleSignIn = async () => {
    setBusy(true);
    const { error } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password,
    });
    setBusy(false);
    if (error) Alert.alert("Sign in failed", error.message);
  };

  const handleSignUp = async () => {
    if (!email.trim() || password.length < 6) {
      Alert.alert(
        "Check your details",
        "Use a valid email and a password with at least 6 characters (Supabase default).",
      );
      return;
    }
    setBusy(true);
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
    });
    setBusy(false);
    if (error) {
      Alert.alert("Sign up failed", error.message);
      return;
    }
    if (data.session) {
      Alert.alert("Welcome", "Your account is ready.");
    } else {
      Alert.alert(
        "Check your email",
        "We sent a confirmation link if your project requires email verification. After confirming, sign in.",
      );
    }
  };

  return (
    <SafeAreaView className="flex-1 justify-center px-5 bg-[#124559]">
      <Text className="text-2xl font-bold text-[#eff6e0] mb-3">
        {isSignUp ? "Create account" : "Sign in"}
      </Text>

      <View className="flex-row mb-4 gap-2">
        <Pressable
          className={`flex-1 py-2 rounded-lg items-center ${!isSignUp ? "bg-[#598392]" : "bg-[#eff6e0]/20"}`}
          onPress={() => setIsSignUp(false)}
        >
          <Text className={`font-semibold ${!isSignUp ? "text-[#01161e]" : "text-[#eff6e0]"}`}>
            Sign in
          </Text>
        </Pressable>
        <Pressable
          className={`flex-1 py-2 rounded-lg items-center ${isSignUp ? "bg-[#598392]" : "bg-[#eff6e0]/20"}`}
          onPress={() => setIsSignUp(true)}
        >
          <Text className={`font-semibold ${isSignUp ? "text-[#01161e]" : "text-[#eff6e0]"}`}>
            Register
          </Text>
        </Pressable>
      </View>

      <TextInput
        placeholder="Email"
        placeholderTextColor="#aec3b0"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        autoComplete="email"
        className="border border-[#aec3b0] rounded-lg px-3 py-3 mb-2.5 bg-[#eff6e0] text-[#01161e]"
      />
      <TextInput
        placeholder="Password"
        placeholderTextColor="#aec3b0"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
        autoComplete={isSignUp ? "password-new" : "password"}
        className="border border-[#aec3b0] rounded-lg px-3 py-3 mb-2.5 bg-[#eff6e0] text-[#01161e]"
      />

      <Pressable
        className="bg-[#598392] py-3 rounded-lg items-center opacity-100"
        disabled={busy}
        onPress={isSignUp ? handleSignUp : handleSignIn}
      >
        <Text className="text-[#01161e] font-bold">
          {busy ? "Please wait…" : isSignUp ? "Create account" : "Sign In"}
        </Text>
      </Pressable>
    </SafeAreaView>
  );
}
