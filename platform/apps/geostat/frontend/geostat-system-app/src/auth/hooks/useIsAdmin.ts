import { useHasAnyPermission } from "./useHasPermission";
import { PERMISSIONS } from "../core/permissions";

/**
 * Single source of truth for "is the current user an admin?".
 *
 * An admin is anyone who holds at least one panel-management permission.
 * Admins bypass all department-level access restrictions.
 *
 * Use this instead of calling useHasAnyPermission with 3 args at every site.
 */
export const useIsAdmin = (): boolean =>
  useHasAnyPermission(
    PERMISSIONS.admin.manageUsers,
    PERMISSIONS.admin.manageRoles,
    PERMISSIONS.admin.managePermissions,
  );
