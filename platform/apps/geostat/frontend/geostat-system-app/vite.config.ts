import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { nodePolyfills } from "vite-plugin-node-polyfills";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [
    react(),

    nodePolyfills({
      // Only polyfill what's actually needed (xlsx needs Buffer)
      include: ["buffer"],
      globals: { global: true, Buffer: true },
    }),
  ],
  optimizeDeps: {
    include: [
      "react-is",
      "prop-types",
      "hoist-non-react-statics",
      "@emotion/react",
      "@emotion/styled",
      "@emotion/cache",
    ],
  },
  server: {
    host: true,
    historyApiFallback: true,
  },
  resolve: {
    // Force single instances — prevents "loaded multiple times" warnings
    // from libraries (e.g. react-sortable-tree) that bundle their own copies
    dedupe: ["react", "react-dom", "@emotion/react", "@emotion/styled"],
    alias: {
      "@mui-icons": path.resolve(
        __dirname,
        "node_modules/@mui/icons-material/esm",
      ),
    },
  },
  build: {
    target: "esnext",
    commonjsOptions: {
      transformMixedEsModules: true,
    },
    sourcemap: mode === "development",
    rollupOptions: {
      output: {
        // Mirror src/ folder structure in dist/assets/
        chunkFileNames(chunkInfo) {
          if (chunkInfo.name.startsWith("vendor-")) {
            return "assets/vendor/[name]-[hash].js";
          }
          const id = chunkInfo.facadeModuleId;
          if (id) {
            const srcIdx = id.indexOf("/src/");
            if (srcIdx !== -1) {
              const rel = id.slice(srcIdx + 5); // strip leading /src/
              const dir = rel.includes("/")
                ? rel.slice(0, rel.lastIndexOf("/"))
                : "";
              return dir
                ? `assets/${dir}/[name]-[hash].js`
                : "assets/[name]-[hash].js";
            }
          }
          return "assets/[name]-[hash].js";
        },
        manualChunks(id) {
          if (!id.includes("node_modules")) return;
          // Truly React-independent — safe to split
          if (id.includes("/xlsx/")) return "vendor-xlsx";
          if (id.includes("@stomp/") || id.includes("sockjs-client"))
            return "vendor-ws";
          // Everything else (React + all dependents) → one cacheable chunk
          // Splitting React-dependent libs causes init-order errors in the browser
          return "vendor-app";
        },
      },
    },
  },
  base: "/",
}));
