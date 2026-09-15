/**
 * MenuAccessContext — computed ONCE at the Menu level.
 *
 * Problem it solves:
 *   RenderMenuItem is a recursive component. Without context, every tree node
 *   would independently call useIsAdmin / usePermissions / getStoredUserId,
 *   re-computing identical auth state for every item in the menu.
 *
 * Pattern:
 *   MenuAccessProvider (in Menu.tsx)
 *     └── RenderMenuItem (reads context — zero auth hooks)
 *           └── RenderMenuItem (same, no extra computation)
 *                 └── ...
 *
 * RenderMenuItem only knows: "should I show this item?" — not how to decide it.
 */

import {
  createContext,
  useContext,
  useCallback,
  useMemo,
  type ReactNode,
} from "react";
import { canAccessItem } from "../authorization";
import { useUserContext } from "../hooks/useUserContext";
import { useSettings } from "../../context/SettingsContext";
import type { MenuItem, FolderMenuItem } from "../../types";

// ── Pure helpers ──────────────────────────────────────────────────────────────

function findParentChain(items: MenuItem[], targetItem: MenuItem): MenuItem[] {
  const chain: MenuItem[] = [];

  const traverse = (menuItems: MenuItem[]): boolean => {
    for (const menuItem of menuItems) {
      if (menuItem.nodeType === "DIRECTORY") {
        const folder = menuItem as FolderMenuItem;
        if (folder.children?.some((c) => c.slug === targetItem.slug)) {
          chain.push(menuItem);
          return true;
        }
        if (traverse(folder.children)) {
          chain.push(menuItem);
          return true;
        }
      }
    }
    return false;
  };

  traverse(items);
  return chain.reverse();
}

const isRestricted = (i: MenuItem): boolean =>
  i.accessControl?.userId != null ||
  i.accessControl?.roleId != null ||
  i.accessControl?.permissionId != null ||
  !!i.role;

// ── Context shape ─────────────────────────────────────────────────────────────

interface MenuAccessContextValue {
  /** True if the user is an admin — exposed so Menu can gate admin links. */
  isAdmin: boolean;
  /** Returns true if the given menu item should be visible to the current user. */
  shouldShowItem: (item: MenuItem) => boolean;
}

const MenuAccessContext = createContext<MenuAccessContextValue | null>(null);

// ── Provider ──────────────────────────────────────────────────────────────────

export const MenuAccessProvider = ({ children }: { children: ReactNode }) => {
  const user = useUserContext();
  const { settings } = useSettings();

  const shouldShowItem = useCallback(
    (item: MenuItem): boolean => {
      if (user.isAdmin) return true;

      const hasAccess = (i: MenuItem): boolean => canAccessItem(i, user);

      // Item carries its own restriction → check directly
      if (isRestricted(item)) return hasAccess(item);

      // No own restriction → check the nearest restricting ancestor
      if (!settings) return true;
      const parentChain = findParentChain(settings, item);
      const restrictingAncestor = parentChain.find(isRestricted);

      // No ancestor restricts it → admin-only by default
      if (!restrictingAncestor) return false;

      return hasAccess(restrictingAncestor);
    },
    [user, settings],
  );

  const value = useMemo(
    () => ({ isAdmin: user.isAdmin, shouldShowItem }),
    [user.isAdmin, shouldShowItem],
  );

  return (
    <MenuAccessContext.Provider value={value}>
      {children}
    </MenuAccessContext.Provider>
  );
};

// ── Consumer hook ─────────────────────────────────────────────────────────────

export const useMenuAccessContext = (): MenuAccessContextValue => {
  const ctx = useContext(MenuAccessContext);
  if (!ctx)
    throw new Error(
      "useMenuAccessContext must be used inside MenuAccessProvider",
    );
  return ctx;
};
