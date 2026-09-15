package org.base.core.entity.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPlatformDefaultsTest {

    @Test
    void profileStartsInLegacyModeUntilExplicitlyMigrated() {
        DataProfile profile = new DataProfile();

        assertEquals(DataMode.LEGACY, profile.getDataMode());
        assertEquals(DataKind.TABLE, profile.getDataKind());
        assertTrue(profile.isEnabled());
        assertEquals(1, profile.getVersion());
    }

    @Test
    void importedMetadataIsDraftByDefault() {
        ChartDefinition chart = new ChartDefinition();
        ImportTableMapping mapping = new ImportTableMapping();
        ImportJob job = new ImportJob();
        ImportJobItem item = new ImportJobItem();

        assertEquals(PublicationStatus.DRAFT, chart.getPublicationStatus());
        assertEquals(ImportMode.APPEND, mapping.getImportMode());
        assertEquals(ImportJobStatus.RECEIVED, job.getStatus());
        assertEquals(ImportJobItemStatus.MAPPED, item.getStatus());
    }
}
