/**
 * Access-field normalization utilities for data providers.
 *
 * Spring/JPA returns nested entity references ({ id, ...fields }).
 * The app works with flat scalar IDs.
 *
 * normalizeAccessFields  — response  → app  (deserialize)
 * serializeAccessFields  — app       → request  (serialize / Spring JPA)
 *
 * Covered fields:
 *   user / users / userId / userIds
 *   role / roles / roleId / roleIds
 *   permission / permissions / permissionId / permissionIds
 */

import { mutator } from "./mutator";

export type { AnyRecord } from "./mutator";

// ── Helpers ───────────────────────────────────────────────────────────────────

type IdObject = { id: number | string; name?: string };

function isIdObject(val: unknown): val is IdObject {
  return (
    typeof val === "object" &&
    val !== null &&
    !Array.isArray(val) &&
    "id" in (val as object)
  );
}

function isIdObjectArray(val: unknown): val is IdObject[] {
  return Array.isArray(val) && val.length > 0 && val.every(isIdObject);
}

// ── Deserialize (response → app) ──────────────────────────────────────────────

/**
 * Normalizes access fields from the backend response to flat scalars.
 *
 * Examples:
 *   user:  { id: 5, username: "john" }   → userId: 5      (user key removed)
 *   users: [{ id: 1 }, { id: 2 }]        → userIds: [1, 2] (users key removed)
 *   role:  { id: 3, name: "PRICES" }     → roleId: 3, role: "PRICES"
 *   roles: [{ id: 1 }, { id: 3 }]        → roleIds: [1, 3] (roles key removed)
 *   permission:  { id: 7, name: "WRITE" }→ permissionId: 7, permission: "WRITE"
 *   permissions: [{ id: 1, name: "A" }]  → permissionIds: [1], permissions: ["A"]
 */
export const normalizeAccessFields = mutator()
  // ── user ──────────────────────────────────────────────────────────────────
  .when(
    (d) => isIdObject(d.user),
    (m) =>
      m
        .set("userId", (d) => d.userId ?? (d.user as IdObject).id) // preserve existing userId
        .omit("user"),
  )
  .when(
    (d) => isIdObjectArray(d.users),
    (m) =>
      m
        .set("userIds", (d) => (d.users as IdObject[]).map((u) => u.id))
        .omit("users"),
  )
  // ── role ──────────────────────────────────────────────────────────────────
  .when(
    (d) => isIdObject(d.role),
    (m) =>
      m
        .set("roleId", (d) => d.roleId ?? (d.role as IdObject).id)
        .set("role", (d) => (d.role as IdObject).name ?? null),
  )
  .when(
    (d) => isIdObjectArray(d.roles),
    (m) =>
      m
        .set("roleIds", (d) => (d.roles as IdObject[]).map((r) => r.id))
        .omit("roles"),
  )
  // ── permission ────────────────────────────────────────────────────────────
  .when(
    (d) => isIdObject(d.permission),
    (m) =>
      m
        .set(
          "permissionId",
          (d) => d.permissionId ?? (d.permission as IdObject).id,
        )
        .set("permission", (d) => (d.permission as IdObject).name ?? null),
  )
  .when(
    (d) => isIdObjectArray(d.permissions),
    (m) =>
      m
        .set("permissionIds", (d) =>
          (d.permissions as IdObject[]).map((p) => p.id),
        )
        .set("permissions", (d) =>
          (d.permissions as IdObject[]).map((p) => p.name ?? null),
        ),
  )
  .build();

// ── Serialize (app → request) ─────────────────────────────────────────────────

/**
 * Converts flat scalar access fields back to Spring/JPA nested entity references.
 *
 * Examples:
 *   userId: 5        → user: { id: 5 }      (userId removed)
 *   userIds: [1, 2]  → users: [{ id: 1 }, { id: 2 }] (userIds removed)
 *   roleId: 3        → role: { id: 3 }       (roleId + role name removed)
 *   roleIds: [1, 3]  → roles: [{ id: 1 }, { id: 3 }] (roleIds removed)
 *   permissionId: 7  → permission: { id: 7 } (permissionId + name removed)
 *   permissionIds: … → permissions: [{ id }] (permissionIds + name array removed)
 *
 * If only a name string is present without an ID, it is dropped —
 * Spring expects an entity reference, not a bare name.
 */
export const serializeAccessFields = mutator()
  // ── user ──────────────────────────────────────────────────────────────────
  .when(
    (d) => d.userId != null,
    (m) => m.set("user", (d) => ({ id: d.userId })).omit("userId"),
  )
  .when(
    (d) => Array.isArray(d.userIds),
    (m) =>
      m
        .set("users", (d) =>
          (d.userIds as (number | string)[]).map((id) => ({ id })),
        )
        .omit("userIds"),
  )
  // ── role ──────────────────────────────────────────────────────────────────
  .branch(
    (d) => d.roleId != null,
    (m) => m.set("role", (d) => ({ id: d.roleId })).omit("roleId"),
    (m) => m.omit("role"), // name-only string → drop (Spring needs entity ref)
  )
  .when(
    (d) => Array.isArray(d.roleIds),
    (m) =>
      m
        .set("roles", (d) =>
          (d.roleIds as (number | string)[]).map((id) => ({ id })),
        )
        .omit("roleIds"),
  )
  // ── permission ────────────────────────────────────────────────────────────
  .branch(
    (d) => d.permissionId != null,
    (m) =>
      m.set("permission", (d) => ({ id: d.permissionId })).omit("permissionId"),
    (m) => m.omit("permission"), // name-only string → drop
  )
  .branch(
    (d) => Array.isArray(d.permissionIds),
    (m) =>
      m
        .set("permissions", (d) =>
          (d.permissionIds as (number | string)[]).map((id) => ({ id })),
        )
        .omit("permissionIds"),
    (m) => m.omit("permissions"), // string array without IDs → drop
  )
  .build();
