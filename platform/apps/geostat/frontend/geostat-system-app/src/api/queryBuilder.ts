/**
 * Universal query builder for REST APIs.
 *
 * Supports every filter value shape React Admin may send:
 *   "john"                  → field=john           (exact)
 *   true / false            → field=true           (boolean)
 *   [1, 2, 3]               → field=1,2,3          (array → csv)
 *   { gte:1, lte:10 }       → field_gte=1&field_lte=10  (range)
 *   { like:"jo%" }          → field_like=jo%        (like)
 *   { ne:"foo" }            → field_ne=foo          (not-equal)
 *   { contains:"foo" }      → field_contains=foo    (contains)
 *   { startsWith:"foo" }    → field_startsWith=foo  (starts-with)
 *   null / undefined        → skipped
 */

export type FilterValue =
  | string
  | number
  | boolean
  | null
  | undefined
  | FilterValue[]
  | RangeFilter;

export interface RangeFilter {
  gte?: string | number;
  lte?: string | number;
  gt?: string | number;
  lt?: string | number;
  like?: string;
  ne?: string | number;
  contains?: string;
  startsWith?: string;
}

export interface QueryOptions {
  /** Pagination param names (json-server style by default) */
  pageParam?: string;
  limitParam?: string;
  sortParam?: string;
  orderParam?: string;
}

const DEFAULT_OPTS: Required<QueryOptions> = {
  pageParam: "_page",
  limitParam: "_limit",
  sortParam: "_sort",
  orderParam: "_order",
};

function appendFilter(
  params: URLSearchParams,
  key: string,
  value: FilterValue,
) {
  if (value == null) return;

  if (Array.isArray(value)) {
    if (value.length === 0) return;
    params.set(key, value.join(","));
    return;
  }

  if (typeof value === "object") {
    const r = value as RangeFilter;
    if (r.gte != null) params.set(`${key}_gte`, String(r.gte));
    if (r.lte != null) params.set(`${key}_lte`, String(r.lte));
    if (r.gt != null) params.set(`${key}_gt`, String(r.gt));
    if (r.lt != null) params.set(`${key}_lt`, String(r.lt));
    if (r.like != null) params.set(`${key}_like`, r.like);
    if (r.ne != null) params.set(`${key}_ne`, String(r.ne));
    if (r.contains != null) params.set(`${key}_contains`, r.contains);
    if (r.startsWith != null) params.set(`${key}_startsWith`, r.startsWith);
    return;
  }

  params.set(key, String(value));
}

export function buildQuery(
  pagination: { page: number; perPage: number },
  sort: { field: string; order: string },
  filter: Record<string, FilterValue> = {},
  extra: Record<string, string> = {},
  opts: QueryOptions = {},
): string {
  const { pageParam, limitParam, sortParam, orderParam } = {
    ...DEFAULT_OPTS,
    ...opts,
  };

  const params = new URLSearchParams({
    [pageParam]: String(pagination.page),
    [limitParam]: String(pagination.perPage),
    [sortParam]: sort.field,
    [orderParam]: sort.order,
    ...extra,
  });

  for (const [key, value] of Object.entries(filter)) {
    appendFilter(params, key, value);
  }

  return params.toString();
}
