import * as React from "react";
import { useState } from "react";
import {
  useInput,
  ReferenceInput,
  AutocompleteInput,
  useGetList,
} from "react-admin";
import { useWatch, useFormContext } from "react-hook-form";
import { Box, Typography, Chip, Paper, Collapse, alpha } from "@mui/material";
import RadioButtonUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import RadioButtonCheckedIcon from "@mui/icons-material/RadioButtonChecked";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import GroupIcon from "@mui/icons-material/Group";
import ShieldIcon from "@mui/icons-material/Shield";
import PersonIcon from "@mui/icons-material/Person";
import { LOOKUP_QUERY } from "../../../config";

// ── Types & helpers ───────────────────────────────────────────────────────────

type AccessType = "none" | "role" | "permission" | "owner";

interface Item {
  id: number;
  name: string;
}

const formatName = (s: string) =>
  s
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());

// ── Chip picker (single-select, no "None" chip — row header handles clearing) ─

const ChipPickerInput = ({
  source,
  items,
  color,
}: {
  source: string;
  items: Item[];
  color: string;
}) => {
  const { field } = useInput({ source });
  const current: number | null = field.value ?? null;

  return (
    <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.75 }}>
      {items.map((item) => {
        const active = current === item.id;
        return (
          <Chip
            key={item.id}
            label={formatName(item.name)}
            size="small"
            variant={active ? "filled" : "outlined"}
            onClick={() => field.onChange(active ? null : item.id)}
            sx={{
              cursor: "pointer",
              fontWeight: active ? 600 : 400,
              bgcolor: active ? color : undefined,
              borderColor: active ? color : undefined,
              color: active ? "#fff" : undefined,
              transition: "background 0.15s, border-color 0.15s",
              "&:hover": { opacity: 0.85 },
            }}
          />
        );
      })}
    </Box>
  );
};

// ── Option row ────────────────────────────────────────────────────────────────

const OPTION_META: Record<
  AccessType,
  { icon: React.ReactNode; label: string; subtitle: string; color: string }
> = {
  none: {
    icon: <LockOpenIcon fontSize="small" />,
    label: "Public",
    subtitle: "Open to all visitors — no restriction",
    color: "#616161",
  },
  role: {
    icon: <GroupIcon fontSize="small" />,
    label: "Role",
    subtitle: "All users assigned this role gain access",
    color: "#1976d2",
  },
  permission: {
    icon: <ShieldIcon fontSize="small" />,
    label: "Permission",
    subtitle: "Users who hold this specific permission",
    color: "#ed6c02",
  },
  owner: {
    icon: <PersonIcon fontSize="small" />,
    label: "Owner",
    subtitle: "Restrict to a single user",
    color: "#7b1fa2",
  },
};

const OptionRow = ({
  type,
  selected,
  onSelect,
  children,
}: {
  type: AccessType;
  selected: AccessType;
  onSelect: () => void;
  children?: React.ReactNode;
}) => {
  const active = selected === type;
  const { icon, label, subtitle, color } = OPTION_META[type];

  return (
    <Box
      sx={{
        borderBottom: "1px solid",
        borderColor: "divider",
        "&:last-child": { borderBottom: 0 },
      }}
    >
      {/* Clickable header */}
      <Box
        onClick={onSelect}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 2,
          py: 1.5,
          cursor: "pointer",
          bgcolor: active ? alpha(color, 0.05) : "transparent",
          transition: "background 0.15s",
          "&:hover": { bgcolor: alpha(color, active ? 0.07 : 0.04) },
          userSelect: "none",
        }}
      >
        <Box
          sx={{
            color: active ? color : "text.disabled",
            display: "flex",
            flexShrink: 0,
            transition: "color 0.15s",
          }}
        >
          {active ? (
            <RadioButtonCheckedIcon fontSize="small" />
          ) : (
            <RadioButtonUncheckedIcon fontSize="small" />
          )}
        </Box>

        <Box
          sx={{
            color: active ? color : "text.secondary",
            display: "flex",
            flexShrink: 0,
            transition: "color 0.15s",
          }}
        >
          {icon}
        </Box>

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            variant="body2"
            fontWeight={active ? 600 : 400}
            sx={{
              color: active ? color : "text.primary",
              transition: "color 0.15s",
            }}
          >
            {label}
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            {subtitle}
          </Typography>
        </Box>
      </Box>

      {/* Expandable picker */}
      {children && (
        <Collapse in={active} unmountOnExit>
          <Box
            sx={{
              px: 2,
              pt: 0.5,
              pb: 2,
              bgcolor: alpha(color, 0.03),
              borderTop: "1px dashed",
              borderColor: alpha(color, 0.2),
            }}
          >
            {children}
          </Box>
        </Collapse>
      )}
    </Box>
  );
};

// ── Main exported component ───────────────────────────────────────────────────

export const AccessControlPanel = () => {
  const { setValue } = useFormContext();
  const roleId = useWatch({ name: "accessControl.roleId" });
  const permissionId = useWatch({ name: "accessControl.permissionId" });
  const userId = useWatch({ name: "accessControl.userId" });

  const { data: roles = [] } = useGetList<Item>("roles", LOOKUP_QUERY);
  const { data: permissions = [] } = useGetList<Item>(
    "permissions",
    LOOKUP_QUERY,
  );

  // Derive initial type from form values (useWatch is synchronous on first render)
  const derivedType: AccessType =
    roleId != null
      ? "role"
      : permissionId != null
        ? "permission"
        : userId != null
          ? "owner"
          : "none";

  const [selected, setSelected] = useState<AccessType>(derivedType);

  const select = (type: AccessType) => {
    setSelected(type);
    if (type !== "role") setValue("accessControl.roleId", null);
    if (type !== "permission") setValue("accessControl.permissionId", null);
    if (type !== "owner") setValue("accessControl.userId", null);
  };

  return (
    <Paper variant="outlined" sx={{ overflow: "hidden" }}>
      <OptionRow
        type="none"
        selected={selected}
        onSelect={() => select("none")}
      />

      <OptionRow
        type="role"
        selected={selected}
        onSelect={() => select("role")}
      >
        <ChipPickerInput source="accessControl.roleId" items={roles} color="#1976d2" />
      </OptionRow>

      <OptionRow
        type="permission"
        selected={selected}
        onSelect={() => select("permission")}
      >
        <ChipPickerInput
          source="accessControl.permissionId"
          items={permissions}
          color="#ed6c02"
        />
      </OptionRow>

      <OptionRow
        type="owner"
        selected={selected}
        onSelect={() => select("owner")}
      >
        <ReferenceInput source="accessControl.userId" reference="users">
          <AutocompleteInput
            optionText="username"
            label="Search user…"
            fullWidth
            helperText={false}
          />
        </ReferenceInput>
      </OptionRow>
    </Paper>
  );
};
