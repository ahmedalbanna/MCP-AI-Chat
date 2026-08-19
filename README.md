# MCP AI Chat

Native Android chat application powered by Gemini AI with Model Context Protocol (MCP) integration. Dynamically discovers and invokes tools from remote MCP servers.

## Features

- **Gemini AI Chat** — conversational AI with multi-turn tool execution
- **MCP Server Management** — connect to JSON-RPC or SSE servers, enable/disable dynamically
- **Inspector Panel** — view and invoke MCP tools with custom parameters
- **Activity Logs** — full request/response history with filtering
- **Material 3 Dynamic Colors** — adaptive theming on Android 12+

## Architecture

```
UI Layer (Compose)           ViewModel Layer                Data Layer
┌─────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│ ChatScreen      │    │ ChatViewModel       │    │ ChatRepository       │
│ ServersScreen   │◄──►│ McpServersViewModel │◄──►│ McpRepository        │
│ InspectorScreen │    │ McpInspectorViewModel│    │ McpHttpClient        │
│ LogsScreen      │    │                     │    │ GeminiApiService     │
└─────────────────┘    └─────────────────────┘    │ Room (AppDatabase)   │
                                                  └──────────────────────┘
```

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt DI, Room, OkHttp, Coroutines

## Setup

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd MCP-AI-Chat
   ```

2. **Configure API key:**
   Copy `.env.example` to `.env` and add your Gemini API key:
   ```
   GEMINI_API_KEY=your_key_here
   ```

3. **Build and run:**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open in Android Studio and run on device/emulator.

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GEMINI_API_KEY` | Yes | Google Gemini API key for AI responses |
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
│   ├── model/          # Domain models, sealed classes
│   ├── network/        # MCP client, Gemini API, Firebase
│   └── repository/     # Data orchestration
├── di/                 # Hilt modules
├── ui/
│   ├── components/     # Reusable composables
│   ├── navigation/     # Nav graph, bottom bar
│   ├── screens/        # Feature screens
│   ├── theme/          # Material 3 theme
│   └── viewmodel/      # ViewModels with UiState
└── McpChatApplication.kt
```
