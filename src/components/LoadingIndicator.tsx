import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '../theme/ThemeContext';

export function LoadingIndicator() {
  const { colors } = useTheme();

  return (
    <View style={styles.container}>
      <View style={[styles.dot, { backgroundColor: colors.primary }]} />
      <View style={[styles.dot, { backgroundColor: colors.primary, opacity: 0.6 }]} />
      <View style={[styles.dot, { backgroundColor: colors.primary, opacity: 0.3 }]} />
      <Text style={[styles.label, { color: colors.textSecondary }]}>Thinking...</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
    gap: 5,
  },
  dot: {
    width: 7,
    height: 7,
    borderRadius: 4,
  },
  label: {
    fontSize: 13,
    marginLeft: 6,
  },
});
