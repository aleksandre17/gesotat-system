/**
 * File upload configuration.
 *
 * Used by useUpload hook for Access database uploads.
 */
export const UPLOAD_CONFIG = {
  /** Maximum time to wait for an upload to complete (ms). 5 minutes. */
  timeoutMs: 300_000,
} as const;
