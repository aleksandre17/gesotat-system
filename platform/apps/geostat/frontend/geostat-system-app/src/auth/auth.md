# Auth Architecture

## Overview

Three-tier RBAC system built on JWT tokens, React Admin v5, and a typed permission layer.

```
JWT (localStorage)
  └── roles[]
        └── permissions[]
              └── UserAuth { roles, permissions }
                    ├── useHasPermission(...)     → fine-grained UI buttons
                    ├── useHasAnyPermission(...)  → admin/menu checks
                    ├── useHasRole(...)           → coarse-grained role check
                    └── useCanAccessPage(item)    → dynamic route access
```

---

## JWT Structure

Backend must generate tokens with this exact structure:

```json
{
  "sub": "username",
  "name": "Display Name",
  "roles": {
    "userId": 42,
    "roles": [
      {
        "id": 1,
        "name": "DEPT_PRICES",
        "permissions": [
          { "id": 5, "name": "MANAGE_DEPT_PRICES" }
        ]
      }
    ]
  },
  "exp": 1234567890
}
```

Decoded payload is stored in `localStorage["user"]`.
Access token → `localStorage["token"]`.
Refresh token → `localStorage["refreshToken"]`.

---

## Roles

Defined in `src/auth/roles.ts`.

### System Roles

| Role      | Purpose                               |
|-----------|---------------------------------------|
| `ADMIN`   | Full system access                    |
| `MANAGER` | Cross-department management           |
| `VIEWER`  | Read-only access                      |

### Department Roles

| Role                       | Department              |
|----------------------------|-------------------------|
| `DEPT_PRICES`              | ფასები (id: 1)          |
| `DEPT_FOREIGN_TRADE`       | საგარეო ვაჭრობა (id: 7) |
| `DEPT_AGRICULTURE`         | სოფლის მეურნეობა (id: 10) |
| `DEPT_NATIONAL_ACCOUNTS`   | ეროვნული ანგარიშები (id: 13) |
| `DEPT_PUBLIC_RELATIONS`    | საზოგადოებასთან ურთიერთობა (id: 16) |
| `DEPT_BUSINESS_STATISTICS` | ბიზნეს სტატისტიკა (id: 20) |
| `DEPT_DEMOGRAPHICS`        | დემოგრაფია (id: 33)     |
| `DEPT_SOCIAL_STATISTICS`   | სოციალური სტატისტიკა (id: 35) |
| `DEPT_IT`                  | IT (id: 67)             |

---

## Permissions

Defined in `src/auth/permissions.ts`. Auto-derived `Permission` union type — adding one line updates the type automatically.

### `resource.*` — Global data access (admin-level)

| Constant                      | Value             |
|-------------------------------|-------------------|
| `PERMISSIONS.resource.read`   | `READ_RESOURCE`   |
| `PERMISSIONS.resource.write`  | `WRITE_RESOURCE`  |
| `PERMISSIONS.resource.delete` | `DELETE_RESOURCE` |

### `admin.*` — Panel management

| Constant                          | Value                 |
|-----------------------------------|-----------------------|
| `PERMISSIONS.admin.manageUsers`   | `MANAGE_USERS`        |
| `PERMISSIONS.admin.manageRoles`   | `MANAGE_ROLES`        |
| `PERMISSIONS.admin.managePermissions` | `MANAGE_PERMISSIONS` |

### `department.*` — Per-department data management

| Constant                                    | Value                          |
|---------------------------------------------|--------------------------------|
| `PERMISSIONS.department.prices`             | `MANAGE_DEPT_PRICES`           |
| `PERMISSIONS.department.foreignTrade`       | `MANAGE_DEPT_FOREIGN_TRADE`    |
| `PERMISSIONS.department.agriculture`        | `MANAGE_DEPT_AGRICULTURE`      |
| `PERMISSIONS.department.nationalAccounts`   | `MANAGE_DEPT_NATIONAL_ACCOUNTS` |
| `PERMISSIONS.department.publicRelations`    | `MANAGE_DEPT_PUBLIC_RELATIONS` |
| `PERMISSIONS.department.businessStatistics` | `MANAGE_DEPT_BUSINESS_STATISTICS` |
| `PERMISSIONS.department.demographics`       | `MANAGE_DEPT_DEMOGRAPHICS`     |
| `PERMISSIONS.department.socialStatistics`   | `MANAGE_DEPT_SOCIAL_STATISTICS` |
| `PERMISSIONS.department.it`                 | `MANAGE_DEPT_IT`               |

