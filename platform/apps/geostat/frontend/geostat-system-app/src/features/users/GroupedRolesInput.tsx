import { useInput, useGetList } from "react-admin";
import { LOOKUP_QUERY } from "../../config";
import {
  Box,
  Typography,
  Checkbox,
  FormControlLabel,
  Paper,
  Chip,
  alpha,
  Divider,
} from "@mui/material";
import ManageAccountsIcon from "@mui/icons-material/ManageAccounts";
import BusinessIcon from "@mui/icons-material/Business";

// ── Label maps ─────────────────────────────────────────────────────────────

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "Administrator",
  MANAGER: "Manager",
  VIEWER: "Viewer",
  DEPT_PRICES: "ფასები",
  DEPT_FOREIGN_TRADE: "საგარეო ვაჭრობა",
  DEPT_AGRICULTURE: "სოფლის მეურნეობა",
  DEPT_NATIONAL_ACCOUNTS: "ეროვნული ანგარიშები",
  DEPT_PUBLIC_RELATIONS: "საზოგადოებასთან ურთიერთობა",
  DEPT_BUSINESS_STATISTICS: "ბიზნეს სტატისტიკა",
  DEPT_DEMOGRAPHICS: "დემოგრაფია",
  DEPT_SOCIAL_STATISTICS: "სოციალური სტატისტიკა",
  DEPT_IT: "IT",
};

const SYSTEM_DESCRIPTIONS: Record<string, string> = {
  ADMIN: "Full system access, bypasses all department restrictions",
  MANAGER: "Cross-department read and write access",
  VIEWER: "Read-only access across all resources",
};

// ── Types ──────────────────────────────────────────────────────────────────

interface RoleRecord {
  id: number;
  name: string;
}

type GroupKey = "system" | "department";

function classifyRole(name: string): GroupKey {
  return name.startsWith("DEPT_") ? "department" : "system";
}

// ── Role item ──────────────────────────────────────────────────────────────

const RoleItem = ({
  role,
  checked,
  onToggle,
}: {
  role: RoleRecord;
  checked: boolean;
  onToggle: () => void;
}) => {
  const label = ROLE_LABELS[role.name] ?? role.name;
  const description = SYSTEM_DESCRIPTIONS[role.name];
  const isSystem = classifyRole(role.name) === "system";

  return (
    <Box
      onClick={onToggle}
      sx={{
        display: "flex",
        alignItems: "flex-start",
        gap: 1,
        px: 2,
        py: 1.5,
        cursor: "pointer",
        borderRadius: 1,
        bgcolor: checked ? alpha("#1976d2", 0.05) : "transparent",
        border: "1px solid",
        borderColor: checked ? alpha("#1976d2", 0.3) : "divider",
        transition: "all 0.15s",
        "&:hover": {
          bgcolor: alpha("#1976d2", 0.06),
          borderColor: alpha("#1976d2", 0.4),
        },
      }}
    >
      <Checkbox
        size="small"
        checked={checked}
        onChange={onToggle}
        onClick={(e) => e.stopPropagation()}
        sx={{
          p: 0,
          mt: 0.25,
          color: checked ? "primary.main" : "action.active",
          "&.Mui-checked": { color: "primary.main" },
        }}
      />
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Typography variant="body2" fontWeight={checked ? 600 : 400}>
            {label}
          </Typography>
          {!isSystem && (
            <Typography
              variant="caption"
              color="text.disabled"
              sx={{ fontFamily: "monospace", fontSize: 10 }}
            >
              {role.name}
            </Typography>
          )}
        </Box>
        {description && (
          <Typography variant="caption" color="text.secondary">
            {description}
          </Typography>
        )}
      </Box>
    </Box>
  );
};

// ── Group section ──────────────────────────────────────────────────────────

const GroupSection = ({
  groupKey,
  roles,
  selected,
  onToggle,
  onBulkToggle,
}: {
  groupKey: GroupKey;
  roles: RoleRecord[];
  selected: number[];
  onToggle: (id: number) => void;
  onBulkToggle: (ids: number[], add: boolean) => void;
}) => {
  const isSystem = groupKey === "system";
  const ids = roles.map((r) => r.id);
  const checkedCount = ids.filter((id) => selected.includes(id)).length;
  const allChecked = checkedCount === ids.length && ids.length > 0;
  const someChecked = checkedCount > 0 && !allChecked;

  return (
    <Paper variant="outlined" sx={{ overflow: "hidden" }}>
      {/* Header */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          px: 2,
          py: 1.25,
          bgcolor: "action.hover",
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          {isSystem ? (
            <ManageAccountsIcon fontSize="small" color="action" />
          ) : (
            <BusinessIcon fontSize="small" color="action" />
          )}
          <Typography variant="subtitle2" fontWeight={600}>
            {isSystem ? "System Roles" : "Department Roles"}
          </Typography>
          <Chip
            label={`${checkedCount} / ${roles.length}`}
            size="small"
            variant={checkedCount > 0 ? "filled" : "outlined"}
            color={checkedCount > 0 ? "primary" : "default"}
            sx={{ height: 18, fontSize: 11 }}
          />
        </Box>

        <FormControlLabel
          labelPlacement="start"
          label={
            <Typography variant="caption" color="text.secondary">
              Select all
            </Typography>
          }
          control={
            <Checkbox
              size="small"
              checked={allChecked}
              indeterminate={someChecked}
              onChange={() => onBulkToggle(ids, !allChecked)}
            />
          }
          sx={{ mr: 0 }}
        />
      </Box>

      <Divider />

      {/* Role items */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: isSystem
            ? "1fr"
            : { xs: "1fr", sm: "1fr 1fr", md: "1fr 1fr 1fr" },
          gap: 1,
          p: 1.5,
        }}
      >
        {roles.map((role) => (
          <RoleItem
            key={role.id}
            role={role}
            checked={selected.includes(role.id)}
            onToggle={() => onToggle(role.id)}
          />
        ))}
      </Box>
    </Paper>
  );
};

// ── Main component ─────────────────────────────────────────────────────────

export const GroupedRolesInput = ({ source }: { source: string }) => {
  const { field } = useInput({ source });
  const { data: allRoles = [] } = useGetList<RoleRecord>("roles", LOOKUP_QUERY);

  const selected: number[] = Array.isArray(field.value)
    ? field.value.map((v: number | { id: number }) =>
        typeof v === "object" && v !== null ? v.id : v,
      )
    : [];

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

  const groups = allRoles.reduce<Record<GroupKey, RoleRecord[]>>(
    (acc, role) => {
      acc[classifyRole(role.name)].push(role);
      return acc;
    },
    { system: [], department: [] },
  );

  return (
    <Box
      sx={{ display: "flex", flexDirection: "column", gap: 2, width: "100%" }}
    >
      {groups.system.length > 0 && (
        <GroupSection
          groupKey="system"
          roles={groups.system}
          selected={selected}
          onToggle={onToggle}
          onBulkToggle={onBulkToggle}
        />
      )}
      {groups.department.length > 0 && (
        <GroupSection
          groupKey="department"
          roles={groups.department}
          selected={selected}
          onToggle={onToggle}
          onBulkToggle={onBulkToggle}
        />
      )}
    </Box>
  );
};
