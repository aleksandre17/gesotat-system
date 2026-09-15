import { useState, useEffect, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useLocation } from "react-router-dom";
import { HttpError } from "../../api";
import { RecordNotFound } from "../../components/RecordNotFound";
import { AccessDenied } from "./AccessDenied";

interface HttpErrorBoundaryProps {
  children: ReactNode;
}

/**
 * HTTP error middleware — wraps all content routes in Layout.
 *
 * Subscribes to the TanStack Query cache and intercepts 403 / 404 responses
 * from standard CRUD queries (getOne, getList). Renders the appropriate
 * guard component in place of the current page — keeping sidebar and header.
 *
 * Defense layers this sits in:
 *   Layer 1 — Route (signProvider.canAccess)       ← blocks before render
 *   Layer 2 — Menu visibility (MenuAccessContext)  ← hides links
 *   Layer 3 — Route render (useCanAccessPage)      ← blocks direct navigation
 *   Layer 4 — THIS: catches 403/404 from API calls ← last-resort fallback
 *
 * Clears on every route change so navigating away always shows a fresh page.
 */
export const HttpErrorBoundary = ({ children }: HttpErrorBoundaryProps) => {
  const queryClient = useQueryClient();
  const { pathname } = useLocation();
  const [activeError, setActiveError] = useState<HttpError | null>(null);

  // Clear on every navigation
  useEffect(() => {
    setActiveError(null);
  }, [pathname]);

  // Subscribe to QueryCache — fires before component observers are notified
  useEffect(() => {
    return queryClient.getQueryCache().subscribe((event) => {
      const err = event.query.state.error;
      if (!(err instanceof HttpError)) return;
      if (err.status !== 404 && err.status !== 403) return;

      // Only intercept standard React Admin CRUD operations
      const key = event.query.queryKey;
      const isCrudOp =
        Array.isArray(key) &&
        (key.includes("getOne") || key.includes("getList"));

      if (isCrudOp) setActiveError(err);
    });
  }, [queryClient]);

  if (activeError?.status === 404) return <RecordNotFound />;
  if (activeError?.status === 403) return <AccessDenied />;

  return <>{children}</>;
};
