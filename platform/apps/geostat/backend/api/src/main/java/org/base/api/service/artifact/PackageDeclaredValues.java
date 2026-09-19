package org.base.api.service.artifact;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Marks a match rule whose row values are declared by the package rather than carried in the row payload.
 * The engine asks for this capability and never for a concrete rule type; how the edges are read from the
 * package is the rule's own knowledge.
 */
public interface PackageDeclaredValues {

    /** One declared attachment: which row, which language slot, which position, which package path. */
    record DeclaredEdge(String rowKey, String language, long ordinal, String path) {}

    List<String> languages();

    /** Payload field the matcher reads for one language. */
    String valueField(String language);

    /** Every edge the package declares for this rule; a dangling or ambiguous declaration is an error, never a guess. */
    List<DeclaredEdge> declaredEdges(String relationCode, PackageTableReader tables, File datasetFile) throws IOException;
}
