import { canAccessItem } from "../authorization";
import type { AccessibleItem } from "../authorization";
import { useUserContext } from "./useUserContext";

export type { AccessibleItem };

/**
 * React hook wrapper around `canAccessItem`.
 * Used in AccessUploadPage to block rendering the route entirely.
 *
 * For the menu tree, use MenuAccessContext instead (computed once at the top).
 */
export const useCanAccessPage = (item: AccessibleItem): boolean => {
  const user = useUserContext();
  return canAccessItem(item, user);
};
