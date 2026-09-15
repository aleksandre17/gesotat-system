package org.base.api.service.catalog;

import java.util.List;

/** Read-only description of one Access table. No source rows are retained here. */
public record AccessTableCatalog(String name, long rowCount, List<AccessColumnCatalog> columns, boolean systemTable) {
}
