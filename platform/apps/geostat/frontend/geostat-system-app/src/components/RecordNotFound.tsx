import { Box, Button, Typography } from "@mui/material";
import SearchOffIcon from "@mui/icons-material/SearchOff";
import { useNavigate, useLocation } from "react-router-dom";

interface RecordNotFoundProps {
  resource?: string;
  id?: string | number;
}

export const RecordNotFound = ({
  resource: resourceProp,
  id: idProp,
}: RecordNotFoundProps) => {
  const navigate = useNavigate();
  const { pathname } = useLocation();

  // Derive from URL if not passed explicitly
  const segments = pathname.split("/").filter(Boolean);
  const resource = resourceProp ?? segments[0];
  const id = idProp ?? segments[1];

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
        <SearchOffIcon sx={{ fontSize: 48, color: "text.disabled" }} />
      </Box>

      <Typography variant="h5" fontWeight={600} color="text.primary">
        ჩანაწერი ვერ მოიძებნა
      </Typography>

      <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 400 }}>
        {resource && id ? (
          <>
            <strong>{resource}/</strong>
            <strong>{id}</strong> — ჩანაწერი წაშლილია ან არ არსებობს.
          </>
        ) : (
          "მოთხოვნილი ჩანაწერი წაშლილია ან არ არსებობს."
        )}
      </Typography>

      <Box sx={{ display: "flex", gap: 1, mt: 1 }}>
        <Button variant="outlined" onClick={() => navigate(-1)}>
          უკან
        </Button>
        {resource && (
          <Button variant="contained" onClick={() => navigate(`/${resource}`)}>
            სიაში
          </Button>
        )}
      </Box>
    </Box>
  );
};