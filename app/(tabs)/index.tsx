import React, { useState, useRef, useEffect } from 'react';
import { View, FlatList, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
import { useTheme } from '../../src/theme/ThemeContext';
import { useChatStore } from '../../src/stores/chat-store';
import { ChatMessage } from '../../src/components/ChatMessage';
import { InputBar } from '../../src/components/InputBar';
import { LoadingIndicator } from '../../src/components/LoadingIndicator';
import { EmptyState } from '../../src/components/EmptyState';
import { WafChallenge } from '../../src/components/WafChallenge';
import { getWafCookies, parseCookieString } from '../../src/services/waf';

export default function ChatScreen() {
  const { colors } = useTheme();
  const { messages, isLoading, wafPending, wafUrl, sendMessage, retryAfterWaf, setWafPending } =
    useChatStore();
  const [input, setInput] = useState('');
  const flatListRef = useRef<FlatList>(null);
  const [savedCookies, setSavedCookies] = useState<Record<string, string>>({});

  useEffect(() => {
    (async () => {
      const stored = await getWafCookies();
      if (stored) {
        setSavedCookies(parseCookieString(stored.cookies));
      }
    })();
  }, []);

  useEffect(() => {
    if (messages.length > 0) {
      setTimeout(() => {
        flatListRef.current?.scrollToEnd({ animated: true });
      }, 100);
    }
  }, [messages.length, isLoading]);

  const handleSend = () => {
    const text = input.trim();
    if (!text || isLoading) return;
    setInput('');
    sendMessage(text);
  };

  const handleWafCookiesReady = async (cookies: Record<string, string>) => {
    setSavedCookies(cookies);
    setWafPending(false);
    await retryAfterWaf(cookies);
  };

  return (
    <KeyboardAvoidingView
      style={[styles.container, { backgroundColor: colors.background }]}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={0}
    >
      {messages.length === 0 && !isLoading ? (
        <EmptyState />
      ) : (
        <FlatList
          ref={flatListRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <ChatMessage message={item} />}
          contentContainerStyle={styles.list}
          onContentSizeChange={() =>
            flatListRef.current?.scrollToEnd({ animated: true })
          }
        />
      )}
      {isLoading && <LoadingIndicator />}
      <InputBar
        value={input}
        onChangeText={setInput}
        onSend={handleSend}
        disabled={isLoading}
      />

      <WafChallenge
        visible={wafPending}
        url={wafUrl || 'https://agentrouter.org/v1/messages'}
        onCookiesReady={handleWafCookiesReady}
        onDismiss={() => setWafPending(false)}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  list: {
    paddingVertical: 12,
    flexGrow: 1,
    justifyContent: 'flex-end',
  },
});
