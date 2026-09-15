import { Box } from "@mui/material";
import type { ReactNode } from "react";

export const TwoColGrid = ({ children }: { children: ReactNode }) => (
  <Box
    sx={{
      display: "grid",
      gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
      gap: 2,
      width: "100%",
    }}
  >
    {children}
  </Box>
);