### Role → Permission mapping (backend DB setup)

| Role                       | Permissions                          |
|----------------------------|--------------------------------------|
| `ADMIN`                    | `WRITE_RESOURCE`, `DELETE_RESOURCE`, `MANAGE_USERS`, `MANAGE_ROLES`, `MANAGE_PERMISSIONS` |
| `MANAGER`                  | `WRITE_RESOURCE`, `READ_RESOURCE`    |
| `VIEWER`                   | `READ_RESOURCE`                      |
| `DEPT_PRICES`              | `MANAGE_DEPT_PRICES`                 |
| `DEPT_FOREIGN_TRADE`       | `MANAGE_DEPT_FOREIGN_TRADE`          |
| `DEPT_AGRICULTURE`         | `MANAGE_DEPT_AGRICULTURE`            |
| `DEPT_NATIONAL_ACCOUNTS`   | `MANAGE_DEPT_NATIONAL_ACCOUNTS`      |
| `DEPT_PUBLIC_RELATIONS`    | `MANAGE_DEPT_PUBLIC_RELATIONS`       |
| `DEPT_BUSINESS_STATISTICS` | `MANAGE_DEPT_BUSINESS_STATISTICS`    |
| `DEPT_DEMOGRAPHICS`        | `MANAGE_DEPT_DEMOGRAPHICS`           |
| `DEPT_SOCIAL_STATISTICS`   | `MANAGE_DEPT_SOCIAL_STATISTICS`      |
| `DEPT_IT`                  | `MANAGE_DEPT_IT`                     |

---

## Hooks

All hooks are exported from `src/auth/index.ts`.

```ts
// All required permissions must be present
useHasPermission(PERMISSIONS.resource.write)

// At least one permission must be present
useHasAnyPermission(PERMISSIONS.admin.manageUsers, PERMISSIONS.resource.write)

// Coarse-grained role check — prefer permissions for feature decisions
useHasRole(ROLES.ADMIN)

// Dynamic page/route access — checks role + userId ownership
useCanAccessPage(item)  // item: { role?, userId? }
```

---

## Defense Layers

### Layer 1 — Route (React Admin `canAccess`)

`signProvider.canAccess` reads `RESOURCE_POLICIES` from `src/auth/policies.ts`.
Blocks entire resource routes before rendering.

```
GET /users → requires MANAGE_USERS
GET /roles → requires MANAGE_ROLES
GET /pages → requires WRITE_RESOURCE
```

No policy entry = accessible to all authenticated users.

### Layer 2 — Menu visibility

`RenderMenuItem` calls `shouldShowItem` for every tree node.

```
isAdmin (WRITE_RESOURCE | MANAGE_USERS) → show everything
item.role set → userRoles.includes(item.role)?
item.userId set → currentUserId === item.userId?
no restriction on item → check nearest restricting ancestor
no restricting ancestor found → public, show
```

### Layer 3 — Route render (`AccessUploadPage`)

`useCanAccessPage(item)` runs on every dynamic route render.
Returns `<AccessDenied />` if access is denied — catches direct URL navigation.

---

## Dynamic Page Access (Department Routes)

### Data flow

```
Settings tree (from backend)
  ├── ფასები (DIRECTORY)   role="DEPT_PRICES"
  │     ├── cpi (PAGE)     role=null  ← getLeafPaths inherits "DEPT_PRICES"
  │     └── ppi (PAGE)     role=null  ← getLeafPaths inherits "DEPT_PRICES"
  └── IT (DIRECTORY)       role="DEPT_IT"
        └── gis (PAGE)     role=null  ← inherits "DEPT_IT"
```

