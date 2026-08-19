import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useTheme } from '../theme/ThemeContext';
import { MCPServerConfig } from '../types';
import { useMcpStore } from '../stores/mcp-store';
import { inspectServer } from '../services/mcp-http';
import { ToolCard } from './ToolCard';

interface Props {
  server: MCPServerConfig;
}

export function ServerCard({ server }: Props) {
  const { colors } = useTheme();
  const { toggleServer, setServerTools, setServerConnected } = useMcpStore();
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRefresh = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await inspectServer(server.url, server.headers);
      setServerTools(server.id, result.tools);
      setServerConnected(server.id, true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Connection failed');
      setServerConnected(server.id, false);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
      <TouchableOpacity
        onPress={() => setExpanded(!expanded)}
        activeOpacity={0.7}
        style={styles.header}
      >
        <View style={styles.headerLeft}>
          <View
            style={[
              styles.statusDot,
              { backgroundColor: server.connected ? colors.success : colors.textSecondary },
            ]}
          />
          <View style={styles.headerText}>
            <Text style={[styles.name, { color: colors.textPrimary }]}>{server.name}</Text>
            <Text style={[styles.url, { color: colors.textSecondary }]} numberOfLines={1}>
              {server.url}
            </Text>
          </View>
        </View>
        <TouchableOpacity
          onPress={() => toggleServer(server.id)}
          style={[
            styles.toggle,
            {
              backgroundColor: server.enabled ? colors.primary : colors.border,
            },
          ]}
        >
          <Text style={[styles.toggleText, { color: server.enabled ? '#FFF' : colors.textSecondary }]}>
            {server.enabled ? 'ON' : 'OFF'}
          </Text>
        </TouchableOpacity>
      </TouchableOpacity>

      {expanded && (
        <View style={styles.body}>
          <View style={styles.actions}>
            <TouchableOpacity
              onPress={handleRefresh}
              disabled={loading}
              style={[styles.btn, { backgroundColor: colors.primaryLight }]}
            >
              <Text style={[styles.btnText, { color: colors.primary }]}>
                {loading ? 'Connecting...' : 'Fetch Tools'}
              </Text>
            </TouchableOpacity>
          </View>

          {error && (
            <Text style={[styles.error, { color: colors.error }]}>{error}</Text>
          )}

          {server.tools && server.tools.length > 0 && (
            <View style={styles.toolsSection}>
              <Text style={[styles.toolsTitle, { color: colors.textSecondary }]}>
                {server.tools.length} tools available
              </Text>
              {server.tools.map((tool) => (
                <ToolCard key={tool.name} tool={tool} />
              ))}
            </View>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 12,
    borderWidth: 1,
    marginHorizontal: 16,
    marginVertical: 6,
    overflow: 'hidden',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 14,
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    gap: 10,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  headerText: {
    flex: 1,
  },
  name: {
    fontSize: 15,
    fontWeight: '600',
  },
  url: {
    fontSize: 12,
    marginTop: 2,
  },
  toggle: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
  },
  toggleText: {
    fontSize: 12,
    fontWeight: '700',
  },
  body: {
    padding: 14,
    paddingTop: 0,
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 10,
  },
  btn: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
  },
  btnText: {
    fontSize: 13,
    fontWeight: '600',
  },
  error: {
    fontSize: 13,
    marginBottom: 8,
  },
  toolsSection: {
    gap: 6,
  },
  toolsTitle: {
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 4,
  },
});
