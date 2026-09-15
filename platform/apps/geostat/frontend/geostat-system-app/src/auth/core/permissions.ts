/**
 * ─── Permission Architecture ─────────────────────────────────────────────────
 *
 * PATTERN: Typed constants with auto-derived union type.
 *
 *   Adding a new permission:
 *     admin: { manageReports: "MANAGE_REPORTS" }   ← one line
 *     type Permission automatically becomes: ... | "MANAGE_REPORTS"
 *
 * Three tiers:
 *   resource   — global data CRUD (admin-level)
 *   admin      — user/role/permission panel management
 *   department — per-department data management
 *
 * For automated sync with backend:
 *   npm run generate:permissions
 * ─────────────────────────────────────────────────────────────────────────────
 */
export const PERMISSIONS = {
  /** Global data access — admin-level */
  resource: {
    read: "READ_RESOURCE",
    write: "WRITE_RESOURCE",
    delete: "DELETE_RESOURCE",
  },

  /** Admin panel management */
  admin: {
    manageUsers: "MANAGE_USERS",
    manageRoles: "MANAGE_ROLES",
    managePermissions: "MANAGE_PERMISSIONS",
  },

  /**
   * Department-level data management.
   * Each permission is granted to the corresponding DEPT_* role.
   * Admin (WRITE_RESOURCE) bypasses all of these.
   *
   * Adding a new department: add one line here + one in roles.ts.
   */
  department: {
    prices: "MANAGE_DEPT_PRICES",
    foreignTrade: "MANAGE_DEPT_FOREIGN_TRADE",
    agriculture: "MANAGE_DEPT_AGRICULTURE",
    nationalAccounts: "MANAGE_DEPT_NATIONAL_ACCOUNTS",
    publicRelations: "MANAGE_DEPT_PUBLIC_RELATIONS",
    businessStatistics: "MANAGE_DEPT_BUSINESS_STATISTICS",
    demographics: "MANAGE_DEPT_DEMOGRAPHICS",
    socialStatistics: "MANAGE_DEPT_SOCIAL_STATISTICS",
    it: "MANAGE_DEPT_IT",
  },
} as const;

// ── Auto-derived union type ───────────────────────────────────────────────────
// Recursively extracts every leaf string from the nested PERMISSIONS object.
// Adding a new entry above → Permission type updates automatically.

type LeafValues<T> = T extends string
  ? T
  : T extends Record<string, unknown>
    ? LeafValues<T[keyof T]>
    : never;

export type Permission = LeafValues<typeof PERMISSIONS>;
// = "READ_RESOURCE" | "WRITE_RESOURCE" | "DELETE_RESOURCE"
// | "MANAGE_USERS"  | "MANAGE_ROLES"   | "MANAGE_PERMISSIONS"
// | "MANAGE_DEPT_PRICES" | "MANAGE_DEPT_FOREIGN_TRADE" | ...
