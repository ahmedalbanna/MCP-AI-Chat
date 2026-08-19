# AGENTS.md — MCP AI Chat

## Expo Version

Read exact versioned docs at https://docs.expo.dev/versions/v57.0.0/ before writing any code.

## Running the App

### Web (Fastest for Real Testing)

```bash
npx expo start --web
```

Opens at `http://localhost:8081`. Full API testing works from browser — no device needed. WAF cookies handled via browser cookie jar natively.

For production-like build:

```bash
npx expo export:web
npx serve dist
```

### Mobile Device (Expo Go)

```bash
npx expo start
```

Scan QR code with Expo Go (iOS/Android). First WAF challenge triggers a WebView modal — solve captcha once, cookies persist via SecureStore.

### Android Emulator / iOS Simulator

```bash
npx expo start --android
npx expo start --ios
```

Requires Android Studio or Xcode installed.

## Project Conventions

- TypeScript strict — run `npx tsc --noEmit` before commits
- State: Zustand stores only (`src/stores/`)
- Storage: `expo-secure-store` for API keys and WAF cookies
- Theme: ERPNext color palette, toggled via `ThemeContext`
- Routing: `expo-router` file-based tabs in `app/(tabs)/`
- No comments in code unless requested

## Key Services

| File | Purpose |
|------|---------|
| `src/services/anthropic.ts` | Messages API, MCP Connector header |
| `src/services/mcp-http.ts` | Streamable HTTP for Inspector tool discovery |
| `src/services/waf.ts` | WAF detection + cookie persistence |
| `src/services/storage.ts` | SecureStore helpers with defaults |

## MCP Connector

Uses Anthropic beta header `mcp-client-2025-11-20`. Tool discovery and execution is server-side — no client-side MCP tool runner needed. The app sends `mcp_servers` + `tools` in the request body, the proxy handles everything.

## WAF Cookie Flow

When `agentrouter.org` returns Aliyun WAF captcha HTML:
1. App detects via `isWafResponse()` check
2. Shows `WafChallenge` WebView modal
3. User solves captcha in WebView
4. Cookies extracted via `postMessage` + saved to SecureStore
5. Original request retried with cookies in `Cookie` header

On web platform, WAF cookies are handled natively by the browser — no WebView needed.

## Pushing to GitHub

```bash
git add -A
git commit -m "type: description"
git push origin main
```

Private repo: `https://github.com/ahmedalbanna/MCP-AI-Chat`
