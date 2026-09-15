import React from "react";
import { Grid, Typography } from "@mui/material";
import { Loading, Title } from "react-admin";
import { useDashboardData } from "./hooks/useDashboardData";
import { RevenueChart } from "./parts/RevenueChart";
import { StatsCards } from "./parts/StatsCards";
import { UsersChart } from "./parts/UsersChart";

export const Dashboard: React.FC = () => {
  const { stats, revenueChart, usersChart, isLoading, error } =
    useDashboardData();

  if (isLoading) return <Loading />;
  if (error) return <Typography color="error">{error.message}</Typography>;

  return (
    <div>
      <Title title="Dashboard" />

      <Grid container spacing={2}>
        <Grid size={{ xl: 12 }}>
          <StatsCards stats={stats} />
        </Grid>

        <Grid size={{ xl: 6 }}>
          <RevenueChart data={revenueChart} />
        </Grid>

        <Grid size={{ xl: 6 }}>
          <UsersChart data={usersChart} />
        </Grid>
      </Grid>
    </div>
  );
};