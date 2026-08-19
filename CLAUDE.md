# CLAUDE.md — MCP AI Chat

@AGENTS.md

## Quick Reference

### Run as Web (Real Testing)

```bash
npx expo start --web
# → http://localhost:8081
```

This is the fastest way to test the full flow — chat, API calls, WAF verification, MCP Inspector. Browser handles cookies natively so no WebView modal needed.

### Build for Production Web

```bash
npx expo export:web
npx serve dist
```

### Run on Device

```bash
npx expo start
# Scan QR with Expo Go
```

### Type Check

```bash
npx tsc --noEmit
```

## Architecture

- **Chat**: sends messages via Anthropic API with MCP Connector
- **Inspector**: connects directly to MCP servers via Streamable HTTP (no Anthropic proxy)
- **Settings**: API key, base URL, model, MCP server list

## MCP Connector Flow

App sends to `agentrouter.org/v1/messages`:
```json
{
  "model": "claude-opus-4-8",
  "messages": [...],
  "mcp_servers": [{"type": "url", "url": "http://...", "name": "..."}],
  "tools": [{"type": "mcp_toolset", "mcp_server_name": "..."}],
  "anthropic-beta": "mcp-client-2025-11-20"
}
```

No client-side tool execution — the proxy handles tool discovery and invocation server-side.

## WAF Handling

- **Web**: browser handles Aliyun WAF cookies natively — no special code needed
- **Mobile**: `WafChallenge` WebView opens → user solves captcha → cookies saved → request retried

## Key Files

```
src/services/anthropic.ts    — API calls + MCP Connector
src/services/mcp-http.ts     — Inspector direct MCP connection
src/services/waf.ts          — WAF detection + cookie save
src/stores/chat-store.ts     — Chat state + WAF retry flow
src/stores/settings-store.ts — API config + server list
src/components/WafChallenge.tsx — WebView captcha modal
```
