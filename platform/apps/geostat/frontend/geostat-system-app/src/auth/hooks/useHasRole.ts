import { usePermissions } from "react-admin";
import type { Role } from "../core/roles";
import type { UserAuth } from "../core/types";

/**
 * Returns true if the current user holds AT LEAST ONE of the given roles.
 *
 * Use for coarse-grained checks (e.g. "is this user a department member?").
 * Prefer `useHasPermission` for feature-level access decisions.
 *
 * @example
 *   const isAdmin = useHasRole(ROLES.ADMIN);
 *   const isDeptUser = useHasRole(ROLES.DEPT_PRICES, ROLES.DEPT_IT);
 */
export const useHasRole = (...required: (Role | string)[]): boolean => {
  const { permissions: auth } = usePermissions<UserAuth>();
  if (!auth?.roles) return false;
  return required.some((r) => auth.roles.includes(r));
};
