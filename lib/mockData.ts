import type { Provider, MCPServer, Chat, MarketItem, PromptTemplate, SlashCmd, AgentMode } from "./types";

export const DEFAULT_PROVIDERS: Provider[] = [
  { id: "openai", name: "OpenAI", models: ["gpt-4o", "gpt-4o-mini", "o1-preview"], apiKey: "", baseUrl: "https://api.openai.com/v1", temp: 0.7, maxTokens: 4096, enabled: true, streaming: true, latency: 142 },
  { id: "anthropic", name: "Anthropic", models: ["claude-3.5-sonnet", "claude-3-opus"], apiKey: "", baseUrl: "https://api.anthropic.com", temp: 0.7, maxTokens: 8192, enabled: true, streaming: true, latency: 210 },
  { id: "google", name: "Google", models: ["gemini-1.5-pro", "gemini-1.5-flash"], apiKey: "", baseUrl: "https://generativelanguage.googleapis.com", temp: 0.8, maxTokens: 8192, enabled: true, streaming: true, latency: 180 },
  { id: "local", name: "Local", models: ["llama-3.1-70B", "mistral-large"], apiKey: "", baseUrl: "http://localhost:11434", temp: 0.6, maxTokens: 4096, enabled: false, streaming: false, latency: 45 },
];

export const DEFAULT_SERVERS: MCPServer[] = [
  { id: "srv1", name: "aws-us-east", transport: "sse", endpoint: "wss://aws.mcp.local/sse", tools: "s3_list, ec2_status", status: "connected", color: "aws", desc: "AWS S3 + EC2 in us-east-1", timeout: 10000, retries: 2, env: [], latency: 142, health: "ok", lastSeen: Date.now(), autoConnect: true, healthCheck: true },
  { id: "srv2", name: "postgres-mcp", transport: "stdio", endpoint: "npx @modelcontextprotocol/server-postgres", tools: "query, list_tables, describe_schema", status: "connected", color: "pg", desc: "Postgres — remote_db", timeout: 10000, retries: 2, env: [{ k: "DATABASE_URL", v: "postgres://..." }], latency: 68, health: "ok", lastSeen: Date.now(), autoConnect: true, healthCheck: true },
  { id: "srv3", name: "local-fs", transport: "stdio", endpoint: "npx @modelcontextprotocol/server-filesystem /data", tools: "read, write, list", status: "disconnected", color: "fs", desc: "Local filesystem /data sandbox", timeout: 8000, retries: 1, env: [], latency: 12, health: "off", lastSeen: Date.now() - 3600000, autoConnect: false, healthCheck: true },
];

export const DEFAULT_CHATS: Chat[] = [
  {
    id: "c1", title: "PostgreSQL users query", created: Date.now() - 3600000, archived: false, messages: [
      { id: "m1", role: "user", text: "Query the remote PostgreSQL database for the last 5 user registrations and format as a table.", time: "10:21 AM" },
      { id: "m2", role: "assistant", html: "pg", time: "10:21 AM", status: "done" },
    ]
  },
  {
    id: "c2", title: "S3 bucket audit", created: Date.now() - 86400000, archived: false, messages: [
      { id: "m3", role: "user", text: "List S3 buckets in aws-us-east", time: "09:12 AM" },
      { id: "m4", role: "assistant", html: "aws", time: "09:12 AM", status: "done" },
    ]
  },
  { id: "c3", title: "Local filesystem", created: Date.now() - 172800000, archived: true, messages: [{ id: "m5", role: "user", text: "List files in /data", time: "Yesterday" }] },
];

