# Changelog

## 1.1.0
- Multi-provider AI chat (Gemini default `gemini-3.6-flash`, Anthropic, OpenAI-compatible) with MCP tool calling on every function-calling provider
- SSE streaming with live token preview, stop-and-persist, regenerate, and throughput progress bar
- MCP resource subscriptions with server-push updates; image attachments and vision across providers
- Room FTS message search, paged message windows, adaptive tablet/foldable layouts
- Persistent theming (system/light/dark, AMOLED, dynamic color) with hardened accessibility
- Security: encrypted provider/API-key storage, auth-header encryption at rest, backup exclusions, no destructive migrations
- Quality gates green: detekt (0 issues), spotless, 450+ unit tests, 23 Roborazzi goldens, lint, Kover 60%

## 1.0
- Initial release: Gemini chat with MCP server management, tool inspector, and activity logs
