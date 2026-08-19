import { ContentBlock } from '../types';
import { isWafResponse } from './waf';

export class WafChallengeError extends Error {
  constructor(public url: string) {
    super('WAF_VERIFICATION_REQUIRED');
    this.name = 'WafChallengeError';
  }
}

interface AnthropicRequest {
  model: string;
  max_tokens: number;
  messages: Array<{ role: string; content: string | ContentBlock[] }>;
  mcp_servers?: Array<{
    type: string;
    url: string;
    name: string;
    authorization_token?: string;
  }>;
  tools?: Array<{
    type: string;
    mcp_server_name: string;
  }>;
}

export async function sendMessage(
  apiKey: string,
  baseUrl: string,
  model: string,
  messages: Array<{ role: string; content: string | ContentBlock[] }>,
  mcpServers: Array<{
    id: string;
    url: string;
    token?: string;
    enabled: boolean;
  }>,
  wafCookies?: Record<string, string>
): Promise<ContentBlock[]> {
  const enabledServers = mcpServers.filter((s) => s.enabled);
  const endpoint = baseUrl.replace(/\/+$/, '') + '/v1/messages';

  const body: AnthropicRequest = {
    model,
    max_tokens: 4096,
    messages,
  };

  if (enabledServers.length > 0) {
    body.mcp_servers = enabledServers.map((s) => ({
      type: 'url',
      url: s.url,
      name: s.id,
      authorization_token: s.token,
    }));
    body.tools = enabledServers.map((s) => ({
      type: 'mcp_toolset',
      mcp_server_name: s.id,
    }));
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'x-api-key': apiKey,
    'anthropic-version': '2023-06-01',
    'anthropic-beta': 'mcp-client-2025-11-20',
  };

  if (wafCookies && Object.keys(wafCookies).length > 0) {
    const cookieStr = Object.entries(wafCookies)
      .map(([k, v]) => `${k}=${v}`)
      .join('; ');
    headers['Cookie'] = cookieStr;
  }

  const response = await fetch(endpoint, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });

  const rawText = await response.text();

  if (isWafResponse(rawText)) {
    throw new WafChallengeError(endpoint);
  }

  if (!response.ok) {
    let errorData: any = {};
    try {
      errorData = JSON.parse(rawText);
    } catch {}
    throw new Error(
      errorData.error?.message ?? `API error: ${response.status}`
    );
  }

  const data = JSON.parse(rawText);
  return data.content as ContentBlock[];
}
