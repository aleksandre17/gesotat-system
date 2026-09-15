import { usePermissions } from "react-admin";
import type { Permission } from "../core/permissions";
import type { UserAuth } from "../core/types";

/**
 * Returns true only if the current user has ALL of the given permissions.
 *
 * @example
 *   const canWrite = useHasPermission(PERMISSIONS.resource.write);
 */
export const useHasPermission = (...required: Permission[]): boolean => {
  const { permissions: auth } = usePermissions<UserAuth>();
  if (!auth?.permissions) return false;
  return required.every((p) => auth.permissions.includes(p));
};

/**
 * Returns true if the current user has AT LEAST ONE of the given permissions.
 *
 * @example
 *   const isAdmin = useHasAnyPermission(PERMISSIONS.admin.manageUsers, PERMISSIONS.resource.write);
 */
export const useHasAnyPermission = (...required: Permission[]): boolean => {
  const { permissions: auth } = usePermissions<UserAuth>();
  if (!auth?.permissions) return false;
  return required.some((p) => auth.permissions.includes(p));
};
