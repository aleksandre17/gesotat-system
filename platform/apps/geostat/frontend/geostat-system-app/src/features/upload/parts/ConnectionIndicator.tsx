import React from "react";
import { Chip } from "@mui/material";
import CircleIcon from "@mui/icons-material/Circle";
import ReconnectIcon from "@mui/icons-material/Refresh";
import type { ConnectionState } from "../../../services/WebSocketService";

interface ConnectionIndicatorProps {
  state: ConnectionState;
  onReconnect?: () => void;
}

const STATE_CONFIG: Record<
  ConnectionState,
  { label: string; color: "success" | "warning" | "error" | "default" }
> = {
  connected: { label: "დაკავშირებული", color: "success" },
  connecting: { label: "კავშირდება...", color: "warning" },
  reconnecting: { label: "ხელახლა კავშირდება...", color: "warning" },
  disconnected: { label: "გათიშულია", color: "error" },
  failed: { label: "კავშირი ვერ მოხერხდა", color: "error" },
};

const ConnectionIndicatorInner: React.FC<ConnectionIndicatorProps> = ({
  state,
  onReconnect,
}) => {
  const config = STATE_CONFIG[state];

  return (
    <Chip
      icon={<CircleIcon sx={{ fontSize: 10 }} />}
      label={config.label}
      color={config.color}
      size="small"
      variant="outlined"
      deleteIcon={
        state === "failed" || state === "disconnected" ? (
          <ReconnectIcon fontSize="small" />
        ) : undefined
      }
      onDelete={
        (state === "failed" || state === "disconnected") && onReconnect
          ? onReconnect
          : undefined
      }
      sx={{ fontWeight: 500 }}
    />
  );
};

export const ConnectionIndicator = React.memo(ConnectionIndicatorInner);
ConnectionIndicator.displayName = "ConnectionIndicator";