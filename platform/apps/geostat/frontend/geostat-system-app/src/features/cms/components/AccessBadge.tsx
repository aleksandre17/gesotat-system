import { Chip, Tooltip } from "@mui/material";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import GroupIcon from "@mui/icons-material/Group";
import KeyIcon from "@mui/icons-material/Key";
import PersonIcon from "@mui/icons-material/Person";
import type { TreeNode } from "../types";

type AccessInfo =
  | { type: "public" }
  | { type: "role"; roleId: number }
  | { type: "permission"; permissionId: number }
  | { type: "owner"; userId: number };

const resolveAccess = (
  node: Pick<TreeNode, "accessControl" | "role">,
): AccessInfo => {
  const ac = node.accessControl;
  if (ac?.userId != null) return { type: "owner", userId: ac.userId };
  if (ac?.permissionId != null)
    return { type: "permission", permissionId: ac.permissionId };
  if (ac?.roleId != null) return { type: "role", roleId: ac.roleId };
  if (node.role) return { type: "role", roleId: -1 };
  return { type: "public" };
};

const CONFIG = {
  public: {
    icon: <LockOpenIcon sx={{ fontSize: 13 }} />,
    label: "Public",
    color: "default" as const,
    tooltip: "No restrictions — all authenticated users",
  },
  role: {
    icon: <GroupIcon sx={{ fontSize: 13 }} />,
    label: "Role",
    color: "primary" as const,
  },
  permission: {
    icon: <KeyIcon sx={{ fontSize: 13 }} />,
    label: "Permission",
    color: "warning" as const,
  },
  owner: {
    icon: <PersonIcon sx={{ fontSize: 13 }} />,
    label: "Owner",
    color: "secondary" as const,
    // tooltip: (id: number) => `Owner (User ID: ${id})`,
  },
};

interface AccessBadgeProps {
  node: Pick<TreeNode, "accessControl" | "role">;
}

const getTooltip = (access: AccessInfo): string => {
  switch (access.type) {
    case "public":
      return "No restrictions — all authenticated users";
    case "role":
      return access.roleId === -1
        ? "Role restricted"
        : `Role ID: ${access.roleId}`;
    case "permission":
      return `Permission ID: ${access.permissionId}`;
    case "owner":
      return `Owner (User ID: ${access.userId})`;
  }
};

export const AccessBadge = ({ node }: AccessBadgeProps) => {
  const access = resolveAccess(node);
  const cfg = CONFIG[access.type];
  const tooltipTitle = getTooltip(access);

  return (
    <Tooltip title={tooltipTitle} placement="top" arrow>
      <Chip
        size="small"
        icon={cfg.icon}
        label={cfg.label}
        color={cfg.color}
        variant={access.type === "public" ? "outlined" : "filled"}
        sx={{
          height: 20,
          fontSize: 11,
          cursor: "default",
          "& .MuiChip-icon": { ml: "4px" },
          "& .MuiChip-label": { px: "6px" },
        }}
      />
    </Tooltip>
  );
};
