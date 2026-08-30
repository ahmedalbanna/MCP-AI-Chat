export type Provider = {
  id: string;
  name: string;
  models: string[];
  apiKey: string;
  baseUrl: string;
  temp: number;
  maxTokens: number;
  enabled: boolean;
  streaming: boolean;
  latency: number;
};

export type MCPServer = {
  id: string;
  name: string;
  transport: "sse" | "stdio" | "http";
  endpoint: string;
  tools: string;
  status: "connected" | "disconnected" | "connecting";
  color: "pg" | "aws" | "fs" | "custom";
  desc: string;
  timeout: number;
  retries: number;
  env: { k: string; v: string }[];
  latency: number;
  health: "ok" | "off" | "warn";
  lastSeen: number;
  autoConnect: boolean;
  healthCheck: boolean;
};

export type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  text?: string;
  html?: "pg" | "aws" | "fs" | "generic";
  time: string;
  streaming?: boolean;
  tasks?: { id: string; text: string; done: boolean }[];
  todos?: { text: string; done: boolean }[];
  status?: "done" | "streaming";
  attachments?: AttachmentPreview[];
};

export type AttachmentPreview = {
  id: string;
  name: string;
  size: number;
  type: string;
  url?: string;
  kind: "image" | "video" | "audio" | "file";
};

export type Chat = {
  id: string;
  title: string;
  created: number;
  archived: boolean;
  messages: ChatMessage[];
};

export type AgentMode = {
  id: string;
  name: string;
  icon: string;
  color: string;
  desc: string;
  prompt: string;
  temp: number;
  tokens: number;
  provider: string;
  model: string;
  servers: string[];
  tools: { sql: boolean; fs: boolean; cloud: boolean; web: boolean };
  builtin: boolean;
};

export type MarketItem = {
  id: string;
  name: string;
  cat: "db" | "cloud" | "fs" | "dev";
  icon: string;
  color: string;
  desc: string;
  author: string;
  version: string;
  stars: number;
  installs: number;
  installed: boolean;
  featured: boolean;
  trending: boolean;
  isNew: boolean;
  tags: string[];
  tools: string[];
  updated: string;
  verified: boolean;
};

export type PromptTemplate = {
  cat: string;
  title: string;
  prompt: string;
  icon: string;
  modes: string[];
};

export type SlashCmd = {
  key: string;
  cat: string;
  icon: string;
  desc: string;
  hint: string;
  insert: string;
};
