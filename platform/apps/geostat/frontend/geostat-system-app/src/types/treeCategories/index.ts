import type { AccessControl } from "../../auth/authorization";

export type { AccessControl };

type NodeType = "DIRECTORY" | "PAGE";

export interface BaseMenuItem {
  name: string;
  isFolder: boolean;
  level: number;
  slug: string;
  icon: string;
  nodeType: NodeType;
  /**
   * Nested access-control block — the backend returns this as a single object.
   * Null / absent means the item is public (or inherits from its parent folder).
   */
  accessControl?: AccessControl | null;
  /**
   * Role name string returned by the settings/menu API (e.g. "DEPT_PRICES").
   * Used as a name-based access-check fallback and for ancestor inheritance.
   */
  role?: string | null;
}

export interface FolderMenuItem extends BaseMenuItem {
  isFolder: true;
  description: string;
  children: MenuItem[];
}

export interface LeafMenuItem extends BaseMenuItem {
  isFolder: false;
  resource: string;
  metaTitle: string;
  metaDescription: string;
}

export type MenuItem = FolderMenuItem | LeafMenuItem;

export type MenuItems = MenuItem[];
