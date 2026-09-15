import { useState, useCallback, useRef, useEffect } from "react";
import {
  useDataProvider,
  useNotify,
  useRedirect,
  useDelete,
  useListContext,
} from "react-admin";
import type { TreeNode, PageRecord } from "../types";
import {
  buildTree,
  updatePositions,
  collectReorders,
  filterNode,
} from "../utils/treeUtils";

/**
 * Module-level: survives component unmount/remount (navigation),
 * cleared on browser refresh. No localStorage needed.
 */
const expandedIds = new Set<number>();

interface UsePageTreeReturn {
  treeData: TreeNode[];
  isDirty: boolean;
  isSaving: boolean;
  isLoading: boolean;
  handleChange: (newTreeData: TreeNode[]) => void;
  handleVisibilityToggle: (params: {
    node: TreeNode;
    expanded: boolean;
  }) => void;
  handleSave: () => Promise<void>;
  handleCancel: () => void;
  handleEdit: (id: number) => void;
  handleDelete: (id: number, name: string) => void;
}

export const usePageTree = (): UsePageTreeReturn => {
  const { data: listData, isPending } = useListContext<PageRecord>();
  const dataProvider = useDataProvider();
  const notify = useNotify();
  const redirect = useRedirect();
  const [deleteOne] = useDelete();

  const [treeData, setTreeData] = useState<TreeNode[]>([]);
  const [isDirty, setIsDirty] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const isLoading = isPending;

  // Snapshot of the last saved state — no re-render needed when it changes
  const originalRef = useRef<TreeNode[]>([]);

  useEffect(() => {
    if (!listData) return;
    const tree = buildTree(listData, expandedIds);
    originalRef.current = tree;
    setTreeData(tree);
  }, [listData]);

  /**
   * Fires on drag-and-drop only (structural changes).
   * isDirty is derived from actual reorders, not from expand/collapse.
   */
  const handleChange = useCallback((newTreeData: TreeNode[]) => {
    const updated = updatePositions(newTreeData);
    setTreeData(updated);
    setIsDirty(collectReorders(updated, originalRef.current).length > 0);
  }, []);

  /**
   * Fires on expand/collapse only.
   * Keeps module-level expandedIds in sync — no re-render, no localStorage.
   */
  const handleVisibilityToggle = useCallback(
    ({ node, expanded }: { node: TreeNode; expanded: boolean }) => {
      if (expanded) {
        expandedIds.add(node.id);
      } else {
        expandedIds.delete(node.id);
      }
    },
    [],
  );

  const handleSave = useCallback(async () => {
    setIsSaving(true);
    try {
      const reorders = collectReorders(treeData, originalRef.current);
      if (reorders.length === 0) {
        notify("No changes to save", { type: "info" });
        return;
      }
      await dataProvider.reorderPages("pages", { reorders });
      originalRef.current = treeData;
      setIsDirty(false);
      notify("Page order saved", { type: "success" });
    } catch {
      notify("Error saving page order", { type: "error" });
    } finally {
      setIsSaving(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [treeData, notify]);

  const handleCancel = useCallback(() => {
    setTreeData(originalRef.current);
    setIsDirty(false);
    notify("Changes discarded", { type: "info" });
  }, [notify]);

  const handleEdit = useCallback(
    (id: number) => {
      redirect("edit", "pages", id);
    },
    [redirect],
  );

  const handleDelete = useCallback(
    (id: number, name: string) => {
      void deleteOne(
        "pages",
        { id, meta: { name } },
        {
          mutationMode: "undoable",
          onSuccess: () => {
            notify(`Page "${name}" deleted`, { type: "info", undoable: true });
            expandedIds.delete(id);
            setTreeData((prev) => filterNode(prev, id));
            originalRef.current = filterNode(originalRef.current, id);
          },
          onError: () => notify("Error deleting page", { type: "error" }),
        },
      );
    },
    [deleteOne, notify],
  );

  return {
    treeData,
    isDirty,
    isSaving,
    isLoading,
    handleChange,
    handleVisibilityToggle,
    handleSave,
    handleCancel,
    handleEdit,
    handleDelete,
  };
};