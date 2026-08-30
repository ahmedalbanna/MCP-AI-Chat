import { test, expect } from "@playwright/test";

test.describe("chat api + streaming", () => {
  test("POST /api/chat non-stream mock response", async ({ request }) => {
    const r = await request.post("/api/chat", {
      data: { messages: [{ role: "user", content: "hello" }], stream: false },
    });
    expect(r.ok()).toBeTruthy();
    const j = await r.json();
    expect(j.role).toBe("assistant");
    expect(typeof j.content).toBe("string");
    expect(j.content.length).toBeGreaterThan(0);
  });

  test("POST /api/chat streaming SSE emits meta/delta/done", async ({ request }) => {
    const r = await request.post("/api/chat", {
      data: { messages: [{ role: "user", content: "List S3 buckets in aws-us-east" }], stream: true },
      headers: { Accept: "text/event-stream" },
    });
    expect(r.ok()).toBeTruthy();
    const ct = r.headers()["content-type"] || "";
    expect(ct).toContain("text/event-stream");
    const txt = await r.text();
    // must contain SSE framing
    expect(txt).toContain("event: meta");
    expect(txt).toContain("data:");
    // delta or done must contain content
    expect(txt).toMatch(/delta|content/);
    expect(txt).toContain("[DONE]");
  });

  test("POST /api/chat postgres prompt returns pg html hint", async ({ request }) => {
    const r = await request.post("/api/chat", {
      data: { messages: [{ role: "user", content: "Query the remote PostgreSQL database for the last 5 user registrations" }], stream: false },
    });
    expect(r.ok()).toBeTruthy();
    const j = await r.json();
    expect(j.html).toBe("pg");
  });

  test("slash /help returns command list", async ({ request }) => {
    const r = await request.post("/api/chat", {
      data: { messages: [{ role: "user", content: "/help" }], stream: false },
    });
    const j = await r.json();
    expect(j.content).toContain("/sql");
  });
});
