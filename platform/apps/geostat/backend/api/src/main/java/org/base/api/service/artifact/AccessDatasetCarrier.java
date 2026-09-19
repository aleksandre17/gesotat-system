package org.base.api.service.artifact;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Microsoft Access as a dataset carrier. */
@Component
public class AccessDatasetCarrier implements PackageDatasetCarrier {
    private static final Set<String> EXTENSIONS = Set.of("accdb", "mdb");

    private final ArtifactAccessPackageValidator validator;

    public AccessDatasetCarrier(ArtifactAccessPackageValidator validator) {
        this.validator = validator;
    }

    @Override
    public boolean carries(String extension) {
        return EXTENSIONS.contains(extension);
    }

    @Override
    public void validate(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        validator.validate(file, contract);
    }

    @Override
    public List<ArtifactMatcher.SourceRow> rows(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        return ArtifactAccessRowReader.read(file, contract);
    }
}
