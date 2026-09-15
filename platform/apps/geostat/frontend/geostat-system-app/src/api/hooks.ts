/**
 * Custom TanStack Query hooks for non-CRUD API operations.
 *
 * Use these alongside React Admin hooks:
 *   - Standard CRUD  → useGetList / useGetOne / useCreate / useUpdate / useDelete
 *   - Custom calls   → hooks from this file
 */

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useNotify } from "react-admin";
import { httpClient } from "./httpClient";
import { ENV, DASHBOARD_CACHE } from "../config";

const apiUrl = ENV.API_URL;

// ── Query keys ───────────────────────────────────────────────────────────────

export const QUERY_KEYS = {
  // matches React Admin's internal key so invalidation hits the SettingsLoader cache
  settings: ["pages", "getList"] as const,
  dashboard: {
    stats: ["dashboard", "stats"] as const,
    revenueChart: (filter?: Record<string, unknown>) =>
      ["dashboard", "revenueChart", filter] as const,
    usersChart: ["dashboard", "usersChart"] as const,
  },
} as const;

// ── Dashboard ────────────────────────────────────────────────────────────────

export const useDashboardStats = <T = unknown>() =>
  useQuery<T>({
    queryKey: QUERY_KEYS.dashboard.stats,
    queryFn: () =>
      httpClient(`${apiUrl}/v1/test/dashboardStats`).then((r) => r.json as T),
    ...DASHBOARD_CACHE,
  });

export const useRevenueChart = <T = unknown>(
  filter?: Record<string, unknown>,
) =>
  useQuery<T>({
    queryKey: QUERY_KEYS.dashboard.revenueChart(filter),
    queryFn: () => {
      const params = filter
        ? "?" + new URLSearchParams(filter as Record<string, string>).toString()
        : "";
      return httpClient(`${apiUrl}/v1/test/revenueChart${params}`).then(
        (r) => r.json as T,
      );
    },
    ...DASHBOARD_CACHE,
  });

export const useUsersChart = <T = unknown>() =>
  useQuery<T>({
    queryKey: QUERY_KEYS.dashboard.usersChart,
    queryFn: () =>
      httpClient(`${apiUrl}/v1/test/usersChart`).then((r) => r.json as T),
    ...DASHBOARD_CACHE,
  });

// ── Settings mutations ────────────────────────────────────────────────────────

export const useApplySettings = <T = unknown>() => {
  const queryClient = useQueryClient();
  const notify = useNotify();

  return useMutation<T, Error, unknown>({
    mutationFn: (data) =>
      httpClient(`${apiUrl}/settings/apply`, {
        method: "POST",
        body: JSON.stringify(data),
      }).then((r) => r.json as T),
    onSuccess: () => {
      // Invalidate sidebar so it reflects saved changes immediately
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.settings });
      notify("Settings saved", { type: "success" });
    },
    onError: () => notify("Failed to save settings", { type: "error" }),
  });
};

export const useResetSettings = () => {
  const queryClient = useQueryClient();
  const notify = useNotify();

  return useMutation({
    mutationFn: () =>
      httpClient(`${apiUrl}/settings/reset`, { method: "POST" }).then(
        (r) => r.json,
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.settings });
      notify("Settings reset", { type: "success" });
    },
    onError: () => notify("Failed to reset settings", { type: "error" }),
  });
};
