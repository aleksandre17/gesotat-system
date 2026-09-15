/**
 * ─── Auth public API ──────────────────────────────────────────────────────────
 *
 * Single import point for everything auth-related.
 * Internal layer structure:
 *
 *   core/            ← types + constants (zero deps)
 *   authentication/  ← JWT, session, AuthProvider
 *   authorization/   ← access policies + pure logic
 *   hooks/           ← React enforcement layer
 *
 * ─────────────────────────────────────────────────────────────────────────────
 */

// Core — types & constants
export * from "./core";

// Authentication — session & provider
export { signProvider, profileApi } from "./authentication";
export type {
  ProfileDto,
  UpdateProfileDto,
  ChangePasswordDto,
} from "./authentication";
export {
  clearSession,
  getToken,
  getRefreshToken,
  getStoredUser,
  getStoredPermissions,
  setTokens,
  setUser,
} from "./authentication";

// Authorization — pure access logic
export {
  RESOURCE_POLICIES,
  canAccessItem,
  getStoredUserId,
} from "./authorization";
export type {
  AccessibleItem,
  AccessControl,
  UserContext,
} from "./authorization";

// Hooks — React enforcement
export {
  useHasPermission,
  useHasAnyPermission,
  useHasRole,
  useIsAdmin,
  useAdminMenuAccess,
  useCanAccessPage,
  useUserContext,
} from "./hooks";
export type { AdminMenuAccess } from "./hooks";
