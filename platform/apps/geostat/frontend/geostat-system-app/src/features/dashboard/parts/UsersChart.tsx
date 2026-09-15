import React from "react";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";
import { Card, CardContent, Typography } from "@mui/material";
import type { ChartData } from "../hooks/useDashboardData";

interface UsersChartProps {
  data: ChartData[];
}

export const UsersChart: React.FC<UsersChartProps> = ({ data }) => {
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
          User Growth
        </Typography>
        <div style={{ width: "100%", height: 300 }}>
          <ResponsiveContainer>
            <LineChart
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
              <YAxis />
              <Tooltip
                formatter={(value: number) => [
                  `${value} users`,
                  "Active Users",
                ]}
                labelFormatter={formatDate}
              />
              <Legend />
              <Line
                type="monotone"
                dataKey="value"
                name="Active Users"
                stroke="#2196f3"
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
};