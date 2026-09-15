import type { TreeNode, PageRecord, PageReorder } from "../types";

export const buildTree = (
  data: PageRecord[],
  expandedIds: Set<number> = new Set(),
): TreeNode[] => {
  const buildNode = (
    item: PageRecord,
    inheritedAccessControl: PageRecord["accessControl"],
    inheritedRole: string | null | undefined,
  ): TreeNode => {
    const effectiveAccessControl =
      item.accessControl ?? inheritedAccessControl ?? null;
    const effectiveRole = item.role ?? inheritedRole ?? null;
    return {
      id: item.id,
      name: item.name,
      title: item.name,
      parentId: item.parentId ?? null,
      orderIndex: item.orderIndex ?? 0,
      expanded: expandedIds.has(item.id),
      children:
        item.children?.map((child) =>
          buildNode(child, effectiveAccessControl, effectiveRole),
        ) ?? [],
      nodeType: item.nodeType,
      accessControl: effectiveAccessControl,
      role: effectiveRole,
    };
  };
  return data.map((item) => buildNode(item, null, null));
};

export const updatePositions = (
  nodes: TreeNode[],
  parentId: number | null = null,
): TreeNode[] =>
  nodes.map((node, index) => ({
    ...node,
    parentId,
    orderIndex: index,
    children: updatePositions(node.children, node.id),
  }));

export const collectReorders = (
  current: TreeNode[],
  original: TreeNode[],
): PageReorder[] => {
  const originalMap = new Map<
    number,
    { parentId: number | null; orderIndex: number }
  >();

  const buildMap = (nodes: TreeNode[], pid: number | null) => {
    nodes.forEach((node, idx) => {
      originalMap.set(node.id, { parentId: pid, orderIndex: idx });
      buildMap(node.children, node.id);
    });
  };
  buildMap(original, null);

  const reorders: PageReorder[] = [];
  const traverse = (nodes: TreeNode[], pid: number | null) => {
    nodes.forEach((node, index) => {
      const orig = originalMap.get(node.id);
      if (orig && (orig.parentId !== pid || orig.orderIndex !== index)) {
        reorders.push({ pageId: node.id, parentId: pid, orderIndex: index });
      }
      traverse(node.children, node.id);
    });
  };
  traverse(current, null);

  return reorders;
};

export const filterNode = (nodes: TreeNode[], id: number): TreeNode[] =>
  nodes
    .filter((n) => n.id !== id)
    .map((n) => ({ ...n, children: filterNode(n.children, id) }));
