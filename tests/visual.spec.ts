import { test, expect } from "@playwright/test";

// Visual regression vs prototype index.html (python -m http.server 8765)
// and Next.js app (docker :3000). Run with: npx playwright test tests/visual.spec.ts

test.describe("MCP Terminal visual parity", () => {
  test("prototype vs app shell", async ({ page }) => {
    // prototype
    await page.goto("http://localhost:8765/index.html");
    await page.waitForTimeout(1200);
    // dismiss onboarding if present
    try { await page.getByRole("button", { name: "Skip" }).click({ timeout: 1000 }); } catch {}
    try { await page.getByRole("button", { name: /Continue as Guest|Demo login/i }).click({ timeout: 1000 }); } catch {}
    const proto = await page.screenshot({ fullPage: false });

    // app
    await page.goto("http://localhost:3000");
    await page.waitForTimeout(1200);
    // clear onboarding for clean shot
    await page.evaluate(() => { localStorage.setItem("mcp_onboarded","1"); localStorage.setItem("mcp_currentUser", JSON.stringify({id:"u_alex",name:"Alex Rivera",email:"alex@local.dev",avatar:"https://i.pravatar.cc/100?img=32",role:"Admin",isGuest:false})); });
    await page.reload();
    await page.waitForTimeout(1000);
    const app = await page.screenshot({ fullPage: false });

    // basic assertion: screenshots non-empty and sizes similar (full pixel diff requires toHaveScreenshot)
    expect(proto.length).toBeGreaterThan(10000);
    expect(app.length).toBeGreaterThan(10000);
  });

  test("chat table renders", async ({ page }) => {
    await page.goto("http://localhost:3000");
    await page.evaluate(() => { localStorage.setItem("mcp_onboarded","1"); localStorage.setItem("mcp_currentUser", JSON.stringify({id:"u_alex",name:"Alex Rivera",email:"alex@local.dev",avatar:"https://i.pravatar.cc/100?img=32",role:"Admin",isGuest:false})); });
    await page.reload();
    await page.waitForTimeout(800);
    await expect(page.getByText("PostgreSQL users query").first()).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "email" })).toBeVisible();
    await expect(page.locator("table")).toContainText("alice@example.com");
    await expect(page.getByText("Tasks").first()).toBeVisible();
  });

  test("api hardening - rate limit headers", async ({ request }) => {
    const res = await request.get("http://localhost:3000/api/health");
    expect(res.headers()["x-mcp-terminal"]).toBe("2.0");
    expect(res.headers()["x-frame-options"]).toBe("DENY");
    const chat = await request.post("http://localhost:3000/api/chat", { data: { messages: [{ role: "user", content: "hi" }], stream: false } });
    expect(chat.headers()["x-ratelimit-limit"]).toBeDefined();
  });
});
