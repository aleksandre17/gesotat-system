import { Box, Button, Typography } from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import { useNavigate } from "react-router-dom";

interface AccessDeniedProps {
  resourceName?: string;
}

/**
 * Rendered when the user lacks permission to view a resource (403).
 *
 * Used by:
 *  - HttpErrorBoundary — catches 403 from API calls
 *  - AccessUploadPage  — blocks direct URL navigation to restricted routes
 */
export const AccessDenied = ({ resourceName }: AccessDeniedProps) => {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        height: "60vh",
        gap: 2,
        textAlign: "center",
        px: 3,
      }}
    >
      <Box
        sx={{
          display: "flex",
          p: 2.5,
          borderRadius: "50%",
          bgcolor: "action.hover",
          mb: 1,
        }}
      >
        <LockOutlinedIcon sx={{ fontSize: 48, color: "text.disabled" }} />
      </Box>

      <Typography variant="h5" fontWeight={600} color="text.primary">
        წვდომა შეზღუდულია
      </Typography>

      {resourceName && (
        <Typography
          variant="body1"
          color="text.secondary"
          sx={{ maxWidth: 400 }}
        >
          თქვენ არ გაქვთ უფლება გახსნათ{" "}
          <strong>&quot;{resourceName}&quot;</strong> გვერდი.
        </Typography>
      )}

      <Typography variant="body2" color="text.disabled" sx={{ maxWidth: 360 }}>
        თუ ფიქრობთ, რომ ეს შეცდომაა, დაუკავშირდით ადმინისტრატორს.
      </Typography>

      <Button variant="outlined" onClick={() => navigate(-1)} sx={{ mt: 1 }}>
        უკან დაბრუნება
      </Button>
    </Box>
  );
};
