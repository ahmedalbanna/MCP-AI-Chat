/**
 * E2E Integration Test — Full App Flow
 *
 * Emulates: launch → configure → chat → WAF challenge → retry → tool call → inspector
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

const secureStore: Record<string, string> = {};
vi.mock('expo-secure-store', () => ({
  setItemAsync: vi.fn(async (key: string, value: string) => { secureStore[key] = value; }),
  getItemAsync: vi.fn(async (key: string) => secureStore[key] ?? null),
  deleteItemAsync: vi.fn(async (key: string) => { delete secureStore[key]; }),
}));

vi.mock('react-native', () => ({
  useColorScheme: () => 'light',
  Platform: { OS: 'web', select: (obj: any) => obj.web },
  StyleSheet: { create: (s: any) => s, flatten: (s: any) => s },
}));

vi.mock('react-native-webview', () => ({ WebView: () => null }));

import { useSettingsStore } from '../stores/settings-store';
import { useMcpStore } from '../stores/mcp-store';
import { useChatStore } from '../stores/chat-store';
import { inspectServer } from '../services/mcp-http';
import {
  saveWafCookies,
  getWafCookies,
  clearWafCookies,
  isWafResponse,
  buildCookieHeader,
  parseCookieString,
} from '../services/waf';

function okJson(data: any) {
  return { ok: true, status: 200, headers: new Map(), text: () => Promise.resolve(JSON.stringify(data)) };
}

function okSse(result: any) {
  const line = `data: ${JSON.stringify({ jsonrpc: '2.0', id: 1, result })}`;
  return { ok: true, status: 200, headers: new Map(), text: () => Promise.resolve(line) };
}

function wafPage() {
  return {
    ok: true, status: 200, headers: new Map(),
    text: () => Promise.resolve('<!doctype html><meta name="aliyun_waf_aa" content="abc123"><title>Access Verification</title>'),
  };
}

const MCP_URL = 'http://185.190.140.93:8022/mcp';
const MCP_AUTH = { Authorization: 'Bearer change-me-admin-token' };

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
      servers: [{
        id: 'erpnext-gateway',
        name: 'ERPNext MCP Gateway',
        url: MCP_URL,
        type: 'remote',
        enabled: false,
        headers: MCP_AUTH,
      }],
    });

    useChatStore.setState({
      messages: [], isLoading: false, error: null,
      wafPending: false, wafUrl: '', pendingMessage: null,
    });
  });

  it('1. App boots with correct defaults', () => {
    const { apiKey, baseUrl, model } = useSettingsStore.getState();
    expect(apiKey).toBe('sk-test-key-123');
    expect(baseUrl).toBe('https://agentrouter.org');
    expect(model).toBe('claude-opus-4-8');
  });

  it('2. User updates settings and they persist', () => {
    useSettingsStore.getState().setApiKey('sk-new');
    useSettingsStore.getState().setBaseUrl('https://other-proxy.com');
    useSettingsStore.getState().setModel('claude-3-haiku');

    expect(useSettingsStore.getState().apiKey).toBe('sk-new');
    expect(secureStore['anthropic_api_key']).toBe('sk-new');
    expect(secureStore['anthropic_base_url']).toBe('https://other-proxy.com');
    expect(secureStore['selected_model']).toBe('claude-3-haiku');
  });

  it('3. User toggles MCP server on', () => {
    useMcpStore.getState().toggleServer('erpnext-gateway');
    const srv = useMcpStore.getState().servers.find((s) => s.id === 'erpnext-gateway');
    expect(srv?.enabled).toBe(true);
  });

  it('4. User sends message, gets text response', async () => {
    mockFetch.mockResolvedValue(okJson({ content: [{ type: 'text', text: 'Sunny and 25C.' }] }));
    await useChatStore.getState().sendMessage('What is the weather?');

    const { messages, isLoading, error } = useChatStore.getState();
    expect(isLoading).toBe(false);
    expect(error).toBeNull();
    expect(messages).toHaveLength(2);
    expect(messages[1]).toMatchObject({ role: 'assistant', content: 'Sunny and 25C.' });
  });

  it('5. Conversation builds context across turns', async () => {
    mockFetch
      .mockResolvedValueOnce(okJson({ content: [{ type: 'text', text: 'Tokyo.' }] }))
      .mockResolvedValueOnce(okJson({ content: [{ type: 'text', text: '~14M people.' }] }));

    await useChatStore.getState().sendMessage('Capital of Japan?');
    await useChatStore.getState().sendMessage('Population?');

    expect(useChatStore.getState().messages).toHaveLength(4);
    const body = JSON.parse(mockFetch.mock.calls[1][1].body);
    expect(body.messages).toHaveLength(3);
  });

  it('6. WAF blocks, cookie saved, retry succeeds', async () => {
    mockFetch
      .mockResolvedValueOnce(wafPage())
      .mockResolvedValueOnce(okJson({ content: [{ type: 'text', text: 'After WAF.' }] }));

    await useChatStore.getState().sendMessage('Hello via WAF');
    expect(useChatStore.getState().wafPending).toBe(true);

    await useChatStore.getState().retryAfterWaf({ aliyun_waf_aa: 'valid_token' });

    const s = useChatStore.getState();
    expect(s.wafPending).toBe(false);
    expect(s.messages[1].content).toBe('After WAF.');
    expect(mockFetch.mock.calls[1][1].headers['Cookie']).toBe('aliyun_waf_aa=valid_token');
  });

  it('7. WAF cookies persist and parse correctly', async () => {
    await saveWafCookies('session=abc; csrf=xyz', 'agentrouter.org');
    const stored = await getWafCookies();
    expect(stored).toEqual({ cookies: 'session=abc; csrf=xyz', domain: 'agentrouter.org' });

    const parsed = parseCookieString(stored!.cookies);
    expect(buildCookieHeader(parsed)).toBe('session=abc; csrf=xyz');

    await clearWafCookies();
    expect(await getWafCookies()).toBeNull();
  });

  it('8. MCP enabled, tool call in response', async () => {
    useMcpStore.getState().toggleServer('erpnext-gateway');
    mockFetch.mockResolvedValue(okJson({
      content: [
        { type: 'text', text: 'Fetching user.' },
        { type: 'mcp_tool_use', id: 'toolu_01', name: 'get_doc', server_name: 'erpnext-gateway', input: { doctype: 'User' } },
      ],
    }));

    await useChatStore.getState().sendMessage('Get Administrator user');
    const assistant = useChatStore.getState().messages[1];
    expect(assistant.toolCalls).toHaveLength(1);
    expect(assistant.toolCalls![0].name).toBe('get_doc');

    const body = JSON.parse(mockFetch.mock.calls[0][1].body);
    expect(body.mcp_servers[0].name).toBe('erpnext-gateway');
    expect(body.tools[0].mcp_server_name).toBe('erpnext-gateway');
  });

  it('9. API error shown to user', async () => {
    mockFetch.mockResolvedValue({
      ok: false, status: 429, headers: new Map(),
      text: () => Promise.resolve(JSON.stringify({ error: { message: 'Rate limited' } })),
    });
    await useChatStore.getState().sendMessage('Trigger error');
    expect(useChatStore.getState().error).toBe('Rate limited');
  });

  it('10. WAF HTML detection works', () => {
    expect(isWafResponse('<meta name="aliyun_waf" content="x">')).toBe(true);
    expect(isWafResponse('{"content":[]}')).toBe(false);
    expect(isWafResponse('')).toBe(false);
  });

  it('11. Clear chat resets everything', async () => {
    mockFetch.mockResolvedValue(okJson({ content: [{ type: 'text', text: 'hi' }] }));
    await useChatStore.getState().sendMessage('hello');
    useChatStore.getState().clearMessages();
    expect(useChatStore.getState().messages).toEqual([]);
  });

  it('12. Sending without API key shows error', async () => {
    useSettingsStore.getState().setApiKey('');
    await useChatStore.getState().sendMessage('test');
    expect(useChatStore.getState().error).toBe('Please set your API key in Settings');
  });

  // ── MCP Inspector fetch validation ──

  it('13. MCP inspector: initialize then tools/list succeeds (SSE)', async () => {
    mockFetch
      .mockResolvedValueOnce(okSse({ protocolVersion: '2025-03-26', serverInfo: { name: 'erpnext-mcp-gateway', version: '3.4.7' }, capabilities: { tools: { listChanged: true } } }))
      .mockResolvedValueOnce(okSse({}))
      .mockResolvedValueOnce(okSse({ tools: [
        { name: 'get_doc', description: 'Get a document', inputSchema: { type: 'object', required: ['doctype'] } },
        { name: 'list_docs', description: 'List documents', inputSchema: { type: 'object' } },
      ] }));

    const result = await inspectServer(MCP_URL, MCP_AUTH);

    expect(result.serverName).toBe('erpnext-mcp-gateway');
    expect(result.tools).toHaveLength(2);
    expect(result.tools[0].name).toBe('get_doc');
    expect(result.tools[1].name).toBe('list_docs');

    expect(mockFetch).toHaveBeenCalledTimes(3);
    expect(JSON.parse(mockFetch.mock.calls[0][1].body).method).toBe('initialize');
    expect(JSON.parse(mockFetch.mock.calls[2][1].body).method).toBe('tools/list');
  });

  it('14. MCP inspector: auth header sent on every request', async () => {
    mockFetch
      .mockResolvedValueOnce(okSse({ serverInfo: { name: 't', version: '1' }, capabilities: {} }))
      .mockResolvedValueOnce(okSse({}))
      .mockResolvedValueOnce(okSse({ tools: [] }));

    await inspectServer(MCP_URL, MCP_AUTH);

    for (let i = 0; i < 3; i++) {
      expect(mockFetch.mock.calls[i][1].headers['Authorization']).toBe('Bearer change-me-admin-token');
    }
  });

  it('15. MCP inspector: HTML response throws clear error', async () => {
    mockFetch.mockResolvedValue(wafPage());
    await expect(inspectServer(MCP_URL, MCP_AUTH)).rejects.toThrow('HTML instead of JSON');
  });

  it('16. MCP inspector: 401 unauthorized throws error', async () => {
    mockFetch.mockResolvedValue({
      ok: false, status: 401, headers: new Map(),
      text: () => Promise.resolve('{"error":"unauthorized"}'),
    });
    await expect(inspectServer(MCP_URL, MCP_AUTH)).rejects.toThrow('unauthorized');
  });

  it('17. MCP inspector: network failure throws error', async () => {
    mockFetch.mockRejectedValue(new TypeError('Failed to fetch'));
    await expect(inspectServer(MCP_URL, MCP_AUTH)).rejects.toThrow('Failed to connect to MCP server');
  });

  it('18. MCP inspector: multiple tools parsed from SSE', async () => {
    mockFetch
      .mockResolvedValueOnce(okSse({ serverInfo: { name: 's', version: '1' }, capabilities: {} }))
      .mockResolvedValueOnce(okSse({}))
      .mockResolvedValueOnce(okSse({ tools: [
        { name: 'tool_a', description: 'A', inputSchema: {} },
        { name: 'tool_b', description: 'B', inputSchema: {} },
        { name: 'tool_c', description: 'C', inputSchema: {} },
      ] }));

    const result = await inspectServer(MCP_URL, MCP_AUTH);
    expect(result.tools.map((t) => t.name)).toEqual(['tool_a', 'tool_b', 'tool_c']);
  });

  it('19. MCP inspector: JSON response (non-SSE) parsed correctly', async () => {
    mockFetch
      .mockResolvedValueOnce({
        ok: true, status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: () => Promise.resolve(JSON.stringify({ jsonrpc: '2.0', id: 1, result: { serverInfo: { name: 'json-srv', version: '2' }, capabilities: {} } })),
      })
      .mockResolvedValueOnce({
        ok: true, status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: () => Promise.resolve(''),
      })
      .mockResolvedValueOnce({
        ok: true, status: 200,
        headers: new Map([['content-type', 'application/json']]),
        text: () => Promise.resolve(JSON.stringify({ jsonrpc: '2.0', id: 3, result: { tools: [{ name: 'x', description: 'X', inputSchema: {} }] } })),
      });

    const result = await inspectServer(MCP_URL, MCP_AUTH);
    expect(result.serverName).toBe('json-srv');
    expect(result.tools).toHaveLength(1);
    expect(result.tools[0].name).toBe('x');
  });

  it('20. MCP inspector: empty tools list returns empty array', async () => {
    mockFetch
      .mockResolvedValueOnce(okSse({ serverInfo: { name: 'empty', version: '1' }, capabilities: {} }))
      .mockResolvedValueOnce(okSse({}))
      .mockResolvedValueOnce(okSse({ tools: [] }));

    const result = await inspectServer(MCP_URL, MCP_AUTH);
    expect(result.tools).toEqual([]);
    expect(result.serverName).toBe('empty');
  });

  it('21. MCP inspector: server error response throws', async () => {
    mockFetch
      .mockResolvedValueOnce(okSse({ serverInfo: { name: 's', version: '1' }, capabilities: {} }))
      .mockResolvedValueOnce(okSse({}))
      .mockResolvedValueOnce({
        ok: true, status: 200, headers: new Map(),
        text: () => Promise.resolve(`data: ${JSON.stringify({ jsonrpc: '2.0', id: 3, error: { code: -32000, message: 'Internal error' } })}`),
      });

    await expect(inspectServer(MCP_URL, MCP_AUTH)).rejects.toThrow('Internal error');
  });
});
