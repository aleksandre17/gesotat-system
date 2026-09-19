package org.base.api.service.artifact;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Capability of a dataset carrier to read the rows of any table of the package, in source order, with
 * values as text. Rules that declare their edges in package tables need it; a carrier without tables does
 * not implement it and such a rule is then refused for that format.
 */
public interface PackageTableReader {

    /** @return one map per row holding exactly the requested fields (absent or null values are {@code null}) */
    List<Map<String, String>> rows(File file, String table, List<String> fields) throws IOException;
}
