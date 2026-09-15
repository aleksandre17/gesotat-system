import axios, { AxiosProgressEvent, ResponseType } from "axios";

/**
 * HTTP error that carries the response status code.
 * Lets TanStack Query (and other callers) distinguish 4xx client errors
 * from network / 5xx server errors — enabling smart retry logic.
 */
export class HttpError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "HttpError";
  }
}

export interface HttpClientOptions {
  method?: string;
  headers?: Headers | Record<string, string>;
  body?: unknown;
  timeout?: number;
  signal?: AbortSignal;
  onUploadProgress?: (progressEvent: AxiosProgressEvent) => void;
  /**
   * Controls how the response body is decoded.
   *   "json"        — default; parses body as JSON (suitable for API calls)
   *   "blob"        — raw binary as Blob (use for file downloads)
   *   "arraybuffer" — raw binary as ArrayBuffer (use for low-level binary work)
   */
  responseType?: "json" | "blob" | "arraybuffer";
}

export interface HttpClientResponse<T = unknown> {
  status: number;
  headers: Record<string, string>;
  /** Typed response body — shape depends on the requested responseType. */
  body: T;
  /** Convenience alias for body; equals body for JSON responses. */
  json: T;
}

export const httpClient = async <T = unknown>(
  url: string,
  options: HttpClientOptions = {},
): Promise<HttpClientResponse<T>> => {
  let headers: Record<string, string> = {};

  // Convert Headers instance to plain object if needed
  if (options.headers instanceof Headers) {
    options.headers.forEach((value: string, key: string) => {
      headers[key] = value;
    });
  } else if (typeof options.headers === "object") {
    headers = { ...options.headers };
  }

  // Don't override if the caller explicitly provided an Authorization header
  if (!headers["Authorization"]) {
    const token = localStorage.getItem("token");
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  let data = options.body;

  if (data && typeof data === "object" && !(data instanceof FormData)) {
    headers["Content-Type"] = "application/json";
    data = JSON.stringify(data);
  }

  // Let the browser set Content-Type with boundary for FormData
  if (data instanceof FormData) {
    delete headers["Content-Type"];
  }

  try {
    const response = await axios({
      url,
      method: options.method ?? "GET",
      headers,
      data,
      responseType: (options.responseType ?? "json") as ResponseType,
      onUploadProgress: options.onUploadProgress,
      timeout: options.timeout,
      signal: options.signal,
    });

    const responseHeaders: Record<string, string> = {};
    if (response.headers) {
      Object.entries(response.headers).forEach(([key, value]) => {
        if (typeof value === "string") {
          responseHeaders[key] = value;
        }
      });
    }

    return {
      status: response.status,
      headers: responseHeaders,
      body: response.data as T,
      json: response.data as T,
    };
  } catch (error: unknown) {
    // Re-throw abort errors cleanly
    if (axios.isCancel(error)) {
      throw new DOMException("Upload cancelled", "AbortError");
    }
    if (axios.isAxiosError(error) && error.response) {
      throw new HttpError(
        error.response.status,
        error.response.data?.message || error.response.statusText,
      );
    }
    throw error;
  }
};