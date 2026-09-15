import { useEffect, useRef, useCallback, useState } from "react";
import {
  WebSocketService,
  ConnectionState,
} from "../../../services/WebSocketService";
import { WEBSOCKET_CONFIG } from "../../../config";
import type { WebSocketConfig } from "../../../config";

interface UseWebSocketProps {
  taskId: string;
  onProgress: (progress: number, message: string) => void;
  onError: (message: string) => void;
  config?: WebSocketConfig;
}

interface UseWebSocketReturn {
  isConnected: () => boolean;
  connectionState: ConnectionState;
  reconnect: () => void;
}

export const useWebSocket = ({
  taskId,
  onProgress,
  onError,
  config = WEBSOCKET_CONFIG,
}: UseWebSocketProps): UseWebSocketReturn => {
  const serviceRef = useRef<WebSocketService | null>(null);
  const [connectionState, setConnectionState] =
    useState<ConnectionState>("disconnected");

  // Use refs for callbacks so the effect doesn't re-run when they change.
  // This prevents the WebSocket from disconnecting/reconnecting on every parent re-render.
  const onProgressRef = useRef(onProgress);
  const onErrorRef = useRef(onError);
  onProgressRef.current = onProgress;
  onErrorRef.current = onError;

  // Guard against updates after unmount
  const isMountedRef = useRef(true);

  const connectService = useCallback(
    (service: WebSocketService) => {
      service.connect(
        ({ progress, message }) => {
          if (!isMountedRef.current) return;
          onProgressRef.current(progress, message);
        },
        (error) => {
          if (!isMountedRef.current) return;
          onErrorRef.current(error);
        },
        (state) => {
          if (!isMountedRef.current) return;
          setConnectionState(state);
        },
      );
    },
    // onProgressRef and onErrorRef are stable refs — no deps needed
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  useEffect(() => {
    isMountedRef.current = true;

    const service = WebSocketService.create(taskId, config);
    serviceRef.current = service;
    connectService(service);

    return () => {
      isMountedRef.current = false;
      service.disconnect();
      serviceRef.current = null;
    };
  }, [taskId, config, connectService]);

  const isConnected = useCallback(() => {
    return serviceRef.current?.isConnected() ?? false;
  }, []);

  const reconnect = useCallback(() => {
    serviceRef.current?.disconnect();
    const service = WebSocketService.create(taskId, config);
    serviceRef.current = service;
    connectService(service);
  }, [taskId, config, connectService]);

  return { isConnected, connectionState, reconnect };
};