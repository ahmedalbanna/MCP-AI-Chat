import React from 'react';
import { Text } from 'react-native';
import { Tabs } from 'expo-router';
import { useTheme } from '../../src/theme/ThemeContext';

function TabIcon({ label, color }: { label: string; color: any }) {
  return <Text style={{ color, fontSize: 16, fontWeight: '700' }}>{label}</Text>;
}

export default function TabLayout() {
  const { colors } = useTheme();

  return (
    <Tabs
      screenOptions={{
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.textPrimary,
        headerTitleStyle: { fontWeight: '600' },
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
        },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSecondary,
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Chat',
          tabBarLabel: 'Chat',
          tabBarIcon: ({ color }) => <TabIcon label="C" color={color} />,
        }}
      />
      <Tabs.Screen
        name="inspector"
        options={{
          title: 'Inspector',
          tabBarLabel: 'Inspector',
          tabBarIcon: ({ color }) => <TabIcon label="I" color={color} />,
        }}
      />
      <Tabs.Screen
        name="settings"
        options={{
          title: 'Settings',
          tabBarLabel: 'Settings',
          tabBarIcon: ({ color }) => <TabIcon label="S" color={color} />,
        }}
      />
    </Tabs>
  );
}
