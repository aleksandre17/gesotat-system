package org.base.api.service.artifact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The manifest a producer may ship inside a package as {@value #ENTRY_NAME} (artifact contract §2, §25):
 * what the package claims to contain and which row each file belongs to. It is a claim, never an
 * authority: admission recomputes everything and accepts the package only when the claim is exactly
 * what the platform derived itself. The producer tool and the API share this one type.
 */
public record PackageManifestDocument(String schema, String contractCode, int contractRevision, String datasetCode,
                                      String datasetEntry, List<FileClaim> files, List<EdgeClaim> edges) {
    public static final String SCHEMA = "geostat.artifact-package-manifest.v1";
    /** Reserved root entry; it describes the package and is not package content. */
    public static final String ENTRY_NAME = "manifest.json";

    public record FileClaim(String path, String sha256, long bytes, String mediaType) {}

    public record EdgeClaim(String rowKey, String relationCode, String language, int ordinal, String path) {}

    public PackageManifestDocument {
        if (!SCHEMA.equals(schema)) throw new IllegalArgumentException("Unsupported package manifest schema: " + schema);
        files = files == null ? List.of() : List.copyOf(files);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }

    /** Canonical document for a derived manifest and relation preview; order is independent of input order. */
    public static PackageManifestDocument describe(ArtifactPackageContractResolver.DatasetContract contract, String datasetEntry,
                                                   List<ArtifactManifest.Entry> entries, List<ArtifactMatcher.Plan> plans) {
        List<FileClaim> files = entries.stream().map(entry -> new FileClaim(entry.originalPath(), entry.sha256(), entry.byteSize(), entry.mediaType()))
                .sorted(Comparator.comparing(FileClaim::path)).toList();
        return new PackageManifestDocument(SCHEMA, contract.contractCode(), contract.revision(), contract.datasetCode(), datasetEntry, files, edges(plans));
    }

    static List<EdgeClaim> edges(List<ArtifactMatcher.Plan> plans) {
        List<EdgeClaim> edges = new ArrayList<>();
        for (ArtifactMatcher.Plan plan : plans)
            for (ArtifactMatcher.PlannedEdge edge : plan.edges())
                edges.add(new EdgeClaim(edge.externalKey(), plan.relationCode(), edge.language(), edge.ordinal(), edge.entry().originalPath()));
        edges.sort(Comparator.comparing(EdgeClaim::relationCode).thenComparing(EdgeClaim::rowKey).thenComparing(EdgeClaim::language).thenComparingInt(EdgeClaim::ordinal));
        return edges;
    }

    /**
     * Differences between this claim and what the platform derived; empty means the claim is exact.
     * Media types are derived by the platform and are not compared as identity.
     */
    public List<String> differencesFrom(PackageManifestDocument derived) {
        List<String> differences = new ArrayList<>();
        if (!contractCode.equals(derived.contractCode) || contractRevision != derived.contractRevision || !datasetCode.equals(derived.datasetCode))
            differences.add("contract identity differs from the admission request");
        if (!String.valueOf(datasetEntry).equals(derived.datasetEntry)) differences.add("dataset entry differs");
        compare("file", identities(files), identities(derived.files), differences);
        compare("edge", new LinkedHashSet<>(edges), new LinkedHashSet<>(derived.edges), differences);
        return differences;
    }

    private static Set<String> identities(List<FileClaim> files) {
        Set<String> identities = new LinkedHashSet<>();
        files.forEach(file -> identities.add(file.path() + "|" + file.sha256() + "|" + file.bytes()));
        return identities;
    }

    private static <T> void compare(String kind, Set<T> claimed, Set<T> derived, List<String> differences) {
        long missing = derived.stream().filter(item -> !claimed.contains(item)).count();
        long unexpected = claimed.stream().filter(item -> !derived.contains(item)).count();
        if (missing > 0) differences.add(missing + " " + kind + "(s) present in the package are not declared");
        if (unexpected > 0) differences.add(unexpected + " declared " + kind + "(s) are not in the package");
    }
}
