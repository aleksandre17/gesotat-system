import { useInput, useGetList } from "react-admin";
import { LOOKUP_QUERY } from "../../config";
import {
  Box,
  Typography,
  Checkbox,
  FormControlLabel,
  Paper,
  alpha,
} from "@mui/material";
import StorageIcon from "@mui/icons-material/Storage";
import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import BusinessIcon from "@mui/icons-material/Business";
import type { SvgIconComponent } from "@mui/icons-material";

// ── Human-readable labels ──────────────────────────────────────────────────

const PERMISSION_LABELS: Record<string, string> = {
  READ_RESOURCE: "Read (all resources)",
  WRITE_RESOURCE: "Write (all resources)",
  DELETE_RESOURCE: "Delete (all resources)",
  MANAGE_USERS: "Manage Users",
  MANAGE_ROLES: "Manage Roles",
  MANAGE_PERMISSIONS: "Manage Permissions",
  MANAGE_DEPT_PRICES: "ფასები",
  MANAGE_DEPT_FOREIGN_TRADE: "საგარეო ვაჭრობა",
  MANAGE_DEPT_AGRICULTURE: "სოფლის მეურნეობა",
  MANAGE_DEPT_NATIONAL_ACCOUNTS: "ეროვნული ანგარიშები",
  MANAGE_DEPT_PUBLIC_RELATIONS: "საზოგადოებასთან ურთიერთობა",
  MANAGE_DEPT_BUSINESS_STATISTICS: "ბიზნეს სტატისტიკა",
  MANAGE_DEPT_DEMOGRAPHICS: "დემოგრაფია",
  MANAGE_DEPT_SOCIAL_STATISTICS: "სოციალური სტატისტიკა",
  MANAGE_DEPT_IT: "IT",
};

// ── Group config ───────────────────────────────────────────────────────────

type GroupKey = "resource" | "admin" | "department";

const GROUP_META: Record<
  GroupKey,
  { label: string; description: string; color: string; Icon: SvgIconComponent }
> = {
  resource: {
    label: "Resource Access",
    description: "Global data read / write / delete across all departments",
    color: "#1976d2",
    Icon: StorageIcon,
  },
  admin: {
    label: "Administration",
    description: "User, role and permission management",
    color: "#d32f2f",
    Icon: AdminPanelSettingsIcon,
  },
  department: {
    label: "Department Access",
    description: "Per-department data management",
    color: "#388e3c",
    Icon: BusinessIcon,
  },
};

const GROUP_ORDER: GroupKey[] = ["resource", "admin", "department"];

function classifyPermission(name: string): GroupKey {
  if (name.startsWith("MANAGE_DEPT_")) return "department";
  if (["READ_RESOURCE", "WRITE_RESOURCE", "DELETE_RESOURCE"].includes(name))
    return "resource";
  return "admin";
}

// ── Types ──────────────────────────────────────────────────────────────────

interface PermRecord {
  id: number;
  name: string;
}

interface GroupCardProps {
  groupKey: GroupKey;
  permissions: PermRecord[];
  selected: number[];
  onToggle: (id: number) => void;
  onBulkToggle: (ids: number[], add: boolean) => void;
}

// ── Group card ─────────────────────────────────────────────────────────────

