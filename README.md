# MCP AI Chat

React Native (Expo) mobile app for AI chat with **Model Context Protocol (MCP)** capabilities.

## Features

- **Anthropic Claude API** integration via configurable proxy (`agentrouter.org`)
- **MCP Connector** — discover and invoke tools from remote MCP servers (beta header `mcp-client-2025-11-20`)
- **Server Inspector** — view tool names, descriptions, and input schemas for any MCP server
- **Tool Call Display** — visual cards showing tool invocations and results inline in chat
- **Preset Server** — ERPNext Gateway (`185.190.140.93:8022`) pre-configured (disabled by default)
- **Dark / Light Mode** — ERPNext/ERPNext color palette with system auto-switch
- **WAF Cookie Flow** — Aliyun WAF verification via hidden WebView; cookies saved for future requests

## Quick Start

```bash
npm install
npx expo start
```

Scan the QR code with **Expo Go** on your phone.

## Configuration

In **Settings** tab:

| Field | Default |
|-------|---------|
| API Key | `sk-XKnp5MEgQX...` |
| Base URL | `https://agentrouter.org` |
| Model | `claude-opus-4-8` |

To enable ERPNext MCP tools: go to **Settings → MCP Servers** and toggle **erpnext-gateway** on.

## Project Structure

```
mcp-ai-chat/
├── app/                    # Expo Router screens
│   ├── _layout.tsx         # Root layout with ThemeProvider
│   └── (tabs)/
│       ├── _layout.tsx     # Tab navigator (Chat, Inspector, Settings)
│       ├── index.tsx       # Chat screen
│       ├── inspector.tsx   # MCP Server Inspector
│       └── settings.tsx    # Settings (API key, servers)
├── src/
│   ├── components/         # UI components
│   │   ├── ChatMessage.tsx
│   │   ├── InputBar.tsx
│   │   ├── ToolCallCard.tsx
│   │   ├── ServerCard.tsx
│   │   ├── ToolCard.tsx
│   │   ├── LoadingIndicator.tsx
│   │   ├── EmptyState.tsx
│   │   └── WafChallenge.tsx
│   ├── services/
│   │   ├── anthropic.ts    # Messages API + MCP Connector
│   │   ├── mcp-http.ts     # Streamable HTTP client (inspector)
│   │   └── waf.ts          # WAF cookie flow
│   ├── stores/             # Zustand state management
│   │   ├── chat-store.ts
│   │   ├── mcp-store.ts
│   │   └── settings-store.ts
│   ├── theme/              # Light/dark palette + context
│   │   ├── colors.ts
│   │   └── ThemeContext.tsx
│   ├── types/
│   │   └── index.ts
│   └── constants/
│       └── preset-servers.ts
├── app.json
├── package.json
└── tsconfig.json
```

## Stack

- Expo SDK 57 · React 19.2 · React Native 0.86
- expo-router · expo-secure-store
- Zustand · TypeScript 6
- react-native-webview (WAF challenge)

## License

MIT
