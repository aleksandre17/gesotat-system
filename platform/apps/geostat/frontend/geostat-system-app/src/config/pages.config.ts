/**
 * Pages / settings-tree query & cache configuration.
 *
 * Used by:
 *  - SettingsLoader   — loads the menu tree on startup
 *  - PageList         — drives the List component sort/filter/pagination
 *  - pagesDataProvider — default sort for the pages resource
 */

/** Query params sent to GET /pages when loading the full settings tree. */
export const PAGES_QUERY = {
  filter: { rootsOnly: true },
  pagination: { page: 1, perPage: 1000 },
  sort: { field: "orderIndex", order: "ASC" as const },
} as const;

/** TanStack Query cache settings for the settings tree. */
export const PAGES_CACHE = {
  staleTime: 5 * 60 * 1000,  // treat as fresh for 5 min
  gcTime:    10 * 60 * 1000, // keep in memory for 10 min
  refetchOnWindowFocus: false,
} as const;