import { test, expect } from "@playwright/test";

test.describe("health + mcp", () => {
  test("GET /api/health returns ok", async ({ request }) => {
    const r = await request.get("/api/health");
    expect(r.ok()).toBeTruthy();
    const j = await r.json();
    expect(j.status).toBe("ok");
    expect(j.version).toBeDefined();
  });

  test("GET /api/mcp/health returns servers mock fallback", async ({ request }) => {
    const r = await request.get("/api/mcp/health");
    expect(r.ok()).toBeTruthy();
    const j = await r.json();
    expect(j.status).toBe("ok");
    expect(j.mcp.servers.length).toBeGreaterThanOrEqual(3);
    expect(j.mcp.servers.some((s: any) => s.name === "postgres-mcp")).toBeTruthy();
  });

  test("GET /api/chat meta includes streaming+providers", async ({ request }) => {
    const r = await request.get("/api/chat");
    expect(r.ok()).toBeTruthy();
    const j = await r.json();
    expect(j.providers).toContain("anthropic");
    expect(j.streaming).toBeDefined();
  });

  test("GET /api/market + /api/modes + /api/chats healthy", async ({ request }) => {
    for (const p of ["/api/market", "/api/modes", "/api/chats"]) {
      const r = await request.get(p);
      expect(r.ok(), p).toBeTruthy();
    }
  });
});
