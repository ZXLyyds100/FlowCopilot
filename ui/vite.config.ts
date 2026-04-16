/// <reference types="vitest/config" />

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig(async ({ mode }) => {
  const plugins = [react()];

  if (mode !== "test") {
    const { default: tailwindcss } = await import("@tailwindcss/vite");
    plugins.push(tailwindcss());
  }

  return {
    plugins,
    test: {
      environment: "jsdom",
      globals: true,
      setupFiles: "./src/test/setup.ts",
    },
  };
});
