/**
 * Pure item-level access logic — no React, no hooks.
 *
 * Safe to use in callbacks, loops, and tree traversals.
 * Hook wrapper lives in hooks/useCanAccessPage.ts.
 */

import type { JwtPayload } from "../core";

// ── Stored user ID ────────────────────────────────────────────────────────────

/** Reads the current user's numeric `userId` from the stored JWT claim. */
export const getStoredUserId = (): number | undefined => {
  try {
    const raw = localStorage.getItem("user");
    if (!raw) return undefined;
    return (JSON.parse(raw) as JwtPayload).roles?.userId;
  } catch {
    return undefined;
  }
};

// ── AccessControl ─────────────────────────────────────────────────────────────

/**
 * Nested access-control block returned by the backend.
 * Mirrors the server DTO: { userId, roleId, permissionId }.
 */
export interface AccessControl {
  userId?: number | null;
  roleId?: number | null;
  permissionId?: number | null;
}

// ── UserContext ───────────────────────────────────────────────────────────────

/**
 * The current user's auth state, passed to canAccessItem.
 * Extracted into one object to avoid a 7-argument function signature.
 * React hook: useUserContext (hooks/useUserContext.ts).
 */
export interface UserContext {
  isAdmin: boolean;
  userId: number | undefined;
  roles: string[];
  roleIds: number[];
  permissions: string[];
  permissionIds: number[];
}

// ── AccessibleItem ────────────────────────────────────────────────────────────

/**
 * Minimum shape required by `canAccessItem`.
 *
 * Access priority (first match wins):
 *  1. isAdmin                    → always allow
 *  2. accessControl.userId       → direct ownership
 *  3. accessControl.permissionId → user must hold this permission
 *  4. accessControl.roleId       → user must carry this role
 *  5. role (string)              → name-based role check (settings API fallback)
 *  6. none set                   → deny (admin-only by default)
 */
export interface AccessibleItem {
  accessControl?: AccessControl | null;
  /** Role name string — settings/menu API may include it for name-based checks. */
  role?: string | null;
}

// ── canAccessItem ─────────────────────────────────────────────────────────────

/**
 * Pure access check — call anywhere without React context.
 *
 * @param item  The page/menu item to check.
 * @param user  The current user's auth context.
 */
export const canAccessItem = (
  item: AccessibleItem,
  user: UserContext,
): boolean => {
  if (user.isAdmin) return true;

  const ac = item.accessControl;
  if (ac == null && item.role == null) return false;

  if (ac?.userId != null) return user.userId === ac.userId;
  if (ac?.permissionId != null)
    return user.permissionIds.includes(ac.permissionId);
  if (ac?.roleId != null) return user.roleIds.includes(ac.roleId);
  if (item.role) return user.roles.includes(item.role);

  return false;
};
