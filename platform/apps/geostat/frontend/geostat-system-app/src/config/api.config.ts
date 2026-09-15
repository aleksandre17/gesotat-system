/**
 * Shared API query configuration.
 *
 * Centralises pagination/sort/filter presets and cache timings
 * so they are not duplicated across hooks and input components.
 */

// ── Lookup queries ────────────────────────────────────────────────────────────

/**
 * Standard query for loading a full reference list (roles, permissions…).
 * Used wherever all items must be visible in a grouped checkbox input.
 */
export const LOOKUP_QUERY = {
  pagination: { page: 1, perPage: 100 },
  sort: { field: "name", order: "ASC" as const },
} as const;

// ── Dashboard ─────────────────────────────────────────────────────────────────

/** Cache settings shared by all dashboard data hooks. */
export const DASHBOARD_CACHE = {
  staleTime: 60_000,          // 1 min — dashboard data refreshes often
  refetchOnWindowFocus: false,
} as const;

/** Revenue chart query — last 30 days, chronological order. */
export const DASHBOARD_REVENUE_QUERY = {
  pagination: { page: 1, perPage: 30 },
  sort: { field: "date", order: "ASC" as const },
  filter: { period: "last30days" },
} as const;