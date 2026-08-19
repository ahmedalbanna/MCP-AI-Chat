import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useTheme } from '../../src/theme/ThemeContext';
import { useSettingsStore } from '../../src/stores/settings-store';
import { useMcpStore } from '../../src/stores/mcp-store';

const MODELS = [
  'claude-opus-4-8',
  'claude-sonnet-4-20250514',
  'claude-opus-4-20250514',
  'claude-haiku-4-20250514',
];

export default function SettingsScreen() {
  const { colors, mode, setMode } = useTheme();
  const { apiKey, baseUrl, model, setApiKey, setBaseUrl, setModel } = useSettingsStore();

  const [keyInput, setKeyInput] = useState(apiKey);
  const [urlInput, setUrlInput] = useState(baseUrl);
  const [customModel, setCustomModel] = useState('');
  const [serverName, setServerName] = useState('');
  const [serverUrl, setServerUrl] = useState('');

  const handleSaveKey = () => {
    setApiKey(keyInput.trim());
    Alert.alert('Saved', 'API key saved securely.');
  };

  const handleSaveBaseUrl = () => {
    setBaseUrl(urlInput.trim());
    Alert.alert('Saved', 'Base URL updated.');
  };

  const handleAddServer = () => {
    if (!serverUrl.trim() || !serverName.trim()) {
      Alert.alert('Error', 'Enter both name and URL.');
      return;
    }
    const id = serverName.trim().toLowerCase().replace(/\s+/g, '-');
    useMcpStore.setState((s) => ({
      servers: [
        ...s.servers,
        {
          id,
          name: serverName.trim(),
          url: serverUrl.trim(),
          type: 'remote' as const,
          enabled: false,
        },
      ],
    }));
    setServerUrl('');
    setServerName('');
    Alert.alert('Added', `Server "${serverName.trim()}" added.`);
  };

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.content}
    >
      {/* Theme */}
      <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Appearance</Text>
      <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        {(['light', 'dark', 'system'] as const).map((opt) => (
          <TouchableOpacity
            key={opt}
            onPress={() => setMode(opt)}
            style={[styles.row, { borderBottomColor: colors.borderLight }]}
          >
            <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
              {opt.charAt(0).toUpperCase() + opt.slice(1)}
            </Text>
            {mode === opt && <Text style={{ color: colors.primary, fontWeight: '700' }}>&#10003;</Text>}
          </TouchableOpacity>
        ))}
      </View>

      {/* Base URL */}
      <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>API Base URL</Text>
      <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <TextInput
          style={[styles.input, { color: colors.textPrimary, borderColor: colors.border }]}
          value={urlInput}
          onChangeText={setUrlInput}
          placeholder="https://api.anthropic.com"
          placeholderTextColor={colors.textSecondary}
          autoCapitalize="none"
          keyboardType="url"
        />
        <TouchableOpacity
          onPress={handleSaveBaseUrl}
          style={[styles.saveBtn, { backgroundColor: colors.primary }]}
        >
          <Text style={styles.saveBtnText}>Save URL</Text>
        </TouchableOpacity>
      </View>

      {/* API Key */}
      <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>API Key</Text>
      <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <TextInput
          style={[styles.input, { color: colors.textPrimary, borderColor: colors.border }]}
          value={keyInput}
          onChangeText={setKeyInput}
          placeholder="sk-ant-..."
          placeholderTextColor={colors.textSecondary}
          secureTextEntry
          autoCapitalize="none"
        />
        <TouchableOpacity
          onPress={handleSaveKey}
          style={[styles.saveBtn, { backgroundColor: colors.primary }]}
        >
          <Text style={styles.saveBtnText}>Save Key</Text>
        </TouchableOpacity>
      </View>

      {/* Model */}
      <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Model</Text>
      <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        {MODELS.map((m) => (
          <TouchableOpacity
            key={m}
            onPress={() => setModel(m)}
            style={[styles.row, { borderBottomColor: colors.borderLight }]}
          >
            <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>{m}</Text>
            {model === m && <Text style={{ color: colors.primary, fontWeight: '700' }}>&#10003;</Text>}
          </TouchableOpacity>
        ))}
        <View style={styles.customRow}>
          <TextInput
            style={[styles.input, { color: colors.textPrimary, borderColor: colors.border, marginTop: 0, flex: 1 }]}
            value={customModel}
            onChangeText={setCustomModel}
            placeholder="Custom model name"
            placeholderTextColor={colors.textSecondary}
            autoCapitalize="none"
          />
          <TouchableOpacity
            onPress={() => {
              if (customModel.trim()) {
                setModel(customModel.trim());
                setCustomModel('');
              }
            }}
            style={[styles.smallBtn, { backgroundColor: colors.primary }]}
          >
            <Text style={styles.saveBtnText}>Set</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Custom Server */}
      <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Add Custom Server</Text>
      <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <TextInput
          style={[styles.input, { color: colors.textPrimary, borderColor: colors.border }]}
          value={serverName}
          onChangeText={setServerName}
          placeholder="Server name"
          placeholderTextColor={colors.textSecondary}
        />
        <TextInput
          style={[styles.input, { color: colors.textPrimary, borderColor: colors.border }]}
          value={serverUrl}
          onChangeText={setServerUrl}
          placeholder="https://example.com/mcp"
          placeholderTextColor={colors.textSecondary}
          autoCapitalize="none"
        />
        <TouchableOpacity
          onPress={handleAddServer}
          style={[styles.saveBtn, { backgroundColor: colors.primary }]}
        >
          <Text style={styles.saveBtnText}>Add Server</Text>
        </TouchableOpacity>
      </View>

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16 },
  sectionTitle: {
    fontSize: 13,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginTop: 20,
    marginBottom: 8,
  },
  card: {
    borderRadius: 12,
    borderWidth: 1,
    overflow: 'hidden',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 14,
    borderBottomWidth: 1,
  },
  rowLabel: { fontSize: 15 },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    marginHorizontal: 12,
    marginTop: 12,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
  },
  saveBtn: {
    margin: 12,
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
  },
  saveBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
  },
  customRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingBottom: 12,
    gap: 8,
  },
  smallBtn: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 10,
  },
});
