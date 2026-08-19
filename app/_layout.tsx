import React, { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { ThemeProvider, useTheme } from '../src/theme/ThemeContext';
import { loadSettings } from '../src/stores/settings-store';

function RootLayoutInner() {
  const { isDark, colors } = useTheme();

  return (
    <>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.background },
        }}
      />
    </>
  );
}

export default function RootLayout() {
  useEffect(() => {
    loadSettings();
  }, []);

  return (
    <ThemeProvider>
      <RootLayoutInner />
    </ThemeProvider>
  );
}
