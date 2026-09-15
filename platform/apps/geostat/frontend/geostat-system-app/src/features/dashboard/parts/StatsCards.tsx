import React from "react";
import { Card, CardContent, Typography } from "@mui/material";
import Grid from "@mui/material/Grid";

import PeopleIcon from "@mui/icons-material/People";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import ShoppingCartIcon from "@mui/icons-material/ShoppingCart";
import PersonAddIcon from "@mui/icons-material/PersonAdd";

interface StatsCardsProps {
  stats: {
    totalUsers: number;
    activeUsers: number;
    newUsersToday: number;
    totalOrders: number;
    revenueToday: number;
  };
}

export const StatsCards: React.FC<StatsCardsProps> = ({ stats }) => {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(amount);
  };

  interface StatCard {
    title: string;
    value: string | number;
    icon: React.ReactNode;
    color: string;
  }

  const statCards: StatCard[] = [
    {
      title: "Total Users",
      value: stats.totalUsers,
      icon: <PeopleIcon />,
      color: "#1976d2",
    },
    {
      title: "New Users Today",
      value: stats.newUsersToday,
      icon: <PersonAddIcon />,
      color: "#2e7d32",
    },
    {
      title: "Total Orders",
      value: stats.totalOrders,
      icon: <ShoppingCartIcon />,
      color: "#ed6c02",
    },
    {
      title: "Revenue Today",
      value: formatCurrency(stats.revenueToday),
      icon: <TrendingUpIcon />,
      color: "#9c27b0",
    },
  ];

  return (
    <Grid container spacing={2}>
      {statCards.map((card, index) => (
        <Grid key={index} size={{ xs: 12, sm: 6, md: 3, xl: 3 }}>
          <Card>
            <CardContent>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  marginBottom: 8,
                }}
              >
                <div
                  style={{
                    backgroundColor: card.color,
                    padding: 8,
                    borderRadius: "50%",
                    marginRight: 8,
                    color: "white",
                  }}
                >
                  {card.icon}
                </div>
                <Typography color="textSecondary">{card.title}</Typography>
              </div>
              <Typography variant="h4" component="div">
                {card.value}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};