export const MARKET: MarketItem[] = [
  { id: "m1", name: "Postgres Pro", cat: "db", icon: "database", color: "#334155", desc: "Advanced SQL with vector search, migrations & pgvector support", author: "modelcontextprotocol", version: "2.1.0", stars: 4.9, installs: 12400, installed: true, featured: true, trending: true, isNew: false, tags: ["postgres", "sql", "vector"], tools: ["query", "list_tables", "describe_schema"], updated: "2024-10-12", verified: true },
  { id: "m2", name: "AWS Suite", cat: "cloud", icon: "cloud", color: "#ff9900", desc: "S3, EC2, Lambda, CloudWatch unified with MFA support", author: "aws", version: "1.8.3", stars: 4.8, installs: 8400, installed: true, featured: true, trending: true, isNew: false, tags: ["aws", "s3", "ec2"], tools: ["s3_list", "ec2_status", "lambda_invoke"], updated: "2024-10-20", verified: true },
  { id: "m3", name: "Filesystem", cat: "fs", icon: "folder", color: "#2563eb", desc: "Secure local file access with sandboxing & watcher", author: "modelcontextprotocol", version: "1.4.2", stars: 4.7, installs: 6100, installed: true, featured: false, trending: false, isNew: false, tags: ["fs", "local", "watch"], tools: ["read", "write", "list"], updated: "2024-09-28", verified: true },
  { id: "m4", name: "GitHub", cat: "dev", icon: "git-branch", color: "#0f172a", desc: "Repos, PRs, issues, actions, code search", author: "github", version: "3.0.1", stars: 4.9, installs: 15200, installed: false, featured: true, trending: true, isNew: false, tags: ["git", "pr", "actions"], tools: ["list_repos", "create_pr", "search_code"], updated: "2024-10-18", verified: true },
  { id: "m5", name: "Slack", cat: "cloud", icon: "message-square", color: "#e11d48", desc: "Channels, messages, threads, reactions & workflows", author: "slack", version: "1.2.5", stars: 4.6, installs: 9000, installed: false, featured: false, trending: false, isNew: true, tags: ["chat", "slack"], tools: ["list_channels", "post_message"], updated: "2024-10-22", verified: false },
  { id: "m6", name: "Notion", cat: "fs", icon: "book-open", color: "#000", desc: "Pages, databases, blocks, comments API", author: "notion", version: "0.9.4", stars: 4.5, installs: 5200, installed: false, featured: false, trending: false, isNew: true, tags: ["notion", "docs"], tools: ["query_db", "create_page"], updated: "2024-10-05", verified: false },
  { id: "m7", name: "Redis", cat: "db", icon: "layers", color: "#dc2626", desc: "Cache, pub/sub, streams & cluster management", author: "redis", version: "1.1.0", stars: 4.7, installs: 4100, installed: false, featured: false, trending: true, isNew: false, tags: ["redis", "cache"], tools: ["get", "set", "publish"], updated: "2024-09-15", verified: true },
  { id: "m8", name: "Puppeteer", cat: "dev", icon: "globe", color: "#7c3aed", desc: "Browser automation, scraping & PDF gen", author: "browser", version: "2.3.0", stars: 4.8, installs: 7300, installed: false, featured: false, trending: true, isNew: false, tags: ["browser", "scrape"], tools: ["navigate", "screenshot", "pdf"], updated: "2024-10-10", verified: true },
  { id: "m9", name: "Stripe", cat: "cloud", icon: "credit-card", color: "#635bff", desc: "Payments, customers, subscriptions, invoices", author: "stripe", version: "1.0.2", stars: 4.8, installs: 6800, installed: false, featured: false, trending: false, isNew: true, tags: ["payments", "stripe"], tools: ["list_customers", "create_charge"], updated: "2024-10-23", verified: true },
  { id: "m10", name: "Linear", cat: "dev", icon: "target", color: "#5e6ad2", desc: "Issues, projects, cycles, roadmap sync", author: "linear", version: "1.3.1", stars: 4.6, installs: 3100, installed: false, featured: false, trending: false, isNew: false, tags: ["linear", "issues"], tools: ["list_issues", "create_issue"], updated: "2024-09-30", verified: false },
];

