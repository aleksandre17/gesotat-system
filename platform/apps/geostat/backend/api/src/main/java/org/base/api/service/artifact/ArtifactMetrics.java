package org.base.api.service.artifact;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Low-cardinality counters for the artifact line; tags never carry keys, paths or URLs. */
@Component
public class ArtifactMetrics {
    private final ObjectProvider<MeterRegistry> registry;

    public ArtifactMetrics(ObjectProvider<MeterRegistry> registry) {
        this.registry = registry;
    }

    public void verification(String status, int count) {
        MeterRegistry meters = registry.getIfAvailable();
        if (meters != null && count > 0) meters.counter("geostat.artifact.object.verification", "status", status).increment(count);
    }

    public void binding(String status) {
        count("geostat.artifact.binding", "status", status);
    }

    public void reconciliation(String result) {
        count("geostat.artifact.reconciliation", "result", result);
    }

    public void malwareScan(String result) {
        count("geostat.artifact.malware_scan", "result", result);
    }

    public void integrityAudit(String result) {
        count("geostat.artifact.integrity_audit", "result", result);
    }

    public void download(String relationCode, String outcome) {
        MeterRegistry meters = registry.getIfAvailable();
        if (meters != null) meters.counter("geostat.artifact.download", "relation", relationCode, "outcome", outcome).increment();
    }

    private void count(String name, String tag, String value) {
        MeterRegistry meters = registry.getIfAvailable();
        if (meters != null) meters.counter(name, tag, value).increment();
    }
}
