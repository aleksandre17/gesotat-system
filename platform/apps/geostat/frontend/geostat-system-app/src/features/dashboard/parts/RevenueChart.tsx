import React from "react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";
import { Card, CardContent, Typography } from "@mui/material";
import type { ChartData } from "../hooks/useDashboardData";

interface RevenueChartProps {
  data: ChartData[];
}

export const RevenueChart: React.FC<RevenueChartProps> = ({ data }) => {
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
    });
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Revenue Overview
        </Typography>
        <div style={{ width: "100%", height: 300 }}>
          <ResponsiveContainer>
            <AreaChart
              data={data}
              margin={{
                top: 10,
                right: 30,
                left: 0,
                bottom: 0,
              }}
            >
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tickFormatter={formatDate} />
              <YAxis tickFormatter={formatCurrency} />
              <Tooltip
                formatter={(value: number) => [
                  formatCurrency(value),
                  "Revenue",
                ]}
                labelFormatter={formatDate}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#8884d8"
                fill="#8884d8"
                fillOpacity={0.3}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
};