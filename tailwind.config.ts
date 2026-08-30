import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./lib/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#2563eb",
        "primary-2": "#1d4ed8",
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
      },
      boxShadow: {
        xs: "0 1px 2px rgba(15,23,42,.05)",
        sm: "0 1px 3px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04)",
      },
    },
  },
  plugins: [],
};
export default config;
