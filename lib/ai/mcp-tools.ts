import { tool } from "ai";
import { z } from "zod";
import { callTool } from "../mcp/registry";

// Build AI SDK tools from MCP registry entries.
// For mock fallback when registry empty, expose deterministic postgres/s3/fs tools matching lib/mcp.ts.

export async function buildMCPTools(servers: { name?: string; server?: string; tools: { name: string }[] }[]): Promise<Record<string, any>> {
  const tools: Record<string, any> = {};

  if (!servers || servers.length === 0) return tools;

  for (const srv of servers) {
    const srvName = (srv as any).name || (srv as any).server || "mcp";
    for (const t of srv.tools || []) {
      const toolKey = `${srvName}_${t.name}`.replace(/[^a-zA-Z0-9_]/g, "_");
      // Avoid duplicate keys
      if (tools[toolKey]) continue;

      // Generic parameters — allow any JSON; server will validate.
      // Use zod schema that accepts any object.
      tools[toolKey] = tool({
        description: `MCP tool ${t.name} on server ${srvName}`,
        parameters: z.object({ input: z.string().optional(), query: z.string().optional(), path: z.string().optional() }).passthrough(),
        execute: async (args: any) => {
          const res = await callTool(srvName, t.name, args || {});
          if (!res.ok) return { error: res.error, latencyMs: res.latency };
          return { result: res.data, latencyMs: res.latency };
        },
      } as any);
    }
  }

  return tools;
}

export function buildMockToolsFallback(): Record<string, any> {
  // Deterministic mocks identical to lib/mcp.ts orchestrator — exposed as AI tools so model can call them
  return {
    postgres_query: tool({
      description: "Query the remote PostgreSQL database (postgres-mcp). Use for users table queries.",
      parameters: z.object({ query: z.string().describe("SQL query, e.g. SELECT * FROM users ORDER BY created_at DESC LIMIT 5") }),
      execute: async ({ query }: { query: string }) => {
        const rows = [
          { id: 1042, email: "alice@example.com", created_at: "2024-10-20" },
          { id: 1041, email: "bob@example.com", created_at: "2024-10-19" },
          { id: 1040, email: "carol@example.com", created_at: "2024-10-18" },
          { id: 1039, email: "dave@example.com", created_at: "2024-10-17" },
          { id: 1038, email: "eve@example.com", created_at: "2024-10-16" },
        ];
        return { rows, query, latencyMs: 68, tls: "TLS 1.3" };
      },
    } as any),
    s3_list: tool({
      description: "List S3 buckets on aws-us-east (aws-us-east server).",
      parameters: z.object({ prefix: z.string().optional() }),
      execute: async () => {
        return {
          buckets: [
            { name: "app-prod-assets", size: "1.2 GB", objects: 342 },
            { name: "logs-archive", size: "842 MB", objects: 12000 },
            { name: "ml-datasets", size: "4.1 GB", objects: 89 },
          ],
          latencyMs: 142,
        };
      },
    } as any),
    fs_list: tool({
      description: "List files in local filesystem /data sandbox (local-fs server).",
      parameters: z.object({ path: z.string().describe("path like /data") }),
      execute: async ({ path }: { path: string }) => {
        return { path: path || "/data", files: ["users.csv", "logs.json", "report.pdf", "dataset.parquet"], latencyMs: 12 };
      },
    } as any),
  };
}
