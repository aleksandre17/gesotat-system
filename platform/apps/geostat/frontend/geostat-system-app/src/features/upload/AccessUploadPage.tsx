import React, { useState, useCallback } from "react";
import { Create, useNotify, SimpleForm } from "react-admin";
import { useParams, useNavigate } from "react-router-dom";
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  FormControl,
  FormControlLabel,
  Switch,
  LinearProgress,
  Button,
} from "@mui/material";
import ArrowBack from "@mui/icons-material/ArrowBack";
import Upload from "@mui/icons-material/Upload";
import Cancel from "@mui/icons-material/Cancel";
import { AccessUploadButton } from "./AccessUploadButton";
import { MultiSelectYears } from "./MultiSelectYears";
import { useUpload } from "./hooks/useUpload";
import { UploadAside } from "./parts/UploadAside";
import { ConnectionIndicator } from "./parts/ConnectionIndicator";
import { useWebSocket } from "./hooks/useWebSocket";
import type { PageItem } from "../cms/leafPaths";
import { ENV } from "../../config/env";
import { useCanAccessPage } from "../../auth";
import { AccessDenied } from "../../auth/guards";

interface AccessUploadPageProps {
  item: PageItem;
  path: string;
}

export const AccessUploadPage: React.FC<AccessUploadPageProps> = ({
  item,
  path,
}) => {
  // ── All hooks must be declared before any conditional return ──────────────
  const canAccess = useCanAccessPage(item);
  const { id } = useParams();
  const notify = useNotify();
  const navigate = useNavigate();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedYears, setSelectedYears] = useState<string[]>([]);
  const [emptyOrData, setEmptyOrData] = useState(false);
  const [clearServerData, setClearServerData] = useState(false);

  //#region Upload state machine
  const {
    uploadState,
    uploadProgress,
    uploadMessage,
    taskId,
    startUpload,
    cancelUpload,
    resetUpload,
    handleProgressUpdate,
  } = useUpload({
    apiBaseUrl: ENV.API_URL,
    path,
    onSuccess: () => notify("File uploaded successfully", { type: "success" }),
    onError: (msg) => notify(msg, { type: "error" }),
  });
  //#endregion

  //#region WebSocket
  const handleWebSocketError = useCallback(
    (error: string) => {
      notify(error, { type: "error" });
    },
    [notify],
  );

  const { isConnected, connectionState, reconnect } = useWebSocket({
    taskId,
    onProgress: handleProgressUpdate,
    onError: handleWebSocketError,
  });
  //#endregion

  //#region Handlers
  const handleSave = useCallback(async () => {
    if (!selectedFile) {
      notify("Please select a file to upload.", { type: "warning" });
      return;
    }

    if (!isConnected()) {
      notify(
        "WebSocket not connected. Please wait for reconnection or click reconnect.",
        {
          type: "error",
        },
      );
      return;
    }

    await startUpload(selectedFile, {
      metaDatabaseType: item.metaDatabaseType,
      metaDatabaseName: item.metaDatabaseName,
      years: selectedYears,
      clearServerData: clearServerData.toString(),
      metaDatabaseUrl: item.metaDatabaseUrl,
      metaDatabaseUser: item.metaDatabaseUser,
      metaDatabasePassword: item.metaDatabasePassword,
    });
  }, [
    selectedFile,
    isConnected,
    notify,
    startUpload,
    item,
    selectedYears,
    clearServerData,
  ]);

  const handleFileSelected = useCallback(
    (file: File | null) => {
      setSelectedFile(file);
      // Reset upload state when a new file is selected
      if (uploadState !== "idle") {
        resetUpload();
      }
    },
    [uploadState, resetUpload],
  );

  const handleYearsChange = useCallback((years: string[]) => {
    setSelectedYears(years);
  }, []);

  const handleClearServerDataChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setClearServerData(e.target.checked);
    },
    [],
  );
  //#endregion

  //#region Derived state
  const isUploading = uploadState === "uploading";
  const canUpload = !!selectedFile && !isUploading && uploadState !== "success";

  const progressColor =
    uploadState === "success"
      ? "success"
      : uploadState === "error"
        ? "error"
        : "primary";
  //#endregion

  if (!canAccess) return <AccessDenied resourceName={item.name} />;

  //#region Render
  return (
    <Create
      sx={{
        display: "flex",
        "& .RaCreate-main": {
          flex: "1 1 0%",
          minWidth: 0,
        },
      }}
      aside={
        <UploadAside
          item={item}
          emptyOrData={emptyOrData}
          onEmptyOrDataChange={setEmptyOrData}
          notify={notify}
        />
      }
      resource={path}
      id={id || item.slug}
      title={`Upload Access Database for ${item.slug}`}
    >
      <SimpleForm onSubmit={handleSave} toolbar={false}>
        <Container maxWidth={false} sx={{ p: 3 }}>
          <Grid
            container
            spacing={4}
            sx={{
              width: "100%",
              margin: 0,
              minHeight: "50vh",
            }}
          >
            {/* მარცხენა სვეტი - ოფციები */}
            <Grid
              size={6}
              sx={{
                display: "flex",
                "& > .MuiPaper-root": {
                  width: "100%",
                  display: "flex",
                  flexDirection: "column",
                },
              }}
            >
              <Paper elevation={3} sx={{ p: 3, flex: 1 }}>
                <Typography variant="h5" gutterBottom sx={{ mb: 3 }}>
                  პარამეტრები
                </Typography>

                <Box
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    height: "100%",
                  }}
                >
                  <Box sx={{ mb: 4 }}>
                    <FormControl component="fieldset" fullWidth>
                      <FormControlLabel
                        control={
                          <Switch
                            checked={clearServerData}
                            onChange={handleClearServerDataChange}
                            color="primary"
                          />
                        }
                        label={
                          <Box>
                            <Typography variant="subtitle1">
                              არსებული მონაცემების წაშლა
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              არსებული მონაცემების წაშლა და ახალი ფაილის
                              ატვირთვა
                            </Typography>
                          </Box>
                        }
                      />
                    </FormControl>
                  </Box>

                  <Box sx={{ flex: 1 }}>
                    <Typography variant="subtitle1" sx={{ mb: 1 }}>
                      კონკრეტული წლების არჩევა (წასაშლელად)
                    </Typography>
                    <Typography
                      variant="body2"
                      color="text.secondary"
                      sx={{ mb: 2 }}
                    >
                      აირჩიეთ წლები, რომლებიც გინდათ წაიშალოს
                    </Typography>
                    <MultiSelectYears
                      value={selectedYears}
                      onChange={handleYearsChange}
                    />
                  </Box>
                </Box>
              </Paper>
            </Grid>

            {/* მარჯვენა სვეტი - ფაილის ატვირთვა */}
            <Grid
              size={6}
              sx={{
                display: "flex",
                "& > .MuiPaper-root": {
                  width: "100%",
                  display: "flex",
                  flexDirection: "column",
                },
              }}
            >
              <Paper elevation={3} sx={{ p: 3, flex: 1 }}>
                <Typography variant="h5" gutterBottom sx={{ mb: 3 }}>
                  ფაილი
                </Typography>

                <Box
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "center",
                    alignItems: "center",
                    border: "2px dashed",
                    borderColor: "divider",
                    borderRadius: 2,
                    p: 4,
                    flex: 1,
                    bgcolor: "action.hover",
                  }}
                >
                  <AccessUploadButton onFileSelected={handleFileSelected} />
                  {selectedFile && (
                    <Typography
                      variant="body2"
                      color="primary.main"
                      sx={{ mt: 2 }}
                    >
                      {selectedFile.name}
                    </Typography>
                  )}
                </Box>
              </Paper>
            </Grid>

            {/* სტატუს პანელი */}
            <Grid size={12}>
              <Paper
                elevation={3}
                sx={{
                  p: 3,
                  width: "100%",
                  bgcolor:
                    uploadState === "success"
                      ? "success.light"
                      : "background.paper",
                }}
              >
                <Box sx={{ mb: 2 }}>
                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      mb: 1,
                    }}
                  >
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 2,
                      }}
                    >
                      <Typography variant="h6">Upload Status</Typography>
                      <ConnectionIndicator
                        state={connectionState}
                        onReconnect={reconnect}
                      />
                    </Box>
                    <Typography
                      variant="h6"
                      color="primary.main"
                      sx={{ minWidth: 48, textAlign: "right" }}
                    >
                      {Math.round(uploadProgress)}%
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={uploadProgress}
                    color={progressColor}
                    sx={{
                      height: 10,
                      borderRadius: 5,
                      "& .MuiLinearProgress-bar": {
                        borderRadius: 5,
                      },
                    }}
                  />
                </Box>
                {uploadMessage && (
                  <Typography
                    variant="body1"
                    color={
                      uploadState === "success"
                        ? "success.dark"
                        : uploadState === "error"
                          ? "error.main"
                          : "text.primary"
                    }
                    sx={{ minHeight: 24 }}
                  >
                    {uploadMessage}
                  </Typography>
                )}
              </Paper>
            </Grid>
          </Grid>

          {/* ქვედა პანელი */}
          <Paper
            elevation={3}
            sx={{
              mt: 4,
              p: 2,
              width: "100%",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <Typography variant="body2" color="text.secondary">
              {selectedFile
                ? `Ready to upload: ${selectedFile.name}`
                : "Select a database file to upload"}
            </Typography>
            <Box sx={{ display: "flex", gap: 2 }}>
              <Button
                variant="outlined"
                onClick={() => navigate(-1)}
                startIcon={<ArrowBack />}
              >
                Back
              </Button>
              {isUploading && (
                <Button
                  variant="outlined"
                  color="warning"
                  onClick={cancelUpload}
                  startIcon={<Cancel />}
                >
                  Cancel
                </Button>
              )}
              <Button
                variant="contained"
                color="primary"
                onClick={(e) => {
                  e.preventDefault();
                  handleSave();
                }}
                disabled={!canUpload}
                endIcon={<Upload />}
                sx={{ minWidth: 150 }}
              >
                {uploadState === "success" ? "Re-upload" : "Upload"}
              </Button>
            </Box>
          </Paper>
        </Container>
      </SimpleForm>
    </Create>
  );
  //#endregion
};
