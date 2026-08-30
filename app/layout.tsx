import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "MCP AI Chat — Terminal",
  description: "Model Context Protocol AI Chat — Next.js Production Build",
  icons: { icon: "/favicon.svg" },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
