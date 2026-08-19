import { Platform } from 'react-native';
import { MCPTool } from '../types';

interface JSONRPCRequest {
  jsonrpc: '2.0';
  id: number;
  method: string;
  params?: Record<string, unknown>;
}

interface JSONRPCResponse {
  jsonrpc: '2.0';
  id: number;
  result?: unknown;
  error?: { code: number; message: string };
}

let requestId = 0;

function nextId(): number {
  return ++requestId;
}

function proxyUrl(url: string): string {
  if (Platform.OS === 'web') {
    const u = new URL(url);
    return `/mcp-proxy${u.pathname}${u.search}`;
  }
  return url;
}

async function jsonrpcPost(
  url: string,
  method: string,
  params?: Record<string, unknown>,
  headers?: Record<string, string>,
  sessionId?: string
): Promise<{ result: unknown; sessionId?: string }> {
  const body: JSONRPCRequest = {
    jsonrpc: '2.0',
    id: nextId(),
    method,
    params,
  };

  const reqHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json, text/event-stream',
    ...headers,
  };

  if (sessionId) {
    reqHeaders['Mcp-Session-Id'] = sessionId;
  }

  const target = proxyUrl(url);

  let response: Response;
  try {
    response = await fetch(target, {
      method: 'POST',
      headers: reqHeaders,
      body: JSON.stringify(body),
    });
  } catch (err: any) {
    throw new Error(
      `Failed to connect to MCP server. ${Platform.OS === 'web' ? 'On web, this may be a CORS issue — try on mobile.' : ''} ${err?.message ?? ''}`
    );
  }

  const newSessionId = response.headers.get('mcp-session-id') ?? sessionId;
  const contentType = response.headers.get('content-type') ?? '';

  if (contentType.includes('text/event-stream')) {
    const text = await response.text();
    const lines = text.split('\n');
    let lastData = '';
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        lastData = line.slice(6);
      }
    }
    if (lastData) {
      const parsed = JSON.parse(lastData) as JSONRPCResponse;
      if (parsed.error) throw new Error(parsed.error.message);
      return { result: parsed.result, sessionId: newSessionId ?? undefined };
    }
    throw new Error('No data in SSE response');
  }

  const text = await response.text();
  if (!text) {
    return { result: null, sessionId: newSessionId ?? undefined };
  }

  const data = JSON.parse(text) as JSONRPCResponse;
  if (data.error) throw new Error(data.error.message);
  return { result: data.result, sessionId: newSessionId ?? undefined };
}

export async function initializeServer(
  url: string,
  headers?: Record<string, string>
): Promise<{ name: string; version: string; capabilities: unknown; sessionId?: string }> {
  const { result, sessionId } = await jsonrpcPost(
    url,
    'initialize',
    {
      protocolVersion: '2025-03-26',
      capabilities: {},
      clientInfo: { name: 'mcp-ai-chat', version: '1.0.0' },
    },
    headers
  );

  const info = result as { serverInfo?: { name: string; version: string }; capabilities?: unknown };

  try {
    await jsonrpcPost(url, 'notifications/initialized', {}, headers, sessionId);
  } catch {
    // notifications/initialized may not return a response — that's OK
  }

  return {
    name: info.serverInfo?.name ?? 'Unknown',
    version: info.serverInfo?.version ?? '0.0.0',
    capabilities: info.capabilities ?? {},
    sessionId,
  };
}

export async function listTools(
  url: string,
  headers?: Record<string, string>,
  sessionId?: string
): Promise<MCPTool[]> {
  const { result } = await jsonrpcPost(
    url,
    'tools/list',
    {},
    headers,
    sessionId
  );

  const data = result as { tools?: Array<{ name: string; description: string; inputSchema: Record<string, unknown> }> };

  return (data.tools ?? []).map((t) => ({
    name: t.name,
    description: t.description ?? '',
    inputSchema: t.inputSchema ?? {},
  }));
}

export async function inspectServer(
  url: string,
  headers?: Record<string, string>
): Promise<{ tools: MCPTool[]; serverName: string }> {
  const info = await initializeServer(url, headers);
  const tools = await listTools(url, headers, info.sessionId);
  return { tools, serverName: info.name };
}
