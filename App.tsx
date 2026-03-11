import React, { useEffect, useState } from "react";
import "./global.css";
import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { supabase } from "./src/lib/supabase";
import { COLORS } from "./src/theme/colors";

import WorkoutScreen from "./src/screens/WorkoutScreen";
import ExerciseScreen from "./src/screens/ExerciseScreen";
import AnalyticsScreen from "./src/screens/AnalyticsScreen";
import LoginScreen from "./src/screens/LoginScreen";
import HistoryScreen from "./src/screens/HistoryScreen";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import WorkoutDetailScreen from "./src/screens/WorkoutDetailsScreen";
import ChooseExerciseScreen from "./src/screens/ChooseExerciseScreen";
import { GestureHandlerRootView } from "react-native-gesture-handler";

const Tab = createBottomTabNavigator();
const WorkoutsStack = createNativeStackNavigator();
const HistoryStack = createNativeStackNavigator();

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
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: COLORS.prussianBlue,
          borderTopColor: COLORS.alabaster,
        },
        tabBarActiveTintColor: COLORS.orange,
        tabBarInactiveTintColor: COLORS.alabaster,
      }}
    >
      <Tab.Screen name="Workouts" component={WorkoutsStackScreen} />
      <Tab.Screen name="History" component={HistoryStackScreen} />
      <Tab.Screen name="Exercises" component={ExerciseScreen} />
      <Tab.Screen name="Analytics" component={AnalyticsScreen} />
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
