import React, { useEffect, useState } from "react";
import "./global.css";
import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { supabase } from "./src/lib/supabase";
import { COLORS } from "./src/theme/colors";

import WorkoutScreen from "./src/screens/WorkoutScreen";
import ExerciseScreen from "./src/screens/ExerciseScreen";
import ProfileScreen from "./src/screens/ProfileScreen";
import MetricsScreen from "./src/screens/MetricsScreen";
import SettingsScreen from "./src/screens/SettingsScreen";
import LoginScreen from "./src/screens/LoginScreen";
import HistoryScreen from "./src/screens/HistoryScreen";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import WorkoutDetailScreen from "./src/screens/WorkoutDetailsScreen";
import ChooseExerciseScreen from "./src/screens/ChooseExerciseScreen";
import { GestureHandlerRootView } from "react-native-gesture-handler";

const Tab = createBottomTabNavigator();
const ProfileStack = createNativeStackNavigator();
const WorkoutsStack = createNativeStackNavigator();
const HistoryStack = createNativeStackNavigator();

function ProfileStackScreen() {
  return (
    <ProfileStack.Navigator>
      <ProfileStack.Screen
        name="ProfileHome"
        component={ProfileScreen}
        options={{ headerShown: false }}
      />
      <ProfileStack.Screen
        name="Metrics"
        component={MetricsScreen}
        options={{ headerShown: false }}
      />
      <ProfileStack.Screen
        name="Settings"
        component={SettingsScreen}
        options={{ headerShown: false }}
      />
    </ProfileStack.Navigator>
  );
}

function WorkoutsStackScreen() {
  return (
    <WorkoutsStack.Navigator>
      <WorkoutsStack.Screen
        name="WorkoutsHome"
        component={WorkoutScreen}
        options={{ headerShown: false }}
      />
      <WorkoutsStack.Screen
        name="WorkoutDetail"
        component={WorkoutDetailScreen}
        options={{ headerShown: false }}
      />
      <WorkoutsStack.Screen
        name="ChooseExercise"
        component={ChooseExerciseScreen}
        options={{ headerShown: false }}
      />
    </WorkoutsStack.Navigator>
  );
}

function HistoryStackScreen() {
  return (
    <HistoryStack.Navigator>
      <HistoryStack.Screen
        name="WorkoutsHome"
        component={HistoryScreen}
        options={{ headerShown: false }}
      />
      <HistoryStack.Screen
        name="WorkoutDetail"
        component={WorkoutDetailScreen}
        options={{ headerShown: false }}
      />
    </HistoryStack.Navigator>
  );
}

function Tabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarStyle: {
          backgroundColor: COLORS.prussianBlue,
          borderTopColor: COLORS.alabaster,
        },
        tabBarActiveTintColor: COLORS.orange,
        tabBarInactiveTintColor: COLORS.alabaster,
        tabBarIcon: ({ color, size }) => {
          let iconName: keyof typeof Ionicons.glyphMap = "ellipse";

          if (route.name === "Profile") iconName = "person-circle-outline";
          if (route.name === "History") iconName = "time-outline";
          if (route.name === "Workouts") iconName = "barbell-outline";
          if (route.name === "Exercises") iconName = "list-outline";

          return <Ionicons name={iconName} size={size} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Profile" component={ProfileStackScreen} />
      <Tab.Screen name="History" component={HistoryStackScreen} />
      <Tab.Screen name="Workouts" component={WorkoutsStackScreen} />
      <Tab.Screen name="Exercises" component={ExerciseScreen} />
    </Tab.Navigator>
  );
}

export default function App() {
  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => {
      setSignedIn(!!data.session);
    });

    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      setSignedIn(!!session);
    });

    return () => sub.subscription.unsubscribe();
  }, []);

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <NavigationContainer>
          {signedIn ? <Tabs /> : <LoginScreen />}
        </NavigationContainer>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
