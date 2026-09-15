/**
 * Token storage helpers — thin wrappers around localStorage.
 *
 * All authentication state lives here so the rest of the auth system
 * has a single place to read/write session data.
 */

import type { JwtPayload } from "../core";

// ── Keys ─────────────────────────────────────────────────────────────────────

const KEY_TOKEN = "token";
const KEY_REFRESH_TOKEN = "refreshToken";
const KEY_USER = "user";

// ── Getters ──────────────────────────────────────────────────────────────────

export const getToken = (): string | null => localStorage.getItem(KEY_TOKEN);

export const getRefreshToken = (): string | null =>
  localStorage.getItem(KEY_REFRESH_TOKEN);

export const getStoredUser = (): JwtPayload | null => {
  try {
    const raw = localStorage.getItem(KEY_USER);
    return raw ? (JSON.parse(raw) as JwtPayload) : null;
  } catch {
    return null;
  }
};

/** Flat permission string[] extracted directly from the stored JWT. */
export const getStoredPermissions = (): string[] => {
  try {
    const payload = getStoredUser();
    if (!payload) return [];
    return payload.roles.roles.flatMap((r) => r.permissions.map((p) => p.name));
  } catch {
    return [];
  }
};

// ── Setters ──────────────────────────────────────────────────────────────────

export const setTokens = (token: string, refreshToken: string): void => {
  localStorage.setItem(KEY_TOKEN, token);
  localStorage.setItem(KEY_REFRESH_TOKEN, refreshToken);
};

export const setUser = (payload: JwtPayload): void =>
  localStorage.setItem(KEY_USER, JSON.stringify(payload));

// ── Clear ────────────────────────────────────────────────────────────────────

export const clearSession = (): void => {
  localStorage.removeItem(KEY_TOKEN);
  localStorage.removeItem(KEY_REFRESH_TOKEN);
  localStorage.removeItem(KEY_USER);
};
