import type { AccessControl } from "../../auth";

/**
 * A single-page item from the settings tree.
 * Access fields are resolved (inherited from the nearest restricting ancestor)
 * before this item is passed to AccessUploadPage and useCanAccessPage.
 */
export interface PageItem {
  id: number;
  slug: string;
  name: string;
  nodeType: string;
  /** Resolved access control — may be inherited from a parent folder. */
  accessControl?: AccessControl | null;
  /** Role name string — may be inherited from a parent folder. */
  role?: string | null;
  metaDatabaseType: string;
  metaDatabaseName: string;
  metaDatabaseUrl: string;
  metaDatabaseUser: string;
  metaDatabasePassword: string;
  children?: PageItem[];
}

export interface LeafPath {
  path: string;
  item: PageItem;
}

/**
 * Flattens the settings tree into a list of leaf PAGE nodes.
 *
 * Access control inheritance rule: a leaf page inherits the `accessControl`
 * and `role` of the nearest ancestor that defines them. This mirrors the
 * menu visibility logic in MenuAccessContext.
 */
export function getLeafPaths(
  items: PageItem[],
  path: string[] = [],
): LeafPath[] {
  const result: LeafPath[] = [];
  const seen = new Set<string>();

  const traverse = (
    nodes: PageItem[],
    segments: string[],
    inheritedAccessControl?: AccessControl | null,
    inheritedRole?: string | null,
  ) => {
    for (const node of nodes) {
      const currentPath = [...segments, node.slug].join("/");
      const effectiveAccessControl =
        node.accessControl ?? inheritedAccessControl;
      const effectiveRole = node.role ?? inheritedRole;

      if (node.nodeType !== "DIRECTORY") {
        if (!seen.has(currentPath)) {
          seen.add(currentPath);
          result.push({
            path: currentPath,
            item: {
              ...node,
              accessControl: effectiveAccessControl,
              role: effectiveRole,
            },
          });
        }
      } else if (node.children) {
        traverse(
          node.children,
          [...segments, node.slug],
          effectiveAccessControl,
          effectiveRole,
        );
      }
    }
  };

  traverse(items, path);
  return result;
}