`getLeafPaths` in `App.tsx` propagates `role` (and `userId`) from the nearest ancestor directory down to every leaf page. By the time a leaf page is rendered, its `item.role` is already set.

### Access check priority (`canAccessItem`)

```
1. isAdmin (has WRITE_RESOURCE or MANAGE_USERS) → ✅ always allow
2. item.role set → userRoles.includes(item.role)?
3. item.userId set → currentUserId === item.userId?
4. neither → ✅ public
```

`canAccessItem` is a pure function — safe in callbacks and tree traversals.
`useCanAccessPage` is the hook wrapper that reads auth state from React Admin.

---

## Admin Workflow — Assigning a Department

1. **Pages** → Edit a top-level `DIRECTORY` node (e.g. "ფასები")
2. Set **Department Role** → select `DEPT_PRICES` from the roles dropdown
3. **Users** → Edit a user → assign role `DEPT_PRICES`
4. That user now sees and can access only `/prices/**` routes
5. Admin (`WRITE_RESOURCE`) always sees everything

---

## Adding a New Department

**Frontend:**

1. `roles.ts` → add `DEPT_MY_NEW: "DEPT_MY_NEW"`
2. `permissions.ts` → add `myNew: "MANAGE_DEPT_MY_NEW"` under `department`

**Backend:**

1. Insert role `DEPT_MY_NEW` into the roles table
2. Insert permission `MANAGE_DEPT_MY_NEW` into the permissions table
3. Assign the permission to the role

**Page setup:**

1. Create the department directory in Pages
2. Set **Department Role** → `DEPT_MY_NEW`
3. Assign the role to the relevant user

---

## Backend Requirements

### Page entity

```java
@ManyToOne
@JoinColumn(name = "role_id")
private Role role;  // nullable FK — department access gate

// DTO getter — returns the role name string to frontend
public String getRole() {
    return role != null ? role.getName() : null;
}
```

### JWT generation

The `roles` claim must contain permission objects `{ id, name }`, not plain strings:

```java
// ✅ Correct
.claim("roles", Map.of(
    "userId", user.getId(),
    "roles", user.getRoles().stream().map(role -> Map.of(
        "id", role.getId(),
        "name", role.getName(),
        "permissions", role.getPermissions().stream()
            .map(p -> Map.of("id", p.getId(), "name", p.getName()))
            .toList()
    )).toList()
))
```

---

## File Map

```
src/auth/
  index.ts                         — single public barrel (all external imports go here)

  core/
    types.ts                       — JwtPayload, JwtRolesClaim, RoleRecord, UserAuth
    roles.ts                       — ROLES constants (system + department)
    permissions.ts                 — PERMISSIONS constants + auto-derived Permission type
    index.ts

  authentication/
    storage.ts                     — localStorage helpers (getToken, setTokens, clearSession…)
    provider.ts                    — React Admin AuthProvider (login, logout, checkAuth, canAccess…)
    index.ts

  authorization/
    policies.ts                    — RESOURCE_POLICIES for canAccess route-level gating
    access.ts                      — AccessibleItem, canAccessItem (pure fn), getStoredUserId
    index.ts

  hooks/
    useHasPermission.ts            — useHasPermission (ALL) / useHasAnyPermission (ANY)
    useHasRole.ts                  — useHasRole
    useCanAccessPage.ts            — useCanAccessPage (hook wrapper around canAccessItem)
    index.ts

src/Layout/
  Menu.tsx                         — admin menu items gated by useHasAnyPermission
  RenderMenuItem.tsx               — dynamic menu tree with shouldShowItem

src/App.tsx                        — PageItem type, getLeafPaths (role/userId inheritance)
src/pages/test/
  AccessUploadPage.tsx             — useCanAccessPage(item) route guard
```