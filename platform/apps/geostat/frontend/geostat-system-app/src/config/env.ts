/**
 * Centralized environment configuration.
 * Set VITE_BASE_URL in your .env file — all endpoint URLs are derived from it.
 */
const BASE_URL =
  import.meta.env.VITE_BASE_URL || "http://localhost:8081";

export const ENV = {
  /** Base REST API URL */
  API_URL: `${BASE_URL}/api`,

  /** Authentication endpoint */
  API_SIGN_URL: `${BASE_URL}/sign`,

  /** WebSocket endpoint */
  WS_URL: `${BASE_URL}/ws`,

  /** Application title shown in the browser tab */
  APP_TITLE: import.meta.env.VITE_APP_TITLE || "My App",
} as const;