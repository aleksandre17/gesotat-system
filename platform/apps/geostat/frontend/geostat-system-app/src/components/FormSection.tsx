import { Box, Divider, Stack, Typography } from "@mui/material";
import { ReactNode } from "react";

interface FormSectionProps {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  children: ReactNode;
}

export const FormSection = ({
  title,
  subtitle,
  icon,
  children,
}: FormSectionProps) => (
  <Box sx={{ width: "100%", mb: 1 }}>
    <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 0.5 }}>
      {icon && (
        <Box
          sx={{
            display: "flex",
            p: 0.75,
            borderRadius: 1.5,
            bgcolor: "primary.main",
            color: "primary.contrastText",
            "& svg": { fontSize: 18 },
          }}
        >
          {icon}
        </Box>
      )}
      <Box>
        <Typography
          variant="subtitle2"
          fontWeight={700}
          letterSpacing={0.4}
          textTransform="uppercase"
          color="text.secondary"
        >
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="caption" color="text.disabled">
            {subtitle}
          </Typography>
        )}
      </Box>
    </Stack>
    <Divider sx={{ mb: 3, mt: 1 }} />
    {children}
  </Box>
);