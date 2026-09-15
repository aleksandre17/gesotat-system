import { useState } from "react";
import { useGetIdentity, useNotify, Title } from "react-admin";
import {
  Box,
  Avatar,
  Typography,
  Chip,
  Divider,
  Paper,
  Stack,
  Grid,
  CircularProgress,
  TextField,
  Button,
  IconButton,
  InputAdornment,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import SaveIcon from "@mui/icons-material/Save";
import CancelIcon from "@mui/icons-material/Cancel";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";
import ShieldIcon from "@mui/icons-material/Shield";
import KeyIcon from "@mui/icons-material/Key";
import PersonIcon from "@mui/icons-material/Person";
import LockIcon from "@mui/icons-material/Lock";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import { useUserContext, profileApi } from "../../auth";

// ── Helpers ───────────────────────────────────────────────────────────────────

const initials = (name: string): string =>
  name
    .split(" ")
    .map((w) => w[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

// ── Left identity panel ───────────────────────────────────────────────────────

const IdentityPanel = ({
  displayName,
  avatarSrc,
  username,
  onEditClick,
}: {
  displayName: string;
  avatarSrc?: string;
  username: string;
  onEditClick: () => void;
}) => {
  const user = useUserContext();

  return (
    <Paper
      elevation={1}
      sx={{ borderRadius: 3, overflow: "hidden", height: "100%" }}
    >
      {/* Gradient strip */}
      <Box
        sx={{
          height: 80,
          background: (t) =>
            `linear-gradient(135deg, ${t.palette.primary.dark}, ${t.palette.secondary.main})`,
        }}
      />

      <Box sx={{ px: 3, pb: 3 }}>
        {/* Avatar */}
        <Box sx={{ display: "flex", justifyContent: "center", mt: -5, mb: 2 }}>
          <Box sx={{ position: "relative" }}>
            <Avatar
              src={avatarSrc}
              sx={{
                width: 88,
                height: 88,
                fontSize: 28,
                bgcolor: "primary.dark",
                border: "4px solid",
                borderColor: "background.paper",
                boxShadow: 3,
              }}
            >
              {initials(displayName)}
            </Avatar>
            <IconButton
              size="small"
              onClick={onEditClick}
              sx={{
                position: "absolute",
                bottom: 2,
                right: 2,
                bgcolor: "background.paper",
                border: "1px solid",
                borderColor: "divider",
                p: 0.4,
                "&:hover": { bgcolor: "action.hover" },
              }}
            >
              <EditIcon sx={{ fontSize: 13 }} />
            </IconButton>
          </Box>
        </Box>

        {/* Name + username */}
        <Box sx={{ textAlign: "center", mb: 2.5 }}>
          <Typography variant="h6" fontWeight={700} lineHeight={1.3}>
            {displayName}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            @{username}
          </Typography>
        </Box>

        <Divider sx={{ mb: 2.5 }} />

        {/* Roles */}
        <Box sx={{ mb: 2.5 }}>
          <Box
            sx={{ display: "flex", alignItems: "center", gap: 0.75, mb: 1.5 }}
          >
            <ShieldIcon sx={{ fontSize: 15, color: "text.secondary" }} />
            <Typography
              variant="overline"
              color="text.secondary"
              lineHeight={1}
            >
              Roles
            </Typography>
          </Box>
          {user.roles.length === 0 ? (
            <Typography variant="body2" color="text.disabled" sx={{ pl: 0.5 }}>
              None assigned
            </Typography>
          ) : (
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.75 }}>
              {user.roles.map((r) => (
                <Chip
                  key={r}
                  label={r}
                  size="small"
                  color="primary"
                  variant="outlined"
                />
              ))}
            </Box>
          )}
        </Box>

        {/* Permissions */}
        {user.permissions.length > 0 && (
          <Box>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 0.75,
                mb: 1.5,
              }}
            >
              <KeyIcon sx={{ fontSize: 15, color: "text.secondary" }} />
              <Typography
                variant="overline"
                color="text.secondary"
                lineHeight={1}
              >
                Permissions
              </Typography>
            </Box>
            <Stack spacing={0.5}>
              {user.permissions.map((p) => (
                <Box
                  key={p}
                  sx={{ display: "flex", alignItems: "center", gap: 0.75 }}
                >
                  <CheckCircleIcon
                    sx={{ fontSize: 14, color: "success.main" }}
                  />
                  <Typography variant="caption" color="text.secondary">
                    {p}
                  </Typography>
                </Box>
              ))}
            </Stack>
          </Box>
        )}
      </Box>
    </Paper>
  );
};

