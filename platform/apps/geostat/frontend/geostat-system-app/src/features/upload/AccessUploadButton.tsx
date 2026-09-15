import React, { useCallback } from "react";
import { Button } from "@mui/material";
import { useNotify } from "react-admin";

interface AccessUploadButtonProps {
  onFileSelected: (file: File | null) => void;
}

export const AccessUploadButton: React.FC<AccessUploadButtonProps> = ({
  onFileSelected,
}) => {
  const notify = useNotify();

  const handleFileChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      if (!file) {
        notify("No file selected", { type: "error" });
        onFileSelected(null);
        return;
      }

      if (file.size > 500 * 1024 * 1024) {
        notify("File size exceeds 500 MB. Please upload a smaller file.", {
          type: "error",
        });
        onFileSelected(null);
        return;
      }

      onFileSelected(file);
    },
    [notify, onFileSelected],
  );

  return (
    <Button variant="contained" component="label">
      Upload Access Database
      <input type="file" accept=".accdb" hidden onChange={handleFileChange} />
    </Button>
  );
};