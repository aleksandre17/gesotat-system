export const KIDS_SITE_CONTRACT = Object.freeze({
  code: import.meta.env.VITE_SITE_CONTRACT_CODE || "KIDS_PORTAL_V1",
  revision: Number(import.meta.env.VITE_SITE_CONTRACT_REVISION || 8),
  pages: Object.freeze({
    root: 7,
    goals: 8,
    resources: 9,
    glossary: 10,
    statistics: 11,
    classifiers: 12,
  }),
});

export const PLATFORM_API_BASE_URL = String(
  import.meta.env.VITE_PLATFORM_API_URL || "",
).replace(/\/$/, "");

export const PLATFORM_AUTH_MODE = import.meta.env.VITE_PLATFORM_AUTH_MODE || "oidc";
export const TOKEN_STORAGE_KEY = import.meta.env.VITE_PLATFORM_TOKEN_STORAGE_KEY || "geostat_access_token";

export function pageIdFor(name) {
  const pageId = KIDS_SITE_CONTRACT.pages[name];
  if (!Number.isInteger(pageId)) {
    throw new Error(`Unknown KIDS contract page: ${name}`);
  }
  return pageId;
}
