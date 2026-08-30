// MCP orchestrator mock — will be replaced with real MCP SDK clients (stdio/sse/http).
// Provides deterministic tool results for postgres/s3/fs based on keywords, with latency simulation.

export type MCPToolResult = {
  server: string;
  tool: string;
  latencyMs: number;
  status: "ok" | "error";
  data: unknown;
  html?: string; // hint for MessageItem rendering
};

const pgRows = [
  { id: 1042, email: "alice@example.com", created_at: "2024-10-20" },
  { id: 1041, email: "bob@example.com", created_at: "2024-10-19" },
  { id: 1040, email: "carol@example.com", created_at: "2024-10-18" },
  { id: 1039, email: "dave@example.com", created_at: "2024-10-17" },
  { id: 1038, email: "eve@example.com", created_at: "2024-10-16" },
];

export async function orchestrate(prompt: string, _context: { mode?: string; servers?: string[] } = {}): Promise<{
  text: string;
  html?: string;
  tools: MCPToolResult[];
}> {
  const lower = prompt.toLowerCase();
  const tools: MCPToolResult[] = [];

  if (lower.includes("postgres") || lower.includes("sql") || lower.includes("users") || lower.includes("select")) {
    tools.push({
      server: "postgres-mcp",
      tool: "query",
      latencyMs: 68,
      status: "ok",
      data: { rows: pgRows, query: "SELECT * FROM users ORDER BY created_at DESC LIMIT 5" },
      html: "pg",
    });
    return {
      text: `Here are the last 5 user registrations from **remote_db.users** (via \`postgres-mcp\`):\n\n| id | email | created_at |\n|---|---|---|\n| 1042 | alice@example.com | 2024-10-20 |\n| 1041 | bob@example.com | 2024-10-19 |\n| 1040 | carol@example.com | 2024-10-18 |\n| 1039 | dave@example.com | 2024-10-17 |\n| 1038 | eve@example.com | 2024-10-16 |\n\nTLS 1.3 • latency 68ms • \`SELECT * FROM users ORDER BY created_at DESC LIMIT 5\``,
      html: "pg",
      tools,
    };
  }

  if (lower.includes("s3") || lower.includes("bucket") || lower.includes("aws")) {
    tools.push({
      server: "aws-us-east",
      tool: "s3_list",
      latencyMs: 142,
      status: "ok",
      data: {
        buckets: [
          { name: "app-prod-assets", size: "1.2 GB", objects: 342 },
          { name: "logs-archive", size: "842 MB", objects: 12000 },
          { name: "ml-datasets", size: "4.1 GB", objects: 89 },
        ],
      },
      html: "aws",
    });
    return {
      text: `S3 buckets in **aws-us-east** (via \`aws-us-east\`):\n\n- \`app-prod-assets\` — 1.2 GB • 342 objects\n- \`logs-archive\` — 842 MB • 12k objects\n- \`ml-datasets\` — 4.1 GB • 89 objects\n\nTotal 3 buckets • 6.1 GB`,
      html: "aws",
      tools,
    };
  }

  if (lower.includes("/data") || lower.includes("file") || lower.includes("filesystem") || lower.includes("list files")) {
    tools.push({
      server: "local-fs",
      tool: "list",
      latencyMs: 12,
      status: "ok",
      data: { path: "/data", files: ["users.csv", "logs.json", "report.pdf", "dataset.parquet"] },
    });
    return {
      text: `Files in \`/data\` (via \`local-fs\`):\n\n- \`users.csv\` — 2.4 MB • 10,420 rows\n- \`logs.json\` — 842 KB • 3,291 entries\n- \`report.pdf\` — 1.1 MB\n- \`dataset.parquet\` — 4.7 MB\n\nSandboxed • read-only`,
      tools,
    };
  }

  if (lower.includes("/help")) {
    return {
      text: `Available commands: \`/sql\`, \`/s3\`, \`/fs\`, \`/github\`, \`/marketplace\`, \`/servers\`, \`/new\`, \`/clear\`, \`/export\`, \`/help\`, \`/todo\`, \`/mode\`, \`/model\`, \`/connect\`, \`/retry\`.\n\nTry: \`Query the remote PostgreSQL database for the last 5 user registrations\``,
      tools,
    };
  }

  // generic fallback — echo with context
  return {
    text: `Got it — **${prompt.slice(0, 120)}**\n\nHow would you like to proceed? You can ask me to query Postgres, list S3 buckets, or browse the marketplace.`,
    tools,
  };
}

// Streaming helper — splits text into ~24 chunks with jitter, matching client simulation in lib/store.ts:145
export async function* streamText(text: string, chunkCount = 24) {
  const chunkLen = Math.max(1, Math.ceil(text.length / chunkCount));
  let pos = 0;
  while (pos < text.length) {
    pos = Math.min(text.length, pos + chunkLen);
    yield text.slice(0, pos);
    // tiny jitter to mimic network; caller will await delay
  }
}
