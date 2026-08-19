/**
 * E2E Integration Test — Full App Flow
 *
 * Emulates: launch → configure → chat → WAF challenge → retry → tool call → inspector
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';

// ── Mock fetch globally ──
const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

// ── Mock expo-secure-store ──
const secureStore: Record<string, string> = {};
vi.mock('expo-secure-store', () => ({
  setItemAsync: vi.fn(async (key: string, value: string) => { secureStore[key] = value; }),
  getItemAsync: vi.fn(async (key: string) => secureStore[key] ?? null),
  deleteItemAsync: vi.fn(async (key: string) => { delete secureStore[key]; }),
}));

// ── Mock react-native ──
vi.mock('react-native', () => ({
  useColorScheme: () => 'light',
  Platform: { OS: 'web', select: (obj: any) => obj.web },
  StyleSheet: { create: (s: any) => s, flatten: (s: any) => s },
}));

vi.mock('react-native-webview', () => ({ WebView: () => null }));

// ── Now import modules that depend on mocks ──
import { useSettingsStore, loadSettings } from '../stores/settings-store';
import { useMcpStore } from '../stores/mcp-store';
import { useChatStore } from '../stores/chat-store';
import {
  saveWafCookies,
  getWafCookies,
  clearWafCookies,
  isWafResponse,
  buildCookieHeader,
  parseCookieString,
} from '../services/waf';

// ── Helpers ──
function okJson(data: any) {
  return { ok: true, status: 200, text: () => Promise.resolve(JSON.stringify(data)) };
}

function wafPage() {
  return {
    ok: true,
    status: 200,
    text: () =>
      Promise.resolve(
        '<!doctype html><meta name="aliyun_waf_aa" content="abc123"><title>Access Verification</title>'
      ),
  };
}

// ─────────────────────────────────────────────────────────
// TEST SUITE
// ─────────────────────────────────────────────────────────

describe('Full App Lifecycle — E2E', () => {
  beforeEach(() => {
    Object.keys(secureStore).forEach((k) => delete secureStore[k]);
    vi.clearAllMocks();

    useSettingsStore.setState({
      apiKey: 'sk-test-key-123',
      baseUrl: 'https://agentrouter.org',
      model: 'claude-opus-4-8',
      themeMode: 'system',
    });

    useMcpStore.setState({
      servers: [
        {
          id: 'erpnext-gateway',
          name: 'ERPNext MCP Gateway',
          url: 'http://185.190.140.93:8022/mcp',
          type: 'remote',
          enabled: false,
          headers: { Authorization: 'Bearer change-me-admin-token' },
        },
      ],
    });

    useChatStore.setState({
      messages: [],
      isLoading: false,
      error: null,
      wafPending: false,
      wafUrl: '',
      pendingMessage: null,
    });
  });

  // ── STEP 1: Boot ──
  it('1. App boots with correct defaults', () => {
    const { apiKey, baseUrl, model } = useSettingsStore.getState();
    expect(apiKey).toBe('sk-test-key-123');
    expect(baseUrl).toBe('https://agentrouter.org');
    expect(model).toBe('claude-opus-4-8');
  });

  // ── STEP 2: Settings ──
  it('2. User updates settings and they persist', () => {
    useSettingsStore.getState().setApiKey('sk-new');
    useSettingsStore.getState().setBaseUrl('https://other-proxy.com');
    useSettingsStore.getState().setModel('claude-3-haiku');

    expect(useSettingsStore.getState().apiKey).toBe('sk-new');
    expect(useSettingsStore.getState().baseUrl).toBe('https://other-proxy.com');
    expect(useSettingsStore.getState().model).toBe('claude-3-haiku');

    expect(secureStore['anthropic_api_key']).toBe('sk-new');
    expect(secureStore['anthropic_base_url']).toBe('https://other-proxy.com');
    expect(secureStore['selected_model']).toBe('claude-3-haiku');
  });

  // ── STEP 3: Enable MCP server ──
  it('3. User toggles MCP server on', () => {
    useMcpStore.getState().toggleServer('erpnext-gateway');
    const srv = useMcpStore.getState().servers.find((s) => s.id === 'erpnext-gateway');
    expect(srv?.enabled).toBe(true);
  });

  // ── STEP 4: Simple chat ──
  it('4. User sends message, gets text response', async () => {
    mockFetch.mockResolvedValue(
      okJson({ content: [{ type: 'text', text: 'Sunny and 25C.' }] })
    );

    await useChatStore.getState().sendMessage('What is the weather?');

    const { messages, isLoading, error } = useChatStore.getState();
    expect(isLoading).toBe(false);
    expect(error).toBeNull();
    expect(messages).toHaveLength(2);
    expect(messages[0]).toMatchObject({ role: 'user', content: 'What is the weather?' });
    expect(messages[1]).toMatchObject({ role: 'assistant', content: 'Sunny and 25C.' });

    const body = JSON.parse(mockFetch.mock.calls[0][1].body);
    expect(body.mcp_servers).toBeUndefined();
  });

  // ── STEP 5: Multi-turn context ──
  it('5. Conversation builds context across turns', async () => {
    mockFetch
      .mockResolvedValueOnce(okJson({ content: [{ type: 'text', text: 'Tokyo.' }] }))
      .mockResolvedValueOnce(okJson({ content: [{ type: 'text', text: '~14M people.' }] }));

    await useChatStore.getState().sendMessage('Capital of Japan?');
    await useChatStore.getState().sendMessage('Population?');

    expect(useChatStore.getState().messages).toHaveLength(4);

    const secondBody = JSON.parse(mockFetch.mock.calls[1][1].body);
    expect(secondBody.messages).toHaveLength(3);
  });

  // ── STEP 6: WAF challenge + retry ──
  it('6. WAF blocks → cookie saved → retry succeeds', async () => {
    mockFetch
      .mockResolvedValueOnce(wafPage())
      .mockResolvedValueOnce(okJson({ content: [{ type: 'text', text: 'After WAF.' }] }));

    await useChatStore.getState().sendMessage('Hello via WAF');

    expect(useChatStore.getState().wafPending).toBe(true);
    expect(useChatStore.getState().pendingMessage).toBe('Hello via WAF');

    await useChatStore.getState().retryAfterWaf({ aliyun_waf_aa: 'valid_token' });

    const s = useChatStore.getState();
    expect(s.wafPending).toBe(false);
    expect(s.messages).toHaveLength(2);
    expect(s.messages[1].content).toBe('After WAF.');

    const retryHeaders = mockFetch.mock.calls[1][1].headers;
    expect(retryHeaders['Cookie']).toBe('aliyun_waf_aa=valid_token');
  });

  // ── STEP 7: Cookie persistence ──
  it('7. WAF cookies persist and parse correctly', async () => {
    await saveWafCookies('session=abc; csrf=xyz', 'agentrouter.org');
    const stored = await getWafCookies();
    expect(stored).toEqual({ cookies: 'session=abc; csrf=xyz', domain: 'agentrouter.org' });

    const parsed = parseCookieString(stored!.cookies);
    expect(parsed).toEqual({ session: 'abc', csrf: 'xyz' });

    const header = buildCookieHeader(parsed);
    expect(header).toBe('session=abc; csrf=xyz');

    await clearWafCookies();
    expect(await getWafCookies()).toBeNull();
  });

  // ── STEP 8: MCP tool call ──
  it('8. MCP enabled → tool call in response', async () => {
    useMcpStore.getState().toggleServer('erpnext-gateway');

    mockFetch.mockResolvedValue(
      okJson({
        content: [
          { type: 'text', text: 'Fetching user.' },
          {
            type: 'mcp_tool_use',
            id: 'toolu_01',
            name: 'get_doc',
            server_name: 'erpnext-gateway',
            input: { doctype: 'User', name: 'Administrator' },
          },
        ],
      })
    );

    await useChatStore.getState().sendMessage('Get Administrator user');

    const assistant = useChatStore.getState().messages[1];
    expect(assistant.toolCalls).toHaveLength(1);
    expect(assistant.toolCalls![0]).toMatchObject({
      id: 'toolu_01',
      name: 'get_doc',
      serverName: 'erpnext-gateway',
    });

    const body = JSON.parse(mockFetch.mock.calls[0][1].body);
    expect(body.mcp_servers[0].name).toBe('erpnext-gateway');
    expect(body.tools[0].mcp_server_name).toBe('erpnext-gateway');
  });

  // ── STEP 9: API error ──
  it('9. API error shown to user', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 429,
      text: () => Promise.resolve(JSON.stringify({ error: { message: 'Rate limited' } })),
    });

    await useChatStore.getState().sendMessage('Trigger error');

    expect(useChatStore.getState().error).toBe('Rate limited');
    expect(useChatStore.getState().isLoading).toBe(false);
    expect(useChatStore.getState().messages).toHaveLength(1);
  });

  // ── STEP 10: WAF detection utility ──
  it('10. WAF HTML detection works', () => {
    expect(isWafResponse('<meta name="aliyun_waf" content="x">')).toBe(true);
    expect(isWafResponse('{"content":[]}')).toBe(false);
    expect(isWafResponse('')).toBe(false);
  });

  // ── STEP 11: Clear chat ──
  it('11. Clear chat resets everything', async () => {
    mockFetch.mockResolvedValue(okJson({ content: [{ type: 'text', text: 'hi' }] }));
    await useChatStore.getState().sendMessage('hello');
    expect(useChatStore.getState().messages).toHaveLength(2);

    useChatStore.getState().clearMessages();
    expect(useChatStore.getState().messages).toEqual([]);
    expect(useChatStore.getState().error).toBeNull();
  });

  // ── STEP 12: Missing API key ──
  it('12. Sending without API key shows error', async () => {
    useSettingsStore.getState().setApiKey('');
    await useChatStore.getState().sendMessage('test');
    expect(useChatStore.getState().error).toBe('Please set your API key in Settings');
  });
});
