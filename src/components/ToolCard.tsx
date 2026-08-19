import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useTheme } from '../theme/ThemeContext';
import { MCPTool } from '../types';

interface Props {
  tool: MCPTool;
}

export function ToolCard({ tool }: Props) {
  const { colors } = useTheme();
  const [expanded, setExpanded] = useState(false);

  const requiredFields = (tool.inputSchema as any)?.required ?? [];
  const properties = (tool.inputSchema as any)?.properties ?? {};

  return (
    <TouchableOpacity
      onPress={() => setExpanded(!expanded)}
      activeOpacity={0.7}
      style={[styles.card, { backgroundColor: colors.surfaceHover, borderColor: colors.border }]}
    >
      <View style={styles.header}>
        <Text style={[styles.name, { color: colors.primary }]}>{tool.name}</Text>
      </View>
      <Text style={[styles.desc, { color: colors.textSecondary }]} numberOfLines={expanded ? undefined : 2}>
        {tool.description}
      </Text>
      {expanded && Object.keys(properties).length > 0 && (
        <View style={styles.schema}>
          <Text style={[styles.schemaTitle, { color: colors.textSecondary }]}>Parameters:</Text>
          {Object.entries(properties).map(([key, val]: [string, any]) => (
            <View key={key} style={styles.param}>
              <Text style={[styles.paramName, { color: colors.textPrimary }]}>{key}</Text>
              <Text style={[styles.paramType, { color: colors.textSecondary }]}>{val.type ?? 'any'}</Text>
              {requiredFields.includes(key) && (
                <Text style={[styles.required, { color: colors.error }]}>required</Text>
              )}
            </View>
          ))}
        </View>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 8,
    borderWidth: 1,
    padding: 10,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  name: {
    fontSize: 13,
    fontWeight: '600',
    fontFamily: 'monospace',
  },
  desc: {
    fontSize: 12,
    marginTop: 4,
    lineHeight: 17,
  },
  schema: {
    marginTop: 8,
    gap: 4,
  },
  schemaTitle: {
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 2,
  },
  param: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 2,
  },
  paramName: {
    fontSize: 12,
    fontFamily: 'monospace',
    fontWeight: '500',
  },
  paramType: {
    fontSize: 11,
    fontFamily: 'monospace',
  },
  required: {
    fontSize: 10,
    fontWeight: '600',
  },
});
