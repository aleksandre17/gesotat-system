import { KIDS_SITE_CONTRACT, pageIdFor } from "./siteContract";

const pagePath = (pageName, suffix = "") =>
  `/platform/contracts/${encodeURIComponent(KIDS_SITE_CONTRACT.code)}/pages/${pageIdFor(pageName)}${suffix}`;

export const KIDS_REQUEST_CASES = Object.freeze({
  discoverContract: () => ({
    method: "GET",
    path: `/platform/contracts/${encodeURIComponent(KIDS_SITE_CONTRACT.code)}/pages`,
    query: { revision: KIDS_SITE_CONTRACT.revision },
  }),
  pageCapabilities: (pageName) => ({
    method: "GET",
    path: pagePath(pageName, "/query-capabilities"),
    query: { revision: KIDS_SITE_CONTRACT.revision },
  }),
  readPage: (pageName, query = {}) => ({
    method: "GET",
    path: `/platform/pages/${pageIdFor(pageName)}/data`,
    query: { contractCode: KIDS_SITE_CONTRACT.code, revision: KIDS_SITE_CONTRACT.revision, ...query },
  }),
  queryPage: (pageName, body = {}) => ({
    method: "POST",
    path: pagePath(pageName, "/query"),
    body: { contractCode: KIDS_SITE_CONTRACT.code, revision: KIDS_SITE_CONTRACT.revision, ...body },
  }),
  exportPage: (pageName, body = {}) => ({
    method: "POST",
    path: "/platform/operations/exports",
    body: { contractCode: KIDS_SITE_CONTRACT.code, pageId: pageIdFor(pageName), revision: KIDS_SITE_CONTRACT.revision, ...body },
  }),
});
