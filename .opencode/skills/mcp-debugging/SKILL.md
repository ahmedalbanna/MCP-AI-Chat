---
name: mcp-debugging
description: Diagnoses MCP server handshake, tool discovery, tool calls, and resilience behavior. Use ONLY when debugging MCP connections, sync failures, tool errors, or retry/circuit-breaker behavior.
---

# MCP Debugging

## Protocol flow (`data/network/McpHttpClient.kt`)

Initialize → `tools/list` → `tools/call` (JSON-RPC over HTTP; SSE `data:` frames tolerated via `extractJsonRpcBody`).

## Resilience (`data/network/Resilience.kt`)

- `RetryManager`/`CircuitBreaker` apply to **idempotent** calls only (`initialize`, `tools/list`). Never retried: `tools/call`.
- `CircuitBreaker.canExecute()` gates `discoverTools` and `executeToolCall`; check `isCircuitOpen` first on repeated failures.
- Monotonic `AtomicLong` RPC ids; session key is `endpoint|auth`.
- Timeouts are global: MCP 15s connect / 30s read-write (`di/AppModule#provideMcpOkHttpClient`); AI providers 60s. Per-server timeouts are WONTFIX.

## Data path

- `McpRepository`: URL validation (https except loopback), `syncServerTools` only wipes/inserts cached tools on `CONNECTED`, preserves cache on `ERROR`. Auth headers encrypted at rest (`CryptoManager`).
- `ChatRepository`: provider-agnostic tool loop (`MAX_TOOL_HOPS=5`); `ProviderToolCatalog` owns provider-specific wire names and resolves responses to stable `McpTool.id` values. Unknown tools get a synthetic error `McpLogItem` without a network call.

## Debug playbook

1. Reproduce in the **Inspector test bench** (tool picker → params → Invoke) before touching code.
2. Check audit logs / `McpLogItem` request/response payloads for the failing hop.
3. For handshake/list/call issues add a `MockWebServer` test (`okhttp3.mockwebserver.MockWebServer` directly); `McpHttpClient`/`GeminiApiService` accept an injected `OkHttpClient`.
4. Typed errors live in `ChatException`; UI mapping in `ChatViewModel:202`. Don't reintroduce substring matching.