export const PROMPTS: PromptTemplate[] = [
  { cat: "MCP", title: "SQL audit", prompt: "Query the remote PostgreSQL database for the last 5 user registrations and format as a table.", icon: "database", modes: ["Chat", "Research", "Code"] },
  { cat: "MCP", title: "S3 inventory", prompt: "List S3 buckets in aws-us-east and sort by size", icon: "cloud", modes: ["Chat", "Research"] },
  { cat: "MCP", title: "File explorer", prompt: "List files in /data and summarize CSV headers", icon: "folder-open", modes: ["Chat", "Code"] },
  { cat: "MCP", title: "Health check", prompt: "Run health check on all MCP servers and summarize latencies", icon: "activity", modes: ["Debug", "Shield"] },
  { cat: "Analysis", title: "Summarize logs", prompt: "Summarize CloudWatch logs for the last 24h, highlight errors", icon: "bar-chart-3", modes: ["Research", "Debug"] },
  { cat: "Analysis", title: "User insights", prompt: "Analyze user registrations trend and propose growth actions", icon: "trending-up", modes: ["Research", "Planning"] },
  { cat: "Analysis", title: "Cost review", prompt: "Analyze AWS costs by service and suggest savings (top 3 drivers)", icon: "wallet", modes: ["Planning", "Research"] },
  { cat: "Code", title: "Generate API", prompt: "Create a REST API for users table with pagination and auth", icon: "code-2", modes: ["Code", "Planning"] },
  { cat: "Code", title: "Debug query", prompt: "Debug why SELECT on remote_db.users is slow — check indexes and explain plan", icon: "bug", modes: ["Debug", "Code"] },
  { cat: "Code", title: "Refactor FS", prompt: "Refactor local-fs access to use streaming and add error handling", icon: "wrench", modes: ["Code", "Shield"] },
  { cat: "Write", title: "Release notes", prompt: "Draft release notes for MCP Terminal v2 with highlights", icon: "pen-tool", modes: ["Write", "Creative"] },
  { cat: "Write", title: "Onboarding guide", prompt: "Write a quickstart guide for connecting a new MCP server via STDIO", icon: "book-open", modes: ["Write", "Planning"] },
  { cat: "Creative", title: "Brainstorm names", prompt: "Brainstorm 10 creative names for the MCP marketplace with taglines", icon: "lightbulb", modes: ["Creative", "Write"] },
  { cat: "MCP", title: "Git diff review", prompt: "Review the latest git diff and summarize risk areas before push", icon: "git-branch", modes: ["Code", "Shield"] },
];

export const SLASH: SlashCmd[] = [
  { key: "/sql", cat: "MCP", icon: "database", desc: "Run SQL via postgres-mcp", hint: "SELECT * FROM users LIMIT 5", insert: "/sql SELECT * FROM users LIMIT 5" },
  { key: "/s3", cat: "MCP", icon: "cloud", desc: "List S3 buckets", hint: "aws-us-east", insert: "/s3 list buckets" },
  { key: "/fs", cat: "MCP", icon: "folder-open", desc: "List local files", hint: "/data", insert: "/fs list /data" },
  { key: "/github", cat: "MCP", icon: "git-branch", desc: "GitHub repos & PRs", hint: "list repos", insert: "/github list repos" },
  { key: "/marketplace", cat: "MCP", icon: "store", desc: "Browse MCP marketplace", hint: "", insert: "/marketplace" },
  { key: "/servers", cat: "MCP", icon: "server", desc: "List & toggle MCP servers", hint: "", insert: "/servers" },
  { key: "/new", cat: "Chat", icon: "plus", desc: "New conversation", hint: "", insert: "/new" },
  { key: "/clear", cat: "Chat", icon: "trash-2", desc: "Clear current chat", hint: "", insert: "/clear" },
  { key: "/export", cat: "Chat", icon: "download", desc: "Export conversation JSON", hint: "", insert: "/export" },
  { key: "/help", cat: "Chat", icon: "help-circle", desc: "Show help & tools", hint: "", insert: "/help" },
  { key: "/todo", cat: "Chat", icon: "list-checks", desc: "Add checklist item", hint: "e.g. review PR", insert: "/todo " },
  { key: "/mode", cat: "System", icon: "bot", desc: "Switch agent mode", hint: "Chat / Code / Research", insert: "/mode " },
  { key: "/model", cat: "System", icon: "cpu", desc: "Switch model/provider", hint: "gpt-4o, claude…", insert: "/model " },
  { key: "/connect", cat: "System", icon: "plug", desc: "Connect MCP server", hint: "<name>", insert: "/connect " },
  { key: "/disconnect", cat: "System", icon: "plug-zap", desc: "Disconnect server", hint: "<name>", insert: "/disconnect " },
  { key: "/retry", cat: "Chat", icon: "refresh-cw", desc: "Retry last message", hint: "", insert: "/retry" },
];

