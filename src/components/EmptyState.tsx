import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '../theme/ThemeContext';

export function EmptyState() {
  const { colors } = useTheme();

  return (
    <View style={styles.container}>
      <View style={[styles.iconCircle, { backgroundColor: colors.primaryLight }]}>
        <Text style={[styles.icon, { color: colors.primary }]}>M</Text>
      </View>
      <Text style={[styles.title, { color: colors.textPrimary }]}>MCP AI Chat</Text>
      <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
        Chat with Claude using Model Context Protocol tools.
      </Text>
      <Text style={[styles.hint, { color: colors.textSecondary }]}>
        Enable servers in the Inspector tab, then ask questions that use their tools.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 32,
    gap: 10,
  },
  iconCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  icon: {
    fontSize: 28,
    fontWeight: '800',
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
  },
  hint: {
    fontSize: 13,
    textAlign: 'center',
    lineHeight: 18,
    marginTop: 4,
  },
});
