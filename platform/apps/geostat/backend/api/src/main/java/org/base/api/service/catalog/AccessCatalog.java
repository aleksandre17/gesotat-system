package org.base.api.service.catalog;

import java.util.List;

/** Full discovery result used before mapping or importing a submitted Access package. */
public record AccessCatalog(boolean managedPackage, List<String> missingRequiredMetadataTables,
                            List<AccessTableCatalog> tables) {
}
