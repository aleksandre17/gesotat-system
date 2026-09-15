import React, { useState } from "react";
import { NotificationType } from "react-admin";
import {
  Box,
  Typography,
  FormControlLabel,
  Switch,
  Button,
  CircularProgress,
  Divider,
  List,
  ListItem,
  ListItemText,
} from "@mui/material";
import AccessIcon from "@mui/icons-material/Storage";
import Download from "@mui/icons-material/Download";
import Info from "@mui/icons-material/Info";
import { httpClient } from "../../../api/httpClient";
import { ENV } from "../../../config/env";

interface MetaDatabaseItem {
  metaDatabaseType: string;
  metaDatabaseName: string;
  metaDatabaseUrl: string;
  metaDatabaseUser: string;
  metaDatabasePassword: string;
}

type NotifyFn = (
  message: string | React.ReactNode,
  options?: { type?: NotificationType },
) => void;

interface UploadAsideProps {
  item: MetaDatabaseItem;
  emptyOrData: boolean;
  onEmptyOrDataChange: (value: boolean) => void;
  notify: NotifyFn;
}

const handleDownloadTemplate = async (
  item: MetaDatabaseItem,
  notify: NotifyFn,
  empty: boolean,
): Promise<void> => {
  const params = new URLSearchParams({
    metaDatabaseType: item.metaDatabaseType,
    fileName: item.metaDatabaseName,
    empty: String(empty),
    metaDatabaseUrl: item.metaDatabaseUrl,
    metaDatabaseUser: item.metaDatabaseUser,
    metaDatabasePassword: item.metaDatabasePassword,
  });

  try {
    const response = await httpClient<Blob>(
      `${ENV.API_URL}/v1/import/mssql-to-access?${params}`,
      { responseType: "blob" },
    );

    const blob = response.body;
    const disposition = response.headers["content-disposition"] ?? null;

    let filename = item.metaDatabaseName.endsWith(".accdb")
      ? item.metaDatabaseName
      : `${item.metaDatabaseName}.accdb`;
    if (disposition) {
      const match = disposition.match(/filename="?([^"]+)"?/);
      if (match) filename = match[1];
    }

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  } catch (error) {
    console.error("Download failed:", error);
    notify("Download failed.", { type: "error" });
  }
};

const UploadAsideInner: React.FC<UploadAsideProps> = ({
  item,
  emptyOrData,
  onEmptyOrDataChange,
  notify,
}) => {
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      await handleDownloadTemplate(item, notify, emptyOrData);
    } finally {
      setDownloading(false);
    }
  };

  return (
    <Box
      sx={{
        width: 300,
        minWidth: 300,
        maxWidth: 300,
        flexShrink: 0,
        height: "100%",
        padding: 2,
        bgcolor: "background.paper",
        borderLeft: 1,
        borderColor: "divider",
        overflowY: "auto",
        display: "flex",
        flexDirection: "column",
        gap: 2,
      }}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 1,
        }}
      >
        <AccessIcon sx={{ fontSize: 120, color: "primary.main" }} />
        <Typography variant="h6">Access</Typography>
      </Box>

      <Box sx={{ mt: 2 }}>
        <FormControlLabel
          control={
            <Switch
              checked={emptyOrData}
              onChange={(e) => onEmptyOrDataChange(e.target.checked)}
              color="primary"
            />
          }
          label="მხოლოდ ცარიელი"
        />
      </Box>

      <Box sx={{ mt: 2 }}>
        <Typography variant="subtitle1" gutterBottom>
          შაბლონ ფაილი
        </Typography>
        <Button
          variant="outlined"
          size="small"
          onClick={handleDownload}
          disabled={downloading}
          startIcon={
            downloading ? (
              <CircularProgress size={16} color="inherit" />
            ) : (
              <Download />
            )
          }
          sx={{ width: "100%", textTransform: "none" }}
        >
          {downloading ? "იტვირთება..." : "შაბლონის ჩამოტვირთვა"}
        </Button>
      </Box>

      <Divider sx={{ my: 2 }} />

      <List
        subheader={
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
            <Info fontSize="small" color="primary" />
            <Typography variant="subtitle2">ინსტრუქცია</Typography>
          </Box>
        }
        dense
      >
        <ListItem>
          <ListItemText
            primary="ჩამოტვირთეთ შაბლონის ფაილი"
            secondary="გამოიყენეთ იგი საწყისად"
          />
        </ListItem>
        <ListItem>
          <ListItemText
            primary="შეავსეთ თქვენი მონაცემები"
            secondary="დაიცავით შაბლონის სტრუქტურა"
          />
        </ListItem>
        <ListItem>
          <ListItemText
            primary="ატვირთეთ შევსებული ფაილი"
            secondary="სისტემა ავტომატურად დაამუშავებს თქვენს მონაცემებს"
          />
        </ListItem>
      </List>
    </Box>
  );
};

export const UploadAside = React.memo(UploadAsideInner);
UploadAside.displayName = "UploadAside";
