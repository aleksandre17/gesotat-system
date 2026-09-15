import { useState, useCallback, useRef } from "react";
import { v4 as uuidv4 } from "uuid";
import { httpClient } from "../../../api/httpClient";
import { throttleWithTrailing } from "../../../utils";
import { UPLOAD_CONFIG } from "../../../config";

export type UploadState =
  | "idle"
  | "uploading"
  | "success"
  | "error"
  | "cancelled";

interface UploadPayload {
  metaDatabaseType: string;
  metaDatabaseName: string;
  years: string[];
  clearServerData: string;
  taskId: string;
  metaDatabaseUrl: string;
  metaDatabaseUser: string;
  metaDatabasePassword: string;
}

interface UseUploadOptions {
  apiBaseUrl: string;
  path: string;
  onProgress?: (progress: number, message: string) => void;
  onSuccess?: () => void;
  onError?: (error: string) => void;
}

interface UseUploadReturn {
  uploadState: UploadState;
  uploadProgress: number;
  uploadMessage: string;
  taskId: string;
  startUpload: (
    file: File,
    payload: Omit<UploadPayload, "taskId">,
  ) => Promise<void>;
  cancelUpload: () => void;
  resetUpload: () => void;
  handleProgressUpdate: (progress: number, message: string) => void;
}

export const useUpload = ({
  apiBaseUrl,
  path,
  onSuccess,
  onError,
}: UseUploadOptions): UseUploadReturn => {
  const [uploadState, setUploadState] = useState<UploadState>("idle");
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadMessage, setUploadMessage] = useState("");
  const [taskId, setTaskId] = useState(() => uuidv4());

  const abortControllerRef = useRef<AbortController | null>(null);

  const throttledUpdate = useRef(
    throttleWithTrailing(
      ({ progress, message }: { progress: number; message: string }) => {
        setUploadProgress(progress);
        setUploadMessage(message);
      },
      30,
    ),
  ).current;

  const handleProgressUpdate = useCallback(
    (progress: number, message: string) => {
      throttledUpdate({ progress, message });

      if (progress >= 100) {
        setUploadState("success");
        onSuccess?.();
      } else if (message?.startsWith("Error")) {
        setUploadState("error");
        onError?.(message);
      }
    },
    [throttledUpdate, onSuccess, onError],
  );

  const startUpload = useCallback(
    async (file: File, payload: Omit<UploadPayload, "taskId">) => {
      // Abort any pending upload
      abortControllerRef.current?.abort();

      const controller = new AbortController();
      abortControllerRef.current = controller;

      setUploadState("uploading");
      setUploadProgress(0);
      setUploadMessage("");

      try {
        const formData = new FormData();
        formData.append("file", file);
        formData.append(
          "payload",
          JSON.stringify({
            ...payload,
            taskId,
          }),
        );

        await httpClient(`${apiBaseUrl}/v1/${path}`, {
          method: "POST",
          body: formData,
          timeout: UPLOAD_CONFIG.timeoutMs,
          signal: controller.signal,
        });
      } catch (error: unknown) {
        if (error instanceof DOMException && error.name === "AbortError") {
          setUploadState("cancelled");
          setUploadMessage("Upload cancelled");
          return;
        }
        const msg = error instanceof Error ? error.message : String(error);
        setUploadState("error");
        setUploadMessage(`Upload failed: ${msg}`);
        onError?.(`Upload failed: ${msg}`);
      }
    },
    [taskId, apiBaseUrl, path, onError],
  );

  const cancelUpload = useCallback(() => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    setUploadState("cancelled");
    setUploadMessage("Upload cancelled");
    setUploadProgress(0);
  }, []);

  const resetUpload = useCallback(() => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    setUploadState("idle");
    setUploadProgress(0);
    setUploadMessage("");
    // Generate a new taskId for the next upload
    setTaskId(uuidv4());
  }, []);

  return {
    uploadState,
    uploadProgress,
    uploadMessage,
    taskId,
    startUpload,
    cancelUpload,
    resetUpload,
    handleProgressUpdate,
  };
};