import { create } from 'zustand';
import { ChatState, Message, ContentBlock, ToolCall } from '../types';
import { sendMessage as apiSendMessage, WafChallengeError } from '../services/anthropic';
import { useSettingsStore } from './settings-store';
import { useMcpStore } from './mcp-store';

let msgId = 0;

function genId(): string {
  return `msg_${Date.now()}_${++msgId}`;
}

function buildApiMessages(
  messages: Message[]
): Array<{ role: string; content: string | ContentBlock[] }> {
  return messages.map((m) => ({
    role: m.role,
    content: m.content,
  }));
}

function extractContentBlocks(blocks: ContentBlock[]): {
  text: string;
  toolCalls: ToolCall[];
} {
  let text = '';
  const toolCalls: ToolCall[] = [];

  for (const block of blocks) {
    if (block.type === 'text' && block.text) {
      text += block.text;
    }
    if (block.type === 'mcp_tool_use' && block.id && block.name) {
      toolCalls.push({
        id: block.id,
        name: block.name,
        serverName: block.server_name ?? '',
        input: block.input ?? {},
      });
    }
  }

  return { text, toolCalls };
}

interface ChatExtendedState extends ChatState {
  wafPending: boolean;
  wafUrl: string;
  pendingMessage: string | null;
  setWafPending: (pending: boolean, url?: string) => void;
  retryAfterWaf: (cookies: Record<string, string>) => Promise<void>;
}

export const useChatStore = create<ChatExtendedState>((set, get) => ({
  messages: [],
  isLoading: false,
  error: null,
  wafPending: false,
  wafUrl: '',
  pendingMessage: null,

  setWafPending: (pending: boolean, url?: string) => {
    set({ wafPending: pending, wafUrl: url ?? '' });
  },

  retryAfterWaf: async (cookies: Record<string, string>) => {
    const { pendingMessage } = get();
    set({ wafPending: false, wafUrl: '', isLoading: true, error: null });

    try {
      const { apiKey, baseUrl, model } = useSettingsStore.getState();
      const allMessages = get().messages;
      const apiMessages = buildApiMessages(allMessages);
      const servers = useMcpStore.getState().servers;

      const mcpServers = servers.map((s) => ({
        id: s.id,
        url: s.url,
        token: s.headers?.Authorization?.replace('Bearer ', ''),
        enabled: s.enabled,
      }));

      const blocks = await apiSendMessage(apiKey, baseUrl, model, apiMessages, mcpServers, cookies);
      const { text, toolCalls } = extractContentBlocks(blocks);

      const assistantMsg: Message = {
        id: genId(),
        role: 'assistant',
        content: text || (toolCalls.length > 0 ? 'Called tools to process your request.' : ''),
        toolCalls: toolCalls.length > 0 ? toolCalls : undefined,
        timestamp: Date.now(),
      };

      set((s) => ({
        messages: [...s.messages, assistantMsg],
        isLoading: false,
        pendingMessage: null,
      }));
    } catch (err) {
      if (err instanceof WafChallengeError) {
        set({ isLoading: false, error: 'Verification expired. Please verify again.' });
      } else {
        set({
          isLoading: false,
          error: err instanceof Error ? err.message : 'Unknown error',
        });
      }
    }
  },

  sendMessage: async (content: string) => {
    const { apiKey, baseUrl, model } = useSettingsStore.getState();
    if (!apiKey) {
      set({ error: 'Please set your API key in Settings' });
      return;
    }

    const userMsg: Message = {
      id: genId(),
      role: 'user',
      content,
      timestamp: Date.now(),
    };

    set((s) => ({
      messages: [...s.messages, userMsg],
      isLoading: true,
      error: null,
    }));

    try {
      const allMessages = get().messages;
      const apiMessages = buildApiMessages(allMessages);
      const servers = useMcpStore.getState().servers;

      const mcpServers = servers.map((s) => ({
        id: s.id,
        url: s.url,
        token: s.headers?.Authorization?.replace('Bearer ', ''),
        enabled: s.enabled,
      }));

      const blocks = await apiSendMessage(apiKey, baseUrl, model, apiMessages, mcpServers);
      const { text, toolCalls } = extractContentBlocks(blocks);

      const assistantMsg: Message = {
        id: genId(),
        role: 'assistant',
        content: text || (toolCalls.length > 0 ? 'Called tools to process your request.' : ''),
        toolCalls: toolCalls.length > 0 ? toolCalls : undefined,
        timestamp: Date.now(),
      };

      set((s) => ({
        messages: [...s.messages, assistantMsg],
        isLoading: false,
      }));
    } catch (err) {
      if (err instanceof WafChallengeError) {
        set({
          isLoading: false,
          wafPending: true,
          wafUrl: err.url,
          pendingMessage: content,
        });
      } else {
        set({
          isLoading: false,
          error: err instanceof Error ? err.message : 'Unknown error',
        });
      }
    }
  },

  clearMessages: () => set({ messages: [], error: null }),
}));
