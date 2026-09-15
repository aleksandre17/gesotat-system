import { useCallback } from "react";
import { List, ListProps } from "react-admin";
import SortableTree from "@nosferatu500/react-sortable-tree";
import "@nosferatu500/react-sortable-tree/style.css";
import {
  Box,
  Button,
  CircularProgress,
  Typography,
  Stack,
} from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import CancelIcon from "@mui/icons-material/Cancel";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import FolderIcon from "@mui/icons-material/Folder";
import InsertDriveFileIcon from "@mui/icons-material/InsertDriveFile";

import { usePageTree } from "./hooks/usePageTree";
import type { TreeNode } from "./types";
import { useHasPermission, PERMISSIONS } from "../../auth";
import { PAGES_QUERY } from "../../config";
import { AccessBadge } from "./components/AccessBadge";

const canDropAlways = () => true;

const NodeTitle = ({ node }: { node: TreeNode }) => (
  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
    {node.nodeType === "DIRECTORY" ? (
      <FolderIcon
        fontSize="small"
        sx={{ color: "warning.main", flexShrink: 0 }}
      />
    ) : (
      <InsertDriveFileIcon
        fontSize="small"
        sx={{ color: "info.main", flexShrink: 0 }}
      />
    )}
    <Typography variant="body2" sx={{ color: "rgba(0,0,0,0.87)" }}>
      {node.name}
    </Typography>
    <AccessBadge node={node} />
  </Box>
);

const PageTreeContent = () => {
  const canWrite = useHasPermission(PERMISSIONS.resource.write);
  const canDelete = useHasPermission(PERMISSIONS.resource.delete);

  const {
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
  } = usePageTree();

  const canDrag = useCallback(() => canWrite, [canWrite]);

  const generateNodeProps = useCallback(
    ({ node }: { node: TreeNode }) => ({
      title: <NodeTitle node={node} />,
      buttons: [
        canWrite && (
          <Button
            key="edit"
            size="small"
            startIcon={<EditIcon fontSize="small" />}
            onClick={() => handleEdit(node.id)}
          >
            Edit
          </Button>
        ),
        canDelete && (
          <Button
            key="delete"
            size="small"
            color="error"
            startIcon={<DeleteIcon fontSize="small" />}
            onClick={() => handleDelete(node.id, node.name)}
          >
            Delete
          </Button>
        ),
      ].filter(Boolean),
    }),
    [canWrite, canDelete, handleEdit, handleDelete],
  );

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 2, borderRadius: 2, boxShadow: 1 }}>
      <Typography variant="h6" gutterBottom>
        Page Hierarchy
      </Typography>

      <Box sx={{ height: 600 }}>
        <SortableTree
          treeData={treeData}
          onChange={handleChange}
          onVisibilityToggle={handleVisibilityToggle}
          canDrag={canDrag}
          canDrop={canDropAlways}
          generateNodeProps={generateNodeProps}
        />
      </Box>

      {canWrite && (
        <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
          <Button
            variant="contained"
            startIcon={isSaving ? <CircularProgress size={20} /> : <SaveIcon />}
            onClick={handleSave}
            disabled={!isDirty || isSaving}
          >
            Save
          </Button>
          <Button
            variant="outlined"
            color="secondary"
            startIcon={<CancelIcon />}
            onClick={handleCancel}
            disabled={!isDirty || isSaving}
          >
            Cancel
          </Button>
        </Stack>
      )}
    </Box>
  );
};

const PageList = (props: ListProps) => (
  <List
    {...props}
    title="Pages"
    perPage={PAGES_QUERY.pagination.perPage}
    sort={PAGES_QUERY.sort}
    filter={PAGES_QUERY.filter}
    pagination={false}
  >
    <PageTreeContent />
  </List>
);

export default PageList;
