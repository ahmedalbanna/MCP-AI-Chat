import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useTheme } from '../theme/ThemeContext';
import { Message, ToolCall } from '../types';
import { ToolCallCard } from './ToolCallCard';

interface Props {
  message: Message;
}

export function ChatMessage({ message }: Props) {
  const { colors } = useTheme();
  const isUser = message.role === 'user';

  return (
    <View style={[styles.container, isUser ? styles.userRow : styles.assistantRow]}>
      <View
        style={[
          styles.bubble,
          isUser
            ? { backgroundColor: colors.primary }
            : { backgroundColor: colors.surface },
        ]}
      >
        {!isUser && message.toolCalls && message.toolCalls.length > 0 && (
          <View style={styles.toolCallsSection}>
            {message.toolCalls.map((tc) => (
              <ToolCallCard key={tc.id} toolCall={tc} />
            ))}
          </View>
        )}
        <Text
          style={[
            styles.text,
            isUser
              ? { color: colors.textInverse }
              : { color: colors.textPrimary },
          ]}
        >
          {message.content}
        </Text>
        <Text
          style={[
            styles.time,
            { color: isUser ? 'rgba(255,255,255,0.6)' : colors.textSecondary },
          ]}
        >
          {new Date(message.timestamp).toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
          })}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingVertical: 4,
  },
  userRow: {
    alignItems: 'flex-end',
  },
  assistantRow: {
    alignItems: 'flex-start',
  },
  bubble: {
    maxWidth: '85%',
    borderRadius: 16,
    padding: 12,
  },
  text: {
    fontSize: 15,
    lineHeight: 22,
  },
  time: {
    fontSize: 11,
    marginTop: 4,
  },
  toolCallsSection: {
    marginBottom: 8,
  },
});