export const DEFAULT_MODES: AgentMode[] = [
  { id: "Chat", name: "Chat", icon: "message-circle", color: "#2563eb", desc: "Balanced, helpful general assistant", prompt: "You are a helpful, concise assistant. Use MCP tools when needed.", temp: 0.7, tokens: 4096, provider: "", model: "", servers: [], tools: { sql: true, fs: true, cloud: true, web: false }, builtin: true },
  { id: "Code", name: "Code", icon: "code-2", color: "#0f172a", desc: "Senior engineer — clean code, tests, review", prompt: "You are a senior software engineer. Produce clean, tested code.", temp: 0.2, tokens: 8192, provider: "", model: "", servers: [], tools: { sql: true, fs: true, cloud: false, web: false }, builtin: true },
  { id: "Research", name: "Research", icon: "search", color: "#7c3aed", desc: "Deep research with citations & MCP browsing", prompt: "You are a research assistant. Search, read, synthesize with citations.", temp: 0.5, tokens: 8192, provider: "", model: "", servers: [], tools: { sql: true, fs: true, cloud: true, web: true }, builtin: true },
  { id: "Planning", name: "Planning", icon: "map", color: "#0891b2", desc: "Project planning, roadmap, todos & deps", prompt: "You are a planning assistant. Break goals into todos.", temp: 0.4, tokens: 4096, provider: "", model: "", servers: [], tools: { sql: false, fs: true, cloud: false, web: false }, builtin: true },
  { id: "Write", name: "Write", icon: "pen-tool", color: "#db2777", desc: "Clear writing, editing, tone & structure", prompt: "You are an expert writer. Improve clarity, tone, structure.", temp: 0.8, tokens: 4096, provider: "", model: "", servers: [], tools: { sql: false, fs: true, cloud: false, web: false }, builtin: true },
  { id: "Debug", name: "Debug", icon: "bug", color: "#dc2626", desc: "Root-cause analysis, logs & fixes", prompt: "You are a debugging assistant. Analyze errors, logs, and MCP outputs.", temp: 0.3, tokens: 8192, provider: "", model: "", servers: [], tools: { sql: true, fs: true, cloud: true, web: false }, builtin: true },
  { id: "Shield", name: "Shield", icon: "shield", color: "#16a34a", desc: "Secure, privacy-aware, minimal tools", prompt: "You are a security-aware assistant. Minimize tool use.", temp: 0.3, tokens: 4096, provider: "", model: "", servers: [], tools: { sql: false, fs: false, cloud: false, web: false }, builtin: true },
  { id: "Creative", name: "Creative", icon: "palette", color: "#f59e0b", desc: "Brainstorm, ideas, variations & play", prompt: "You are a creative partner. Generate ideas, variations, playful tone.", temp: 0.9, tokens: 4096, provider: "", model: "", servers: [], tools: { sql: false, fs: false, cloud: false, web: true }, builtin: true },
];
