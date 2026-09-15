import { useMemo } from "react";
import { usePermissions } from "react-admin";
import { useIsAdmin } from "./useIsAdmin";
import { getStoredUserId } from "../authorization";
import type { UserContext } from "../authorization";
import type { UserAuth } from "../core";

/**
 * Assembles the current user's auth context for canAccessItem.
 *
 * Single source of truth shared by:
 *   - useCanAccessPage  (route-level guard)
 *   - MenuAccessProvider (menu visibility, computed once at the top)
 */
export const useUserContext = (): UserContext => {
  const isAdmin = useIsAdmin();
  const { permissions: auth } = usePermissions<UserAuth>();
  const userId = useMemo(() => getStoredUserId(), []);

  return useMemo(
    () => ({
      isAdmin,
      userId,
      roles: auth?.roles ?? [],
      roleIds: auth?.roleIds ?? [],
      permissions: auth?.permissions ?? [],
      permissionIds: auth?.permissionIds ?? [],
    }),
    [isAdmin, userId, auth],
  );
};
