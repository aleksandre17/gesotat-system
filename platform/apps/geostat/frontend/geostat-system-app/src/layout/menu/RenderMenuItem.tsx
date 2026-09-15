import { MenuItemLink } from "react-admin";
import { useLocation } from "react-router-dom";
import { useSettings } from "../../context/SettingsContext";
import SubMenu from "./SubMenu";
import { useState, useMemo, memo, useCallback, useEffect } from "react";
import type { FolderMenuItem, MenuItem } from "../../types";
import { useMenuAccessContext } from "../../auth/guards";
import { IconLoader } from "../../components";

// ── Visibility helpers ────────────────────────────────────────────────────────

/**
 * Returns true only if the item has at least one accessible leaf descendant.
 * Prevents empty directories from appearing in the menu.
 */
function hasVisibleLeaf(
  item: MenuItem,
  shouldShowItem: (item: MenuItem) => boolean,
): boolean {
  if (item.nodeType !== "DIRECTORY") return shouldShowItem(item);
  const folder = item as FolderMenuItem;
  return (
    folder.children?.some((child) => hasVisibleLeaf(child, shouldShowItem)) ??
    false
  );
}

// ── Path helpers ──────────────────────────────────────────────────────────────

function findItemPath(
  items: MenuItem[],
  targetSlug: string,
  path: string[] = [],
): string[] | null {
  for (const item of items) {
    const currentPath = [...path, item.slug];
    if (item.slug === targetSlug) return currentPath;
    if (item.nodeType === "DIRECTORY") {
      const folder = item as FolderMenuItem;
      const result = findItemPath(folder.children, targetSlug, currentPath);
      if (result) return result;
    }
  }
  return null;
}

// ── Component ─────────────────────────────────────────────────────────────────

const RenderMenuItem = ({
  item,
  dense,
}: {
  item: MenuItem;
  dense?: boolean;
}) => {
  // undefined = not yet set (auto-expand), false = user closed, true = open
  const [state, setState] = useState<Record<string, boolean | undefined>>({});
  const { settings } = useSettings();
  const { shouldShowItem } = useMenuAccessContext();
  const { pathname } = useLocation();

  const getIconComponent = useCallback(
    (iconName?: string) => <IconLoader iconName={iconName} />,
    [],
  );

  // Auto-expand the active path on navigation
  useEffect(() => {
    if (!settings || !Array.isArray(settings)) return;
    const segments = pathname.split("/").filter(Boolean);
    if (segments.length === 0) return;

    const targetSlug = segments[segments.length - 1];
    const fullPathArray = findItemPath(settings, targetSlug);

    if (fullPathArray) {
      setState((prev) => {
        const next = { ...prev };
        fullPathArray.forEach((slug) => {
          // Only auto-expand if the user hasn't explicitly collapsed it.
          // Prev[slug] can be undefined (not yet set) → expand by default.
          // Prev[slug] === false means the user closed it → respect that.
          if (slug !== targetSlug && prev[slug] !== false) {
            next[slug] = true;
          }
        });
        return next;
      });
    }
  }, [pathname, settings]);

  const handleToggle = (menu: string) =>
    setState((s) => ({ ...s, [menu]: !s[menu] }));

  const fullPath = useMemo(
    () =>
      item.nodeType !== "DIRECTORY" && Array.isArray(settings)
        ? (findItemPath(settings, item.slug) ?? []).join("/")
        : "",
    [item.nodeType, item.slug, settings],
  );

  if (item.nodeType === "DIRECTORY") {
    const folderItem = item as FolderMenuItem;
    const hasVisibleChildren = folderItem.children.some((child) =>
      hasVisibleLeaf(child, shouldShowItem),
    );
    if (!hasVisibleChildren) return null;

    return (
      <SubMenu
        key={item.slug}
        handleToggle={() => handleToggle(item.slug)}
        isOpen={state[item.slug] ?? false}
        name={item.name}
        icon={getIconComponent(item.icon)}
        dense={dense ?? false}
      >
        {folderItem.children.map((child) => (
          <RenderMenuItem dense={dense} key={child.slug} item={child} />
        ))}
      </SubMenu>
    );
  }

  if (!shouldShowItem(item)) return null;

  return (
    <MenuItemLink
      key={item.slug}
      to={`/${fullPath}`}
      state={{ _scrollToTop: true }}
      primaryText={item.name}
      leftIcon={getIconComponent(item.icon)}
      dense={dense}
    />
  );
};

export default memo(RenderMenuItem, (prev, next) => {
  const prevFolder = prev.item as FolderMenuItem;
  const nextFolder = next.item as FolderMenuItem;
  return (
    prev.item.slug === next.item.slug &&
    prev.item.nodeType === next.item.nodeType &&
    prev.item.name === next.item.name &&
    prev.item.icon === next.item.icon &&
    JSON.stringify(prevFolder.children) === JSON.stringify(nextFolder.children)
  );
});
