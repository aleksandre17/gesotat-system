import { lazy, Suspense, useMemo } from "react";
import { Admin, CustomRoutes, Resource, useStore } from "react-admin";
import { signProvider } from "./auth";
import dataProviderFactory from "./providers";
import { SettingsProvider, useSettings } from "./context/SettingsContext";
import { AppLayout } from "./layout";
import Login from "./layout/Login";
import { BrowserRouter, Route } from "react-router-dom";
import { Alert } from "@mui/material";
import { getLeafPaths } from "./features/cms/leafPaths";
import type { PageItem } from "./features/cms/leafPaths";
import { themes, ThemeName } from "./themes/themes";
import { settingsDataProvider } from "./providers/settingsDataProvider.ts";
import { queryClient } from "./api";

import PeopleIcon from "@mui/icons-material/People";
import GroupIcon from "@mui/icons-material/Group";
import LockIcon from "@mui/icons-material/Lock";
import PagesIcon from "@mui/icons-material/Pages";

// ── Data provider — created once at module level, never recreated ─────────────
const dataProvider = dataProviderFactory();

// ── Lazy page chunks ──────────────────────────────────────────────────────────
const UserList = lazy(() => import("./features/users/UserList"));
const UserCreate = lazy(() => import("./features/users/UserCreate"));
const UserEdit = lazy(() => import("./features/users/UserEdit"));

const RoleList = lazy(() => import("./features/roles/RoleList"));
const RoleCreate = lazy(() => import("./features/roles/RoleCreate"));
const RoleEdit = lazy(() => import("./features/roles/RoleEdit"));

const PermissionList = lazy(
  () => import("./features/permissions/PermissionList"),
);
const PermissionCreate = lazy(
  () => import("./features/permissions/PermissionCreate"),
);
const PermissionEdit = lazy(
  () => import("./features/permissions/PermissionEdit"),
);

const PageList = lazy(() => import("./features/cms/PageList"));
const PageCreate = lazy(() => import("./features/cms/PageCreate"));
const PageEdit = lazy(() => import("./features/cms/PageEdit"));

const Dashboard = lazy(() =>
  import("./features/dashboard/Dashboard").then((m) => ({
    default: m.Dashboard,
  })),
);
const AccessUploadPage = lazy(() =>
  import("./features/upload/AccessUploadPage").then((m) => ({
    default: m.AccessUploadPage,
  })),
);
const ProfilePage = lazy(() =>
  import("./features/profile/ProfilePage").then((m) => ({
    default: m.ProfilePage,
  })),
);

// ─────────────────────────────────────────────────────────────────────────────

export type { PageItem };

const AdminWithSettings = () => {
  const [themeName] = useStore<ThemeName>("themeName", "default");
  const singleTheme = themes.find((t) => t.name === themeName)?.single;
  const lightTheme = themes.find((t) => t.name === themeName)?.light;
  const darkTheme = themes.find((t) => t.name === themeName)?.dark;

  const { settings, error, reloadSettings } = useSettings();

  const leafPaths = useMemo(
    () =>
      Array.isArray(settings)
        ? getLeafPaths(settings as unknown as PageItem[])
        : [],
    [settings],
  );

  if (error) {
    return (
      <Alert
        severity="error"
        action={<button onClick={reloadSettings}>Retry</button>}
      >
        Failed to load settings: {error.message}
      </Alert>
    );
  }

  return (
    <Admin
      loginPage={Login}
      dashboard={Dashboard}
      dataProvider={dataProvider}
      authProvider={signProvider}
      layout={AppLayout}
      queryClient={queryClient}
      disableTelemetry
      requireAuth
      theme={singleTheme}
      lightTheme={lightTheme}
      darkTheme={darkTheme}
      defaultTheme="dark"
    >
      {/* User management */}
      <Resource
        name="users"
        list={UserList}
        create={UserCreate}
        edit={UserEdit}
        recordRepresentation="username"
        icon={PeopleIcon}
      />

      {/* Role management */}
      <Resource
        name="roles"
        list={RoleList}
        create={RoleCreate}
        edit={RoleEdit}
        recordRepresentation="name"
        icon={GroupIcon}
      />

      {/* Permission management */}
      <Resource
        name="permissions"
        list={PermissionList}
        create={PermissionCreate}
        edit={PermissionEdit}
        recordRepresentation="name"
        icon={LockIcon}
      />

      {/* Page / menu management */}
      <Resource
        name="pages"
        list={PageList}
        create={PageCreate}
        edit={PageEdit}
        recordRepresentation="name"
        icon={PagesIcon}
      />

      {/* Static custom routes */}
      <CustomRoutes>
        <Route
          path="/profile"
          element={
            <Suspense fallback={null}>
              <ProfilePage />
            </Suspense>
          }
        />
      </CustomRoutes>

      {/* Dynamic data routes generated from the settings tree */}
      <CustomRoutes>
        {leafPaths.map(({ path, item }) => (
          <Route
            key={item.id}
            path={path}
            element={
              <Suspense fallback={null}>
                <AccessUploadPage path={path} item={item} />
              </Suspense>
            }
          />
        ))}
      </CustomRoutes>
    </Admin>
  );
};

export const App = () => (
  <SettingsProvider dataProvider={settingsDataProvider}>
    <BrowserRouter>
      <AdminWithSettings />
    </BrowserRouter>
  </SettingsProvider>
);
