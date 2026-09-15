import { createDataProvider } from "./createDataProvider";

/**
 * Data provider for the dashboard resource (standard CRUD only).
 * Custom dashboard API calls (stats, charts) are in apiHooks.ts.
 */
export const dashboardDataProvider = createDataProvider({
  version: "v1",
});