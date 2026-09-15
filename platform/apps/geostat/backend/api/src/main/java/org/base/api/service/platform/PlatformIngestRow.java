package org.base.api.service.platform;

/** A source row remains untouched in staging; semantic transformation happens only after review. */
public record PlatformIngestRow(long sourceRowNumber, String sourceKey, String payloadJson) {
}
