import { PERMISSIONS } from "../core/permissions";
import type { Permission } from "../core/permissions";

type CrudAction = "list" | "create" | "edit" | "delete" | "show";

/**
 * Resource access policy map.
 *
 * Defines what permission each resource + action requires.
 * Used by `authProvider.canAccess` — gates entire routes, not just buttons.
 *
 * Missing entry = accessible to any authenticated user.
 *
 * Adding a new resource:
 *   myResource: { list: PERMISSIONS.admin.manageX, ... }
 *   → React Admin automatically blocks the route if permission is absent.
 */
export const RESOURCE_POLICIES: Record<
  string,
  Partial<Record<CrudAction, Permission>>
> = {
  users: {
    list: PERMISSIONS.admin.manageUsers,
    create: PERMISSIONS.admin.manageUsers,
    edit: PERMISSIONS.admin.manageUsers,
    delete: PERMISSIONS.admin.manageUsers,
  },
  roles: {
    list: PERMISSIONS.admin.manageRoles,
    create: PERMISSIONS.admin.manageRoles,
    edit: PERMISSIONS.admin.manageRoles,
    delete: PERMISSIONS.admin.manageRoles,
  },
  permissions: {
    list: PERMISSIONS.admin.managePermissions,
    create: PERMISSIONS.admin.managePermissions,
    edit: PERMISSIONS.admin.managePermissions,
    delete: PERMISSIONS.admin.managePermissions,
  },
  pages: {
    list: PERMISSIONS.resource.write,
    create: PERMISSIONS.resource.write,
    edit: PERMISSIONS.resource.write,
    delete: PERMISSIONS.resource.delete,
  },
};
