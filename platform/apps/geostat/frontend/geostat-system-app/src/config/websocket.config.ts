import { ENV } from "./env";

export interface WebSocketConfig {
  /** WebSocket endpoint URL */
  url: string;
  /** Initial delay before first reconnection attempt (ms) */
  initialReconnectDelay: number;
  /** Maximum reconnect delay after exponential backoff (ms) */
  maxReconnectDelay: number;
  /** Multiplier for exponential backoff */
  backoffMultiplier: number;
  /** STOMP heartbeat: expected interval from server (ms). 0 = disabled */
  heartbeatIncoming: number;
  /** STOMP heartbeat: client send interval (ms). 0 = disabled */
  heartbeatOutgoing: number;
  /** STOMP subscription topic path */
  topicPath: string;
  /** Maximum reconnect attempts before giving up */
  maxReconnectAttempts: number;
  /** Timeout for initial connection (ms) */
  connectionTimeout: number;
}

export const WEBSOCKET_CONFIG: WebSocketConfig = {
  url: ENV.WS_URL,
  initialReconnectDelay: 1_000,
  maxReconnectDelay: 30_000,
  backoffMultiplier: 2,
  heartbeatIncoming: 15_000,
  heartbeatOutgoing: 15_000,
  topicPath: "/user/topic/progress",
  maxReconnectAttempts: 15,
  connectionTimeout: 15_000,
};
