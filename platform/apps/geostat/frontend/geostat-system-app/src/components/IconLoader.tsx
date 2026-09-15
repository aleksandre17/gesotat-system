import { lazy, Suspense, useMemo } from "react";
import { CircularProgress } from "@mui/material";

const loadIcon = (iconName?: string) => {
  return lazy(() =>
    import(`@mui-icons/${iconName}.js`).catch(
      () => import("@mui/icons-material/Dashboard"),
    ),
  );
};

export const IconLoader = ({ iconName }: { iconName?: string }) => {
  const Icon = useMemo(() => loadIcon(iconName), [iconName]);
  return (
    <Suspense fallback={<CircularProgress size={16} />}>
      <Icon />
    </Suspense>
  );
};