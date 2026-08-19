export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  toolCalls?: ToolCall[];
  timestamp: number;
}

export interface ToolCall {
  id: string;
  name: string;
  serverName: string;
  input: Record<string, unknown>;
  result?: ToolResult;
}

export interface ToolResult {
  content: Array<{ type: string; text?: string }>;
  isError: boolean;
}

export interface MCPServerConfig {
  id: string;
  name: string;
  url: string;
  type: 'remote';
  enabled: boolean;
  headers?: Record<string, string>;
  tools?: MCPTool[];
  connected?: boolean;
}

export interface MCPTool {
  name: string;
  description: string;
  inputSchema: Record<string, unknown>;
}

export interface AnthropicResponse {
  id: string;
  type: string;
  role: string;
  content: ContentBlock[];
  model: string;
  stop_reason: string | null;
  usage: {
    input_tokens: number;
    output_tokens: number;
  };
}

export interface ContentBlock {
  type: 'text' | 'mcp_tool_use' | 'mcp_tool_result';
  text?: string;
  id?: string;
  name?: string;
  server_name?: string;
  input?: Record<string, unknown>;
  tool_use_id?: string;
  content?: Array<{ type: string; text?: string }>;
  is_error?: boolean;
}

export interface ChatState {
  messages: Message[];
  isLoading: boolean;
  error: string | null;
  sendMessage: (content: string) => Promise<void>;
  clearMessages: () => void;
}

export interface MCPState {
  servers: MCPServerConfig[];
  toggleServer: (id: string) => void;
  updateServer: (id: string, updates: Partial<MCPServerConfig>) => void;
  setServerTools: (id: string, tools: MCPTool[]) => void;
  setServerConnected: (id: string, connected: boolean) => void;
}

export interface SettingsState {
  apiKey: string;
  baseUrl: string;
  model: string;
  themeMode: 'light' | 'dark' | 'system';
  setApiKey: (key: string) => void;
  setBaseUrl: (url: string) => void;
  setModel: (model: string) => void;
  setThemeMode: (mode: 'light' | 'dark' | 'system') => void;
}
