export type NodeType = "DIRECTORY" | "PAGE";

export interface PageRecord {
  id: number;
  name: string;
  nodeType: NodeType;
  slug: string;
  level: number;
  icon?: string | null;
  parentId?: number | null;
  orderIndex: number;
  description?: string | null;
  resource?: string | null;
  metaTitle?: string | null;
  metaDescription?: string | null;
  metaDatabaseType?: string | null;
  metaDatabaseUrl?: string | null;
  metaDatabaseUser?: string | null;
  metaDatabasePassword?: string | null;
  metaDatabaseName?: string | null;
  accessControl?: {
    userId?: number | null;
    roleId?: number | null;
    permissionId?: number | null;
  } | null;
  /** Role name string — returned by the settings/menu API (e.g. "DEPT_PRICES"). */
  role?: string | null;
  children?: PageRecord[];
}

export interface TreeNode {
  id: number;
  name: string;
  title: string;
  parentId: number | null;
  orderIndex: number;
  expanded: boolean;
  children: TreeNode[];
  nodeType: NodeType;
  accessControl?: {
    userId?: number | null;
    roleId?: number | null;
    permissionId?: number | null;
  } | null;
  role?: string | null;
}

export interface PageReorder {
  pageId: number;
  parentId: number | null;
  orderIndex: number;
}
