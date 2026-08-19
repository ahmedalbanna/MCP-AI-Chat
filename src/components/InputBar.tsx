import React from 'react';
import { View, TextInput, StyleSheet, TouchableOpacity, Text } from 'react-native';
import { useTheme } from '../theme/ThemeContext';

interface Props {
  value: string;
  onChangeText: (text: string) => void;
  onSend: () => void;
  disabled: boolean;
}

export function InputBar({ value, onChangeText, onSend, disabled }: Props) {
  const { colors } = useTheme();
  const canSend = value.trim().length > 0 && !disabled;

  return (
    <View style={[styles.container, { backgroundColor: colors.background, borderTopColor: colors.border }]}>
      <TextInput
        style={[
          styles.input,
          {
            backgroundColor: colors.surface,
            color: colors.textPrimary,
            borderColor: colors.border,
          },
        ]}
        value={value}
        onChangeText={onChangeText}
        placeholder="Type a message..."
        placeholderTextColor={colors.textSecondary}
        multiline
        maxLength={4000}
        editable={!disabled}
      />
      <TouchableOpacity
        onPress={onSend}
        disabled={!canSend}
        style={[
          styles.sendBtn,
          {
            backgroundColor: canSend ? colors.primary : colors.border,
          },
        ]}
      >
        <Text style={[styles.sendText, { color: canSend ? '#FFFFFF' : colors.textSecondary }]}>
          Send
        </Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderTopWidth: 1,
    gap: 8,
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 15,
    maxHeight: 120,
  },
  sendBtn: {
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: 12,
  },
  sendText: {
    fontSize: 15,
    fontWeight: '600',
  },
});