const GroupCard = ({
  groupKey,
  permissions,
  selected,
  onToggle,
  onBulkToggle,
}: GroupCardProps) => {
  const { label, description, color, Icon } = GROUP_META[groupKey];
  const ids = permissions.map((p) => p.id);
  const checkedCount = ids.filter((id) => selected.includes(id)).length;
  const allChecked = checkedCount === ids.length && ids.length > 0;
  const someChecked = checkedCount > 0 && !allChecked;

  return (
    <Paper
      variant="outlined"
      sx={{
        overflow: "hidden",
        borderColor:
          allChecked || someChecked ? `${color}88` : "divider",
        borderWidth: allChecked || someChecked ? 2 : 1,
        transition: "border-color 0.2s",
      }}
    >
      {/* Header */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 2,
          py: 1.5,
          bgcolor: alpha(color, 0.06),
          borderBottom: "1px solid",
          borderColor: "divider",
        }}
      >
        <Icon sx={{ color, fontSize: 22 }} />
        <Box sx={{ flex: 1 }}>
          <Typography variant="subtitle2" fontWeight={700} sx={{ color }}>
            {label}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {description}
          </Typography>
        </Box>
        <FormControlLabel
          labelPlacement="start"
          label={
            <Typography variant="caption" color="text.secondary">
              ყველა
            </Typography>
          }
          control={
            <Checkbox
              size="small"
              checked={allChecked}
              indeterminate={someChecked}
              onChange={() => onBulkToggle(ids, !allChecked)}
              sx={{
                color,
                "&.Mui-checked": { color },
                "&.MuiCheckbox-indeterminate": { color },
              }}
            />
          }
          sx={{ mr: 0, ml: "auto" }}
        />
      </Box>

      {/* Permission list */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
        }}
      >
        {permissions.map((perm) => {
          const isChecked = selected.includes(perm.id);
          return (
            <Box
              key={perm.id}
              sx={{
                px: 2,
                py: 1.25,
                borderRight: "1px solid",
                borderBottom: "1px solid",
                borderColor: "divider",
                bgcolor: isChecked ? alpha(color, 0.05) : "transparent",
                transition: "background 0.15s",
                cursor: "pointer",
                "&:hover": { bgcolor: alpha(color, 0.08) },
              }}
              onClick={() => onToggle(perm.id)}
            >
              <FormControlLabel
                label={
                  <Box>
                    <Typography
                      variant="body2"
                      fontWeight={isChecked ? 600 : 400}
                      lineHeight={1.3}
                    >
                      {PERMISSION_LABELS[perm.name] ?? perm.name}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.disabled"
                      sx={{ fontFamily: "monospace", fontSize: 10 }}
                    >
                      {perm.name}
                    </Typography>
                  </Box>
                }
                control={
                  <Checkbox
                    size="small"
                    checked={isChecked}
                    onChange={() => onToggle(perm.id)}
                    onClick={(e) => e.stopPropagation()}
                    sx={{
                      alignSelf: "flex-start",
                      mt: 0.25,
                      color: isChecked ? color : undefined,
                      "&.Mui-checked": { color },
                    }}
                  />
                }
                sx={{ width: "100%", m: 0, alignItems: "flex-start" }}
              />
            </Box>
          );
        })}
      </Box>
    </Paper>
  );
};

// ── Main exported component ────────────────────────────────────────────────

export const GroupedPermissionsInput = ({ source }: { source: string }) => {
  const { field } = useInput({ source });
  const { data: allPermissions = [] } = useGetList<PermRecord>(
    "permissions",
    LOOKUP_QUERY,
  );

  // Backend returns [{id, name}, ...]; normalize to plain id[] for consistent comparison.
  const raw = Array.isArray(field.value) ? field.value : [];
  const selected: number[] = raw.map((v: unknown) =>
    typeof v === "number" ? v : (v as { id: number }).id,
  );

  const onToggle = (id: number) => {
    field.onChange(
      selected.includes(id)
        ? selected.filter((s) => s !== id)
        : [...selected, id],
    );
  };

  const onBulkToggle = (ids: number[], add: boolean) => {
    field.onChange(
      add
        ? [...new Set([...selected, ...ids])]
        : selected.filter((id) => !ids.includes(id)),
    );
  };

  const groups = allPermissions.reduce<Record<GroupKey, PermRecord[]>>(
    (acc, perm) => {
      acc[classifyPermission(perm.name)].push(perm);
      return acc;
    },
    { resource: [], admin: [], department: [] },
  );

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 2, width: "100%" }}>
      {GROUP_ORDER.map((key) =>
        groups[key].length > 0 ? (
          <GroupCard
            key={key}
            groupKey={key}
            permissions={groups[key]}
            selected={selected}
            onToggle={onToggle}
            onBulkToggle={onBulkToggle}
          />
        ) : null,
      )}
    </Box>
  );
};