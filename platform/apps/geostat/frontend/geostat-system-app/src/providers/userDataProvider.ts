import { createDataProvider } from "./createDataProvider";
import type { AnyRecord } from "./utils/mutator";

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Converts a mixed array of IDs or entity objects to Spring/JPA entity refs.
 *   [1, 2] → [{ id: 1 }, { id: 2 }]
 *   [{ id: 1, name }] → [{ id: 1 }]
 */
function toEntityRefs(arr: unknown[]): { id: unknown }[] {
  return arr.map((v) =>
    typeof v === "number" || typeof v === "string"
      ? { id: v }
      : { id: (v as { id: unknown }).id },
  );
}

// ── Provider ─────────────────────────────────────────────────────────────────

export const userDataProvider = createDataProvider({
  version: "v1",
  defaultPagination: { page: 1, perPage: 10 },
  defaultSort: { field: "id", order: "ASC" },
  resources: {
    users: {
      // Keep roles as [{id, name}] — UserList needs .name for ChipField.
      // GroupedRolesInput normalizes to IDs internally for selection state.
      deserialize: (data) => data,

      // Handle both shapes that the form may submit:
      //   unchanged: [{id, name}]  (default value, user never toggled)
      //   changed: [number] (GroupedRolesInput stores plain IDs)
      serialize: (data: AnyRecord): AnyRecord => {
        const roles = data.roles as unknown[] | undefined;
        if (!Array.isArray(roles)) return data;
        return { ...data, roles: toEntityRefs(roles) };
      },
    },

    roles: {
      // Keep permissions as [{id, name}] — RoleList needs .name for ChipField.
      // GroupedPermissionsInput normalizes to IDs internally for selection state.
      deserialize: (data) => data,

      // RoleEdit has transform={transformRole} which converts permissions to
      // [{id}] before update() is called — pass through unchanged.
      serialize: (data) => data,
    },
  },
});
