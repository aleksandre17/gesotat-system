import SockJS from "sockjs-client";
import { Client, IMessage } from "@stomp/stompjs";
import { WEBSOCKET_CONFIG, type WebSocketConfig } from "../config";

export interface ProgressUpdate {
  progress: number;
  message: string;
}

export type ConnectionState =
  | "connecting"
  | "connected"
  | "disconnected"
  | "reconnecting"
  | "failed";

type ProgressCallback = (update: ProgressUpdate) => void;
type ErrorCallback = (message: string) => void;
type StateCallback = (state: ConnectionState) => void;

export class WebSocketService {
  private client: Client | null = null;
  private readonly taskId: string;
  private readonly config: WebSocketConfig;

  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private connectionTimeoutTimer: ReturnType<typeof setTimeout> | null = null;
  private intentionalDisconnect = false;
  private connectionState: ConnectionState = "disconnected";

  // Stored callbacks for reconnection
  private onProgressCallback?: ProgressCallback;
  private onErrorCallback?: ErrorCallback;
  private onConnectionStateChange?: StateCallback;

  constructor(taskId: string, config: WebSocketConfig = WEBSOCKET_CONFIG) {
    this.taskId = taskId;
    this.config = config;
  }

  static create(taskId: string, config?: WebSocketConfig): WebSocketService {
    return new WebSocketService(taskId, config);
  }

  connect(
    onProgress: ProgressCallback,
    onError: ErrorCallback,
    onConnectionStateChange?: StateCallback,
  ): void {
    const token = localStorage.getItem("token");
    if (!token) {
      onError("No authentication token found");
      return;
    }

    this.onProgressCallback = onProgress;
    this.onErrorCallback = onError;
    this.onConnectionStateChange = onConnectionStateChange;
    this.intentionalDisconnect = false;

    this.createAndActivateClient(token);
  }

  private createAndActivateClient(token: string): void {
    // Clean up any existing client
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // Ignore errors during cleanup
      }
      this.client = null;
    }

    this.setConnectionState("connecting");
    this.startConnectionTimeout();

    this.client = new Client({
      // Create a NEW SockJS instance every time STOMP needs to (re)connect.
      // SockJS instances cannot be reused after close.
      webSocketFactory: () => new SockJS(this.config.url),
      connectHeaders: {
        "X-Task-ID": this.taskId,
        Authorization: `Bearer ${token}`,
      },
      // Disable STOMP's built-in reconnect — we handle it manually
      // to avoid double-counting reconnect attempts
      reconnectDelay: 0,
      heartbeatIncoming: this.config.heartbeatIncoming,
      heartbeatOutgoing: this.config.heartbeatOutgoing,

      debug: (msg) => {
        if (import.meta.env.DEV) {
          console.debug("[STOMP]", msg);
        }
      },

      onConnect: () => {
        this.clearConnectionTimeout();
        this.reconnectAttempts = 0;
        this.setConnectionState("connected");
        console.info(`[WebSocket] Connected (taskId: ${this.taskId})`);

        this.client?.subscribe(this.config.topicPath, (message: IMessage) => {
          try {
            const update: ProgressUpdate = JSON.parse(message.body);
            this.onProgressCallback?.(update);
          } catch {
            this.onErrorCallback?.("Error processing progress update");
          }
        });
      },

      onStompError: (frame) => {
        console.error(
          "[WebSocket] STOMP error:",
          frame.headers?.message || "Unknown",
        );
        // Don't call handleReconnect here — onWebSocketClose will fire and handle it
        this.onErrorCallback?.(
          `STOMP error: ${frame.headers?.message || "Unknown error"}`,
        );
      },

      onWebSocketError: () => {
        // Don't call handleReconnect here — onWebSocketClose will fire after this
        console.warn("[WebSocket] Transport error");
      },

      onWebSocketClose: () => {
        this.clearConnectionTimeout();

        if (this.intentionalDisconnect) {
          this.setConnectionState("disconnected");
          return;
        }

        // Unexpected close — attempt reconnection
        this.scheduleReconnect();
      },

      onDisconnect: () => {
        if (this.intentionalDisconnect) {
          this.setConnectionState("disconnected");
        }
      },
    });

    this.client.activate();
  }

  /**
   * Exponential backoff reconnection.
   * Delay = min(initialDelay * multiplier^attempt, maxDelay)
   */
  private scheduleReconnect(): void {
    if (this.intentionalDisconnect) return;

    this.reconnectAttempts++;

    if (this.reconnectAttempts > this.config.maxReconnectAttempts) {
      console.error(
        `[WebSocket] Max reconnect attempts (${this.config.maxReconnectAttempts}) reached.`,
      );
      this.setConnectionState("failed");
      this.onErrorCallback?.(
        `Connection failed after ${this.config.maxReconnectAttempts} attempts. Please refresh the page.`,
      );
      this.cleanupClient();
      return;
    }

    const delay = Math.min(
      this.config.initialReconnectDelay *
        Math.pow(this.config.backoffMultiplier, this.reconnectAttempts - 1),
      this.config.maxReconnectDelay,
    );

    console.info(
      `[WebSocket] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.config.maxReconnectAttempts})`,
    );
    this.setConnectionState("reconnecting");

    this.reconnectTimer = setTimeout(() => {
      if (this.intentionalDisconnect) return;

      const token = localStorage.getItem("token");
      if (!token) {
        this.onErrorCallback?.(
          "No authentication token found during reconnection",
        );
        this.setConnectionState("failed");
        return;
      }

      this.createAndActivateClient(token);
    }, delay);
  }

  private startConnectionTimeout(): void {
    this.clearConnectionTimeout();
    this.connectionTimeoutTimer = setTimeout(() => {
      if (this.connectionState === "connecting") {
        console.warn("[WebSocket] Connection timeout");
        this.onErrorCallback?.("Connection timeout — retrying...");
        // Force close and let the reconnect logic handle it
        this.cleanupClient();
        this.scheduleReconnect();
      }
    }, this.config.connectionTimeout);
  }

  private clearConnectionTimeout(): void {
    if (this.connectionTimeoutTimer) {
      clearTimeout(this.connectionTimeoutTimer);
      this.connectionTimeoutTimer = null;
    }
  }

  private cleanupClient(): void {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // Ignore
      }
      this.client = null;
    }
  }

  private setConnectionState(state: ConnectionState): void {
    if (this.connectionState === state) return;
    this.connectionState = state;
    this.onConnectionStateChange?.(state);
  }

  async disconnect(): Promise<void> {
    this.intentionalDisconnect = true;

    // Clear pending timers
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.clearConnectionTimeout();

    if (this.client) {
      try {
        await this.client.deactivate();
      } catch {
        // Ignore deactivation errors
      }
      this.client = null;
    }

    this.reconnectAttempts = 0;
    this.setConnectionState("disconnected");
  }

  isConnected(): boolean {
    return this.connectionState === "connected" && !!this.client?.connected;
  }

  getConnectionState(): ConnectionState {
    return this.connectionState;
  }
}
