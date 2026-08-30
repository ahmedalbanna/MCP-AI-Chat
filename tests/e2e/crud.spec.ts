import { test, expect } from "@playwright/test";

test.describe("CRUD api", () => {
  test("chats CRUD + messages", async ({ request }) => {
    const created = await request.post("/api/chats", { data: { title: "e2e chat" } });
    if (created.status() === 503) {
      // mock mode without DB — verify mock note
      const j = await created.json();
      expect(j.persistence).toBe("mock");
      const list = await request.get("/api/chats");
      expect(list.ok()).toBeTruthy();
      const lj = await list.json();
      expect(lj.persistence).toBe("mock");
      return;
    }
    expect(created.ok()).toBeTruthy();
    const j = await created.json();
    const chat = j.chat || j;
    expect(chat.id).toBeDefined();

    const msg = await request.post(`/api/chats/${chat.id}/messages`, { data: { role: "user", content: "hello e2e" } });
    expect(msg.ok() || msg.status() === 503).toBeTruthy();

    const list = await request.get("/api/chats");
    expect(list.ok()).toBeTruthy();
    const lj = await list.json();
    expect(lj.chats).toBeDefined();

    const del = await request.delete(`/api/chats/${chat.id}`);
    expect(del.ok()).toBeTruthy();
  });

  test("servers + providers + modes read", async ({ request }) => {
    const s = await request.get("/api/servers");
    expect(s.ok()).toBeTruthy();
    const sj = await s.json();
    expect(Array.isArray(sj.servers) || Array.isArray(sj)).toBeTruthy();

    const p = await request.get("/api/providers");
    expect(p.ok()).toBeTruthy();

    const m = await request.get("/api/modes");
    expect(m.ok()).toBeTruthy();
  });
});
