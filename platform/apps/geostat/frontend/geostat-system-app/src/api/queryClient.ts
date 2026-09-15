import { QueryClient, QueryCache } from "@tanstack/react-query";
import type {
  InvalidateQueryFilters,
  InvalidateOptions,
} from "@tanstack/react-query";
import { HttpError } from "./httpClient";

/**
 * Returns false for 4xx client errors — these won't fix themselves on retry.
 * Only retries network failures and 5xx server errors.
 */
const shouldRetry = (failureCount: number, error: unknown): boolean => {
  if (error instanceof HttpError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 3;
};

/**
 * Returns true if the query should refetch on window focus.
 * Prevents error-state queries from being re-fired every time the user
 * switches browser tabs — the main source of the "looping 404" symptom.
 */
const shouldRefetchOnWindowFocus = (query: { state: { status: string } }) =>
  query.state.status !== "error";

// ── QueryClient ───────────────────────────────────────────────────────────────

export const queryClient = new QueryClient({
  queryCache: new QueryCache(),
  defaultOptions: {
    queries: {
      retry: shouldRetry,
      refetchOnWindowFocus: shouldRefetchOnWindowFocus,
      staleTime: 0,
    },
    mutations: {
      retry: shouldRetry,
    },
  },
});

// ── Prevent the RA refresh() → invalidateQueries() → refetch → 404 → loop ───
//
// React Admin's useEditController calls refresh() after a getOne error.
// refresh() calls queryClient.invalidateQueries(), which triggers an immediate
// refetch for any currently-mounted (active) query — including the one that
// just 404'd.  Each refetch creates a NEW HttpError instance, the useEffect
// dependency changes, and the cycle repeats indefinitely.
//
// Fix: make invalidateQueries skip queries that are already in a 4xx error
// state.  They can only be "fixed" by the user taking action (the component
// will have navigated away via redirect anyway).

const _invalidateQueries = queryClient.invalidateQueries.bind(queryClient);

queryClient.invalidateQueries = ((
  filters?: InvalidateQueryFilters,
  options?: InvalidateOptions,
) => {
  const enhanced: InvalidateQueryFilters = {
    ...(filters ?? {}),
    predicate: (query) => {
      // Skip 4xx error queries — won't recover on their own
      const err = query.state.error;
      if (err instanceof HttpError && err.status >= 400 && err.status < 500) {
        return false;
      }
      // Delegate to caller's own predicate if provided
      return filters?.predicate ? filters.predicate(query) : true;
    },
  };
  return _invalidateQueries(enhanced, options);
}) as typeof queryClient.invalidateQueries;
