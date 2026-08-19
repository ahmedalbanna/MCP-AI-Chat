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

async function jsonrpcPost(
  url: string,
  method: string,
  params?: Record<string, unknown>,
  headers?: Record<string, string>
): Promise<unknown> {
  const body: JSONRPCRequest = {
    jsonrpc: '2.0',
    id: nextId(),
    method,
    params,
  };

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json, text/event-stream',
      ...headers,
    },
    body: JSON.stringify(body),
  });

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
      return parsed.result;
    }
    throw new Error('No data in SSE response');
  }

  const data = (await response.json()) as JSONRPCResponse;
  if (data.error) throw new Error(data.error.message);
  return data.result;
}

export async function initializeServer(
  url: string,
  headers?: Record<string, string>
): Promise<{ name: string; version: string; capabilities: unknown }> {
  const result = (await jsonrpcPost(
    url,
    'initialize',
    {
      protocolVersion: '2025-03-26',
      capabilities: {},
      clientInfo: { name: 'mcp-ai-chat', version: '1.0.0' },
    },
    headers
  )) as { serverInfo?: { name: string; version: string }; capabilities?: unknown };

  await jsonrpcPost(url, 'notifications/initialized', {}, headers);

  return {
    name: result.serverInfo?.name ?? 'Unknown',
    version: result.serverInfo?.version ?? '0.0.0',
    capabilities: result.capabilities ?? {},
  };
}

export async function listTools(
  url: string,
  headers?: Record<string, string>
): Promise<MCPTool[]> {
  const result = (await jsonrpcPost(
    url,
    'tools/list',
    {},
    headers
  )) as { tools?: Array<{ name: string; description: string; inputSchema: Record<string, unknown> }> };

  return (result.tools ?? []).map((t) => ({
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
  const tools = await listTools(url, headers);
  return { tools, serverName: info.name };
}
