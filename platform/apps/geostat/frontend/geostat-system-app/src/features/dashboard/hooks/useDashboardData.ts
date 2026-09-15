import { useQueryClient } from "@tanstack/react-query";
import {
  useDashboardStats,
  useRevenueChart,
  useUsersChart,
  QUERY_KEYS,
} from "../../../api/hooks";
import { DASHBOARD_REVENUE_QUERY } from "../../../config";

export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  newUsersToday: number;
  totalOrders: number;
  revenueToday: number;
}

export interface ChartData {
  date: string;
  value: number;
}

export const useDashboardData = () => {
  const queryClient = useQueryClient();

  const statsQuery = useDashboardStats<DashboardStats>();
  const revenueQuery = useRevenueChart<ChartData[]>(DASHBOARD_REVENUE_QUERY);
  const usersQuery = useUsersChart<ChartData[]>();

  const isLoading =
    statsQuery.isPending || revenueQuery.isPending || usersQuery.isPending;

  const error = statsQuery.error ?? revenueQuery.error ?? usersQuery.error;

  const refresh = () => {
    void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.dashboard.stats });
    void queryClient.invalidateQueries({
      queryKey: QUERY_KEYS.dashboard.revenueChart(DASHBOARD_REVENUE_QUERY),
    });
    void queryClient.invalidateQueries({
      queryKey: QUERY_KEYS.dashboard.usersChart,
    });
  };

  return {
    stats: statsQuery.data ?? null,
    revenueChart: revenueQuery.data ?? [],
    usersChart: usersQuery.data ?? [],
    isLoading,
    error,
    refresh,
  };
};
