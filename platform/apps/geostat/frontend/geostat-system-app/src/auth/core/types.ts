/** Auth domain types — shared across all auth layers. Zero external dependencies. */

import type { Permission } from "./permissions";

export interface PermissionRecord {
  id: number;
  name: string;
}

export interface RoleRecord {
  id: number;
  name: string;
  permissions: PermissionRecord[];
}

/** Shape of the `roles` claim inside the decoded JWT. */
export interface JwtRolesClaim {
  userId: number;
  roles: RoleRecord[];
}

/** Full decoded JWT payload structure. */
export interface JwtPayload {
  sub: string;
  name?: string;
  avatar?: string;
  roles: JwtRolesClaim;
  exp?: number;
  iat?: number;
}

/**
 * What `signProvider.getPermissions()` returns — and what `usePermissions<UserAuth>()` gives.
 *
 * Keeps roles (coarse-grained) and permissions (fine-grained) together
 * so a single hook call gives you everything you need for access decisions.
 */
export interface UserAuth {
  /** Role names the current user holds, e.g. ["ADMIN", "DEPT_PRICES"] */
  roles: string[];
  /** Role IDs — used when a menu item only carries roleId, not the name string. */
  roleIds: number[];
  /** Flat permission strings derived from all roles, e.g. ["WRITE_RESOURCE", "MANAGE_USERS"] */
  permissions: Permission[];
  /** Flat permission IDs derived from all roles — used when an item carries only permissionId. */
  permissionIds: number[];
}
