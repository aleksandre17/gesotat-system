import { useHasAnyPermission } from "./useHasPermission";
import { PERMISSIONS } from "../core/permissions";

/**
 * Returns per-section visibility flags for the admin panel menu.
 *
 * Each flag maps to the permission required by that resource's route policy.
 * A user may hold only a subset — e.g. canManageUsers without canManageRoles.
 *
 * Consumed by Menu.tsx only. Keep component JSX out of auth hooks.
 */
export interface AdminMenuAccess {
  canManageUsers: boolean;
  canManageRoles: boolean;
  canManagePermissions: boolean;
  canManagePages: boolean;
}

export const useAdminMenuAccess = (): AdminMenuAccess => ({
  canManageUsers: useHasAnyPermission(PERMISSIONS.admin.manageUsers),
  canManageRoles: useHasAnyPermission(PERMISSIONS.admin.manageRoles),
  canManagePermissions: useHasAnyPermission(
    PERMISSIONS.admin.managePermissions,
  ),
  canManagePages: useHasAnyPermission(
    PERMISSIONS.resource.write,
    PERMISSIONS.resource.delete,
  ),
});
