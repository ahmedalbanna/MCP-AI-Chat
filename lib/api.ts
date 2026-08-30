// Client helper for DB-backed APIs — graceful fallback to local state when DB absent.
export async function apiFetch<T>(path: string, opts: RequestInit = {}): Promise<{ ok: boolean; data?: T; error?: string; status: number }> {
  try {
    const res = await fetch(path, { ...opts, headers: { "Content-Type": "application/json", ...(opts.headers as any) } });
    const status = res.status;
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      return { ok: false, error: (err as any).error || res.statusText, status };
    }
    const data = (await res.json()) as T;
    return { ok: true, data, status };
  } catch (e) {
    return { ok: false, error: (e as Error).message, status: 0 };
  }
}

export async function isDbEnabled(): Promise<boolean> {
  try {
    const r = await fetch("/api/health", { cache: "no-store" });
    const j = await r.json();
    return j.persistence === "postgres";
  } catch {
    return false;
  }
}
