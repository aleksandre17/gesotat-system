/**
 * React Admin AuthProvider — authentication layer.
 *
 * Responsibilities:
 *   login / logout / checkAuth / checkError / getIdentity / getPermissions / canAccess
 *
 * Access-control decisions (canAccess) delegate to authorization/policies.
 * Token storage delegates to storage.ts.
 */

import { AuthProvider } from "react-admin";
import { jwtDecode } from "jwt-decode";
import { ENV } from "../../config";
import { httpClient } from "../../api";
import type { JwtPayload, UserAuth } from "../core";
import type { Permission } from "../core";
import { RESOURCE_POLICIES } from "../authorization";
import {
  clearSession,
  getToken,
  getRefreshToken,
  getStoredUser,
  getStoredPermissions,
  setTokens,
  setUser,
} from "./storage";

const apiUrl = ENV.API_SIGN_URL;
const meUrl = `${ENV.API_URL}/v1/users/me`;

// ── Profile API ───────────────────────────────────────────────────────────────

export interface ProfileDto {
  username: string; // read-only login name from users table
  displayName: string | null; // editable display name from profile table
  avatar: string | null;
}

export interface UpdateProfileDto {
  displayName?: string | null;
  avatar?: string | null;
}

export interface ChangePasswordDto {
  currentPassword: string;
  newPassword: string;
}

export const profileApi = {
  /** GET /api/users/me */
  get: (): Promise<ProfileDto> =>
    httpClient(meUrl).then((r) => r.json as ProfileDto),

  /** PATCH /api/users/me */
  update: (dto: UpdateProfileDto): Promise<ProfileDto> =>
    httpClient(meUrl, { method: "PATCH", body: dto }).then(
      (r) => r.json as ProfileDto,
    ),

  /** PATCH /api/users/me/password */
  changePassword: (dto: ChangePasswordDto): Promise<void> =>
    httpClient(`${meUrl}/password`, { method: "PATCH", body: dto }).then(
      () => undefined,
    ),
} as const;

// ── Token refresh ─────────────────────────────────────────────────────────────

const refreshAccessToken = async (): Promise<void> => {
  const storedRefreshToken = getRefreshToken();
  if (!storedRefreshToken) return Promise.reject();

  try {
    const { json } = await httpClient(`${apiUrl}/refresh`, {
      method: "POST",
      headers: { Authorization: `Bearer ${storedRefreshToken}` },
    });

    const { token: newToken, refreshToken: newRefreshToken } = json as {
      token: string;
      refreshToken: string;
    };

    setTokens(newToken, newRefreshToken);
    setUser(jwtDecode<JwtPayload>(newToken));
  } catch {
    clearSession();
    return Promise.reject();
  }
};

// ── AuthProvider ──────────────────────────────────────────────────────────────

export const signProvider: AuthProvider = {
  login: async ({ username, password }) => {
    const response = await fetch(`${apiUrl}/login`, {
      method: "POST",
      body: JSON.stringify({ username, password }),
      headers: { "Content-Type": "application/json" },
    });

    if (!response.ok) throw new Error("Login failed");

    const { token, refreshToken } = await response.json();
    setTokens(token, refreshToken);
    setUser(jwtDecode<JwtPayload>(token));
  },

  logout: () => {
    clearSession();
    return Promise.resolve();
  },

  checkError: (error) => {
    const { status } = error;
    if (status === 401 || status === 403) {
      clearSession();
      return Promise.reject();
    }
    return Promise.resolve();
  },

  checkAuth: () => {
    const token = getToken();
    if (!token) return Promise.reject();

    try {
      const decoded = jwtDecode(token);
      if (decoded.exp && decoded.exp < Date.now() / 1000) {
        return refreshAccessToken();
      }
    } catch {
      return Promise.reject();
    }

    return Promise.resolve();
  },

  /**
   * Route-level access control — called by React Admin before rendering
   * any Resource page (list, create, edit, delete, show).
   *
   * Policy source: authorization/policies.ts
   * No policy entry = accessible to all authenticated users.
   */
  canAccess: async ({ action, resource }) => {
    const required =
      RESOURCE_POLICIES[resource]?.[
        action as keyof (typeof RESOURCE_POLICIES)[string]
      ];
    if (!required) return true;
    return getStoredPermissions().includes(required);
  },

  /**
   * Returns UserAuth: { roles, permissions }
   *   roles       — coarse-grained role names  (e.g. "ADMIN", "DEPT_PRICES")
   *   permissions — fine-grained flat list from all roles
   *
   * Use typed hooks: useHasPermission / useHasAnyPermission / useHasRole
   */
  getPermissions: (): Promise<UserAuth> => {
    const payload = getStoredUser();
    if (!payload) return Promise.reject();

    try {
      const roles = payload.roles.roles.map((r) => r.name);
      const roleIds = payload.roles.roles.map((r) => r.id);
      const permissions = payload.roles.roles.flatMap((r) =>
        r.permissions.map((p) => p.name as Permission),
      );
      const permissionIds = payload.roles.roles.flatMap((r) =>
        r.permissions.map((p) => p.id),
      );
      return Promise.resolve({ roles, roleIds, permissions, permissionIds });
    } catch {
      return Promise.reject();
    }
  },

  getIdentity: async () => {
    try {
      const { json } = await httpClient(meUrl);
      const data = json as ProfileDto;
      return {
        id: data.username,
        fullName: data.displayName ?? data.username,
        avatar: data.avatar ?? undefined,
        displayName: data.displayName,
      };
    } catch {
      const payload = getStoredUser();
      if (!payload) return Promise.reject();
      return {
        id: payload.sub,
        fullName: payload.name ?? payload.sub,
        avatar: payload.avatar,
        displayName: null,
      };
    }
  },
};
