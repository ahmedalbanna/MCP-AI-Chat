import React from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { useTheme } from '../../src/theme/ThemeContext';
import { useMcpStore } from '../../src/stores/mcp-store';
import { ServerCard } from '../../src/components/ServerCard';

export default function InspectorScreen() {
  const { colors } = useTheme();
  const servers = useMcpStore((s) => s.servers);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>MCP Servers</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
          Browse connected servers and their tools
        </Text>
      </View>
      <FlatList
        data={servers}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => <ServerCard server={item} />}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
              No servers configured
            </Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomWidth: 1,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 13,
    marginTop: 4,
  },
  list: {
    paddingVertical: 12,
  },
  empty: {
    alignItems: 'center',
    paddingVertical: 48,
  },
  emptyText: {
    fontSize: 14,
  },
});
