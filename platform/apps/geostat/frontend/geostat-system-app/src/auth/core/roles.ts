/**
 * Known role constants — match exactly the role names in the backend.
 *
 * Two tiers:
 *  1. System roles     — coarse-grained, cross-department privileges
 *  2. Department roles — grant access to a specific department's routes
 *
 * Fine-grained feature access should always use PERMISSIONS, not ROLES.
 *
 * Adding a new department:
 *   1. Add DEPT_MY_NEW: "DEPT_MY_NEW" here
 *   2. Add the matching permission in permissions.ts
 */
export const ROLES = {
  // ── System ───────────────────────────────────────────────────────────────
  ADMIN: "ADMIN",
  MANAGER: "MANAGER",
  VIEWER: "VIEWER",

  // ── Departments ──────────────────────────────────────────────────────────
  DEPT_PRICES: "DEPT_PRICES",
  DEPT_FOREIGN_TRADE: "DEPT_FOREIGN_TRADE",
  DEPT_AGRICULTURE: "DEPT_AGRICULTURE",
  DEPT_NATIONAL_ACCOUNTS: "DEPT_NATIONAL_ACCOUNTS",
  DEPT_PUBLIC_RELATIONS: "DEPT_PUBLIC_RELATIONS",
  DEPT_BUSINESS_STATISTICS: "DEPT_BUSINESS_STATISTICS",
  DEPT_DEMOGRAPHICS: "DEPT_DEMOGRAPHICS",
  DEPT_SOCIAL_STATISTICS: "DEPT_SOCIAL_STATISTICS",
  DEPT_IT: "DEPT_IT",
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];
