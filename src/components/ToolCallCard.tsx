import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useTheme } from '../theme/ThemeContext';
import { ToolCall } from '../types';

interface Props {
  toolCall: ToolCall;
}

export function ToolCallCard({ toolCall }: Props) {
  const { colors } = useTheme();
  const [expanded, setExpanded] = useState(false);

  return (
    <TouchableOpacity
      onPress={() => setExpanded(!expanded)}
      activeOpacity={0.7}
      style={[styles.container, { backgroundColor: colors.primaryLight, borderColor: colors.border }]}
    >
      <View style={styles.header}>
        <View style={[styles.icon, { backgroundColor: colors.primary }]}>
          <Text style={styles.iconText}>T</Text>
        </View>
        <Text style={[styles.name, { color: colors.primary }]} numberOfLines={1}>
          {toolCall.name}
        </Text>
      </View>
      {expanded && (
        <View style={styles.details}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>Server:</Text>
          <Text style={[styles.value, { color: colors.textPrimary }]}>{toolCall.serverName}</Text>
          <Text style={[styles.label, { color: colors.textSecondary }]}>Input:</Text>
          <Text style={[styles.json, { color: colors.textPrimary }]}>
            {JSON.stringify(toolCall.input, null, 2)}
          </Text>
          {toolCall.result && (
            <>
              <Text style={[styles.label, { color: colors.textSecondary }]}>Result:</Text>
              <Text
                style={[
                  styles.json,
                  {
                    color: toolCall.result.isError ? colors.error : colors.textPrimary,
                  },
                ]}
              >
                {toolCall.result.content
                  .map((c) => c.text ?? '')
                  .join('\n')}
              </Text>
            </>
          )}
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    marginBottom: 6,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  icon: {
    width: 22,
    height: 22,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },
  name: {
    fontSize: 13,
    fontWeight: '600',
    flex: 1,
  },
  details: {
    marginTop: 8,
    gap: 4,
  },
  label: {
    fontSize: 11,
    fontWeight: '600',
    marginTop: 4,
  },
  value: {
    fontSize: 12,
  },
  json: {
    fontSize: 11,
    fontFamily: 'monospace',
  },
});