// ── Personal form ─────────────────────────────────────────────────────────────

const PersonalForm = ({
  initialName,
  initialAvatar,
  onSaved,
  onCancel,
}: {
  initialName: string;
  initialAvatar: string;
  onSaved: () => void;
  onCancel: () => void;
}) => {
  const notify = useNotify();
  const [name, setName] = useState(initialName);
  const [avatar, setAvatar] = useState(initialAvatar);
  const [saving, setSaving] = useState(false);
  const dirty = name !== initialName || avatar !== initialAvatar;

  const handleSave = async () => {
    setSaving(true);
    try {
      await profileApi.update({ displayName: name, avatar });
      notify("Profile updated", { type: "success" });
      onSaved();
    } catch {
      notify("Failed to update profile", { type: "error" });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Stack spacing={3}>
      <Grid container spacing={3} alignItems="center">
        <Grid size={{ xs: 12, sm: "auto" }}>
          <Avatar
            src={avatar || undefined}
            sx={{
              width: 64,
              height: 64,
              fontSize: 20,
              bgcolor: "primary.dark",
              mx: "auto",
            }}
          >
            {initials(name || "?")}
          </Avatar>
        </Grid>
        <Grid size={{ xs: 12, sm: "grow" }}>
          <TextField
            label="Avatar URL"
            value={avatar}
            onChange={(e) => setAvatar(e.target.value)}
            fullWidth
            size="small"
            placeholder="https://example.com/photo.png"
            helperText="Paste a direct link to your photo"
          />
        </Grid>
      </Grid>

      <TextField
        label="Display name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        fullWidth
      />

      <Box sx={{ display: "flex", gap: 1.5 }}>
        <Button
          variant="contained"
          startIcon={saving ? <CircularProgress size={16} /> : <SaveIcon />}
          onClick={handleSave}
          disabled={!dirty || saving}
        >
          Save changes
        </Button>
        <Button
          variant="outlined"
          startIcon={<CancelIcon />}
          onClick={onCancel}
          disabled={saving}
        >
          Cancel
        </Button>
      </Box>
    </Stack>
  );
};

// ── Field row (view mode) ─────────────────────────────────────────────────────

const FieldRow = ({ label, value }: { label: string; value?: string }) => (
  <Box>
    <Typography variant="caption" color="text.secondary" fontWeight={500}>
      {label}
    </Typography>
    <Typography
      variant="body1"
      color={value ? "text.primary" : "text.disabled"}
      sx={{ mt: 0.25, wordBreak: "break-all" }}
    >
      {value || "Not set"}
    </Typography>
  </Box>
);

// ── Password form ─────────────────────────────────────────────────────────────

const PasswordForm = () => {
  const notify = useNotify();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNext, setShowNext] = useState(false);
  const [saving, setSaving] = useState(false);

  const mismatch = next.length > 0 && confirm.length > 0 && next !== confirm;
  const canSave = current.length > 0 && next.length >= 6 && next === confirm;

  const handleSave = async () => {
    setSaving(true);
    try {
      await profileApi.changePassword({
        currentPassword: current,
        newPassword: next,
      });
      notify("Password changed", { type: "success" });
      setCurrent("");
      setNext("");
      setConfirm("");
    } catch {
      notify("Failed to change password", { type: "error" });
    } finally {
      setSaving(false);
    }
  };

  const eye = (show: boolean, toggle: () => void) => (
    <InputAdornment position="end">
      <IconButton size="small" onClick={toggle} edge="end">
        {show ? (
          <VisibilityOff fontSize="small" />
        ) : (
          <Visibility fontSize="small" />
        )}
      </IconButton>
    </InputAdornment>
  );

  return (
    <Stack spacing={3}>
      <TextField
        label="Current password"
        type={showCurrent ? "text" : "password"}
        value={current}
        onChange={(e) => setCurrent(e.target.value)}
        fullWidth
        slotProps={{
          input: {
            endAdornment: eye(showCurrent, () => setShowCurrent((v) => !v)),
          },
        }}
      />
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label="New password"
            type={showNext ? "text" : "password"}
            value={next}
            onChange={(e) => setNext(e.target.value)}
            fullWidth
            helperText="Minimum 6 characters"
            slotProps={{
              input: {
                endAdornment: eye(showNext, () => setShowNext((v) => !v)),
              },
            }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label="Confirm new password"
            type={showNext ? "text" : "password"}
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            fullWidth
            error={mismatch}
            helperText={mismatch ? "Passwords do not match" : " "}
          />
        </Grid>
      </Grid>
      <Box>
        <Button
          variant="contained"
          color="warning"
          startIcon={saving ? <CircularProgress size={16} /> : <LockIcon />}
          onClick={handleSave}
          disabled={!canSave || saving}
        >
          Change password
        </Button>
      </Box>
    </Stack>
  );
};

// ── Section heading ───────────────────────────────────────────────────────────

const SectionHeading = ({
  icon,
  title,
  action,
}: {
  icon: React.ReactNode;
  title: string;
  action?: React.ReactNode;
}) => (
  <Box
    sx={{
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      mb: 3,
    }}
  >
    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
      <Box sx={{ color: "primary.main", display: "flex" }}>{icon}</Box>
      <Typography variant="subtitle1" fontWeight={700}>
        {title}
      </Typography>
    </Box>
    {action}
  </Box>
);

// ── Main component ────────────────────────────────────────────────────────────

export const ProfilePage = () => {
  const { data: identity, isPending, refetch } = useGetIdentity();
  const [editing, setEditing] = useState(false);

  if (isPending) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", pt: 10 }}>
        <CircularProgress />
      </Box>
    );
  }

  const username = String(identity?.id ?? "");
  const displayName = (identity?.displayName as string | null) ?? null;
  const avatarSrc = identity?.avatar as string | undefined;

  return (
    <Box sx={{ p: 3 }}>
      <Title title="Profile" />

      <Box
        sx={{
          display: "flex",
          gap: 3,
          alignItems: "stretch",
          flexDirection: { xs: "column", md: "row" },
        }}
      >
        {/* ── Left: identity panel ───────────────── */}
        <Box sx={{ width: { xs: "100%", md: "25%" }, flexShrink: 0 }}>
          <IdentityPanel
            displayName={displayName ?? username}
            avatarSrc={avatarSrc}
            username={username}
            onEditClick={() => setEditing(true)}
          />
        </Box>

        {/* ── Right: edit forms ──────────────────── */}
        <Box sx={{ flex: 1 }}>
          <Stack spacing={3}>
            {/* Personal info */}
            <Paper elevation={1} sx={{ borderRadius: 3, p: 4 }}>
              <SectionHeading
                icon={<PersonIcon />}
                title="Personal information"
                action={
                  !editing && (
                    <Button
                      size="small"
                      startIcon={<EditIcon />}
                      onClick={() => setEditing(true)}
                    >
                      Edit
                    </Button>
                  )
                }
              />

              {editing ? (
                <PersonalForm
                  initialName={displayName ?? ""}
                  initialAvatar={avatarSrc ?? ""}
                  onSaved={async () => {
                    await refetch();
                    setEditing(false);
                  }}
                  onCancel={() => setEditing(false)}
                />
              ) : (
                <Grid container spacing={3}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <FieldRow label="Username" value={username} />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <FieldRow
                      label="Display name"
                      value={displayName ?? undefined}
                    />
                  </Grid>
                  <Grid size={{ xs: 12 }}>
                    <FieldRow label="Avatar URL" value={avatarSrc} />
                  </Grid>
                </Grid>
              )}
            </Paper>

            {/* Security */}
            <Paper elevation={1} sx={{ borderRadius: 3, p: 4 }}>
              <SectionHeading icon={<LockIcon />} title="Change password" />
              <PasswordForm />
            </Paper>
          </Stack>
        </Box>
      </Box>
    </Box>
  );
};
