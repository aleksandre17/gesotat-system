package org.base.api.service.artifact;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Registry of the dataset carriers present in this deployment. */
@Component
public class PackageDatasetCarriers {
    private final List<PackageDatasetCarrier> carriers;

    public PackageDatasetCarriers(List<PackageDatasetCarrier> carriers) {
        this.carriers = List.copyOf(carriers);
    }

    public Optional<PackageDatasetCarrier> forExtension(String extension) {
        return carriers.stream().filter(carrier -> carrier.carries(extension)).findFirst();
    }
}
