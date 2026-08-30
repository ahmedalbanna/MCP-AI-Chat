export const uid = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
export const fmtSize = (b: number) => {
  if (b < 1024) return b + " B";
  if (b < 1024 * 1024) return (b / 1024).toFixed(1) + " KB";
  return (b / (1024 * 1024)).toFixed(1) + " MB";
};
export const esc = (s: string) => String(s || "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c] as string));
export const timeNow = () => new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
export const cn = (...c: (string | false | null | undefined)[]) => c.filter(Boolean).join(" ");
