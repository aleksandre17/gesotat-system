package org.base.api.service.artifact;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A file format that carries a package dataset (its business rows). The package engine knows carriers
 * only through this port; supporting another format is another bean, not a change to the engine.
 */
public interface PackageDatasetCarrier {

    /** Whether a package entry with this lowercase file extension is a dataset of this format. */
    boolean carries(String extension);

    /** Fails when the file does not have the structure the approved contract declares. */
    void validate(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException;

    List<ArtifactMatcher.SourceRow> rows(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException;
}
