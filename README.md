# MCP AI Chat

Native Android chat application powered by pluggable AI providers (Google Gemini, Anthropic Claude, or any OpenAI-compatible endpoint) with Model Context Protocol (MCP) integration. Dynamically discovers and invokes tools from remote MCP servers.

## Features

- **Multi-provider AI chat** — switch between Google Gemini, Anthropic Claude, OpenAI/ChatGPT, local runtimes (OpenCode, Ollama, vLLM, LM Studio), or custom endpoints; MCP tool calling works with every provider that supports function calling
- **Encrypted credentials** — provider API keys are stored on-device via `EncryptedSharedPreferences` (AndroidKeyStore)
- **MCP Server Management** — connect to JSON-RPC or SSE servers, enable/disable dynamically
- **Inspector Panel** — view and invoke MCP tools with custom parameters
- **Activity Logs** — full request/response history with filtering
- **Material 3 Dynamic Colors** — adaptive theming on Android 12+

## Architecture

```
UI Layer (Compose)                  ViewModel Layer                      Data Layer
┌──────────────────────┐    ┌────────────────────────┐    ┌─────────────────────────────┐
│ ChatScreen           │    │ ChatViewModel          │    │ ChatRepository              │
│ ServersScreen        │◄──►│ McpServersViewModel    │◄──►│ McpRepository               │
│ InspectorScreen      │    │ McpInspectorViewModel  │    │ ProviderChatRouter          │
│ OnboardingScreen     │    │ AiProviderSettings     │    │  ├─ GeminiApiService        │
│ ProviderSettingsSheet│    │   ViewModel            │    │  ├─ AnthropicClient         │
└──────────────────────┘    └────────────────────────┘    │  └─ OpenAiCompatibleClient  │
                                                          │ McpHttpClient · Resilience  │
                                                          │ Room (AppDatabase)          │
                                                          │ Security (CryptoManager)    │
                                                          └─────────────────────────────┘
```

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt DI, Room, OkHttp, Coroutines, EncryptedSharedPreferences

## Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd MCP-AI-Chat
   ```

2. **Configure API key (optional):**
   Copy `.env.example` to `.env` to set a default Gemini key. At runtime, API keys for
   any provider are added in-app via the **AI Provider settings sheet** and stored
   encrypted on-device:
   ```
   GEMINI_API_KEY=your_key_here
   ```

3. **Build and run:**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and run on device/emulator.

## Building

**Debug APK** (signed with the debug keystore):
```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

**Signed release APK** (R8-minified; release signing config expects a keystore):
```bash
# 1. Generate an upload keystore once:
keytool -genkeypair -keystore my-upload-key.jks -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Build with signing credentials:
KEYSTORE_PATH=my-upload-key.jks \
STORE_PASSWORD=... KEY_PASSWORD=... \
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

> `KEYSTORE_PATH` defaults to `my-upload-key.jks` in the repo root. Never commit
> keystores or passwords (`my-upload-key.jks` is already in `.gitignore`).

> **Note:** MCP server endpoints must use `https://` (plain `http` is only
> accepted for localhost development servers) so auth headers are never sent
> in cleartext. The local Room database is excluded from Android cloud
> backups because it stores server credentials and audit logs.

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes* | Default Google Gemini API key (*or configure providers in-app) |
| `KEYSTORE_PATH` | Release | Path to upload keystore (default: `my-upload-key.jks`) |
| `STORE_PASSWORD` | Release | Keystore password |
| `KEY_PASSWORD` | Release | Key alias password |

## MCP Protocol Flow

1. **Initialize** — send `initialize` request with protocol version and capabilities
2. **Discover** — call `tools/list` to enumerate available tools
3. **Execute** — invoke `tools/call` with tool name and arguments
4. **Loop** — Gemini AI autonomously chains tool calls until task is complete

## Testing

```bash
./gradlew test                    # Unit + Robolectric tests
./gradlew detekt                  # Static analysis
./gradlew spotlessCheck           # Code formatting check
```

## Project Structure

```
app/src/main/java/com/aistudio/mcpchat/
├── data/
│   ├── local/          # Room entities, DAOs, database
│   ├── model/          # Domain models, provider definitions
│   ├── network/        # MCP client, provider clients (Gemini/Anthropic/OpenAI-compatible), resilience
│   ├── repository/     # Data orchestration, system prompt builder
│   ├── security/       # Encrypted provider settings, crypto helpers
│   └── tool/           # Built-in tools (memory, fallback handler)
├── di/                 # Hilt modules
├── ui/
│   ├── screens/        # Feature screens + provider settings sheet
│   ├── theme/          # Material 3 theme
│   └── viewmodel/      # ViewModels with UiState
├── MainActivity.kt
└── McpChatApplication.kt
```
