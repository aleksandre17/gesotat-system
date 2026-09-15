import { DataProvider, Identifier } from "react-admin";
import { httpClient } from "../api";
import { ENV } from "../config";
import { buildQuery, QueryOptions, FilterValue } from "../api";
import {
  normalizeAccessFields,
  serializeAccessFields,
} from "./utils/normalizeAccess";
import type { AnyRecord } from "./utils/mutator";

export type { AnyRecord };
const apiUrl = ENV.API_URL;

// ── Types ─────────────────────────────────────────────────────────────────────

// Casts AnyRecord → React Admin RecordType. `any` is intentional —
// DataProvider<RecordType> is generic; base RaRecord doesn't satisfy the subtype constraint.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const asRecord = (data: AnyRecord): any => data;

/**
 * Transform hooks applied per-resource.
 * `serialize` — called before POST / PUT   (form data → request body)
 * `deserialize` — called after GET one / list (response → form data)
 */
export interface ResourceTransform {
  serialize?: (data: AnyRecord) => AnyRecord;
  deserialize?: (data: AnyRecord) => AnyRecord;
}

export interface DataProviderConfig {
  /**
   * API base URL. Defaults to ENV.API_URL.
   */
  baseUrl?: string;

  /**
   * API version segment appended after baseUrl, e.g. "v1" → /v1/resource.
   * Set to "" to omit.
   */
  version?: string;

  /** Fallback pagination when React Admin omits it. */
  defaultPagination?: { page: number; perPage: number };

  /** Fallback sort when React Admin omits it. */
  defaultSort?: { field: string; order: string };

  /**
   * Per-resource transform hooks.
   * Key = resource name (e.g. "users", "roles").
   */
  resources?: Record<string, ResourceTransform>;

  /** Override query param names (default: json-server _page/_limit/_sort/_order). */
  queryOptions?: QueryOptions;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function normalizeList(json: unknown): { data: AnyRecord[]; total: number } {
  // Spring Page: { content: [...], totalElements: N }
  if (json && typeof json === "object" && "content" in (json as object)) {
    const page = json as { content: AnyRecord[]; totalElements?: number };
    return {
      data: page.content,
      total: Number(page.totalElements ?? page.content.length),
    };
  }
  // Wrapped: { data: [...], total: N }
  if (json && typeof json === "object" && "data" in (json as object)) {
    const wrapped = json as { data: AnyRecord[]; total?: number };
    return {
      data: wrapped.data,
      total: Number(wrapped.total ?? wrapped.data.length),
    };
  }
  // Plain array
  if (Array.isArray(json)) {
    return { data: json as AnyRecord[], total: json.length };
  }
  return { data: [], total: 0 };
}

// ── Factory ──────────────────────────────────────────────────────────────────

export function createDataProvider(
  config: DataProviderConfig = {},
): DataProvider {
  const {
    baseUrl = apiUrl,
    version = "v1",
    defaultPagination = { page: 1, perPage: 10 },
    defaultSort = { field: "id", order: "ASC" },
    resources = {},
    queryOptions = {},
  } = config;

  const base = version ? `${baseUrl}/${version}` : baseUrl;

  const ser = (resource: string, data: AnyRecord): AnyRecord => {
    const hook = resources[resource]?.serialize;
    if (hook) return hook(data);
    return serializeAccessFields(data);
  };

  const deser = (resource: string, data: AnyRecord): AnyRecord => {
    const hook = resources[resource]?.deserialize;
    if (hook) return hook(data);
    return normalizeAccessFields(data);
  };

  return {
    // ── READ ────────────────────────────────────────────────────────────────

    getList: async (resource, params) => {
      const pagination = params.pagination ?? defaultPagination;
      const sort = params.sort ?? defaultSort;
      const filter = (params.filter ?? {}) as Record<string, FilterValue>;

      const query = buildQuery(pagination, sort, filter, {}, queryOptions);
      const { json } = await httpClient(`${base}/${resource}?${query}`);
      const { data, total } = normalizeList(json);
      return {
        data: data.map((item) => asRecord(deser(resource, item))),
        total,
      };
    },

    getOne: async (resource, params) => {
      const { json } = await httpClient(`${base}/${resource}/${params.id}`);
      return { data: asRecord(deser(resource, json as AnyRecord)) };
    },

    getMany: async (resource, params) => {
      const query = new URLSearchParams({
        id: params.ids.join(","),
      }).toString();
      const { json } = await httpClient(`${base}/${resource}?${query}`);
      const { data } = normalizeList(json);
      return { data: data.map((item) => asRecord(deser(resource, item))) };
    },

    getManyReference: async (resource, params) => {
      const pagination = params.pagination ?? defaultPagination;
      const sort = params.sort ?? defaultSort;
      const filter = (params.filter ?? {}) as Record<string, FilterValue>;

      const query = buildQuery(
        pagination,
        sort,
        filter,
        {
          [params.target]: String(params.id),
        },
        queryOptions,
      );
      const { json } = await httpClient(`${base}/${resource}?${query}`);
      const { data, total } = normalizeList(json);
      return {
        data: data.map((item) => asRecord(deser(resource, item))),
        total,
      };
    },

    // ── WRITE ───────────────────────────────────────────────────────────────

    create: async (resource, params) => {
      const { json } = await httpClient(`${base}/${resource}`, {
        method: "POST",
        body: ser(resource, params.data as AnyRecord),
      });
      return { data: asRecord(deser(resource, json as AnyRecord)) };
    },

    update: async (resource, params) => {
      const { json } = await httpClient(`${base}/${resource}/${params.id}`, {
        method: "PUT",
        body: ser(resource, params.data as AnyRecord),
      });
      return { data: asRecord(deser(resource, json as AnyRecord)) };
    },

    updateMany: async (resource, params) => {
      const results = await Promise.all(
        params.ids.map(async (id) => {
          const { json } = await httpClient(`${base}/${resource}/${id}`, {
            method: "PUT",
            body: ser(resource, params.data as AnyRecord),
          });
          return (json as { id: Identifier }).id;
        }),
      );
      return { data: results };
    },

    // ── DELETE ──────────────────────────────────────────────────────────────

    delete: async (resource, params) => {
      await httpClient(`${base}/${resource}/${params.id}`, {
        method: "DELETE",
      });
      return { data: params.previousData! };
    },

    deleteMany: async (resource, params) => {
      await Promise.all(
        params.ids.map((id) =>
          httpClient(`${base}/${resource}/${id}`, { method: "DELETE" }),
        ),
      );
      return { data: params.ids };
    },
  };
}
