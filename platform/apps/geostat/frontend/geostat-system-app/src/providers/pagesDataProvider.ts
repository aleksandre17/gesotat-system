import { DataProvider } from "react-admin";
import { httpClient } from "../api";
import { ENV } from "../config";
import { createDataProvider } from "./createDataProvider";
import { createTransform } from "./utils/createTransform";

const apiUrl = ENV.API_URL;

// ── Transforms ───────────────────────────────────────────────────────────────
// Access fields (role, userId, …) are handled globally in createDataProvider.
// Only the page-specific hierarchy field needs mapping here.

const pageTransform = createTransform({ parent: "parentId" });

// ── Custom method types ───────────────────────────────────────────────────────

interface PageReorder {
  pageId: number;
  parentId: number | null;
  orderIndex: number;
}

interface ReorderParams {
  reorders: PageReorder[];
}

export interface PagesDataProvider extends DataProvider {
  reorderPage: (
    resource: string,
    params: ReorderParams,
  ) => Promise<{ data: number[] }>;
  reorderPages: (
    resource: string,
    params: ReorderParams,
  ) => Promise<{ data: number[] }>;
}

// ── Provider ─────────────────────────────────────────────────────────────────

const base = createDataProvider({
  version: "v1",
  defaultPagination: { page: 1, perPage: 1000 },
  defaultSort: { field: "orderIndex", order: "ASC" },
  resources: {
    pages: pageTransform,
  },
});

export const pagesDataProvider: PagesDataProvider = {
  ...base,

  // Sequential reorder (one request per item — legacy)
  reorderPage: async (_resource, params) => {
    const results = await Promise.all(
      params.reorders.map(async (reorder) => {
        await httpClient(`${apiUrl}/v1/pages/reorder`, {
          method: "POST",
          body: {
            pageId: reorder.pageId,
            parentId: reorder.parentId,
            orderIndex: reorder.orderIndex,
          },
        });
        return reorder.pageId;
      }),
    );
    return { data: results };
  },

  // Batch reorder (single request for all items)
  reorderPages: async (_resource, params) => {
    await httpClient(`${apiUrl}/v1/pages/reorders`, {
      method: "POST",
      body: params.reorders,
    });
    return { data: params.reorders.map((r) => r.pageId) };
  },
};
