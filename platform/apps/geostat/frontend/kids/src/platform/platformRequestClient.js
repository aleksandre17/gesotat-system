import { PLATFORM_API_BASE_URL, PLATFORM_AUTH_MODE, TOKEN_STORAGE_KEY } from "./siteContract";

const REQUEST_TIMEOUT_MS = 30_000;

function token() {
  if (PLATFORM_AUTH_MODE === "none") return null;
  if (typeof window === "undefined") return null;
  return window.sessionStorage.getItem(TOKEN_STORAGE_KEY) || null;
}

function queryString(query = {}) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") return;
    if (Array.isArray(value)) value.forEach((item) => params.append(key, String(item)));
    else params.set(key, String(value));
  });
  const encoded = params.toString();
  return encoded ? `?${encoded}` : "";
}

export function buildPlatformRequest(requestCase, { signal, requestId } = {}) {
  if (!PLATFORM_API_BASE_URL) {
    throw new Error("VITE_PLATFORM_API_URL is required for canonical KIDS requests");
  }
  if (!requestCase?.method || !requestCase.path?.startsWith("/")) {
    throw new Error("Malformed canonical request case");
  }

  const headers = {
    Accept: "application/json",
    "Accept-Profile": `urn:geostat:kids:${requestCase.path.includes("/pages/11") ? "statistics" : "portal"}:v${requestCase.revision || 8}`,
    "X-Request-ID": requestId || crypto.randomUUID(),
  };
  const accessToken = token();
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  if (requestCase.body !== undefined) headers["Content-Type"] = "application/json";

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  if (signal) signal.addEventListener("abort", () => controller.abort(), { once: true });

  return {
    url: `${PLATFORM_API_BASE_URL}${requestCase.path}${queryString(requestCase.query)}`,
    init: {
      method: requestCase.method,
      headers,
      body: requestCase.body === undefined ? undefined : JSON.stringify(requestCase.body),
      signal: controller.signal,
    },
    dispose: () => clearTimeout(timeout),
  };
}

export async function sendPlatformRequest(requestCase, options) {
  const request = buildPlatformRequest(requestCase, options);
  try {
    return await fetch(request.url, request.init);
  } finally {
    request.dispose();
  }
}
