package org.base.api.service.platform;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Contract-declared, provider-neutral stable ordering metadata for keyset pagination. */
public record StableSortSpec(List<Column> columns, String requiredIndexCode) {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    public enum Direction { ASC, DESC }
    public enum NullOrder { FIRST, LAST }
    public record Column(String name, Direction direction, NullOrder nullOrder) {
        public Column {
            if (name == null || !IDENTIFIER.matcher(name).matches()) throw new IllegalArgumentException("Unapproved sort column");
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(nullOrder, "nullOrder");
        }
    }

    public StableSortSpec {
        if (columns == null || columns.isEmpty() || columns.size() > 32) throw new IllegalArgumentException("Stable sort tuple is invalid");
        columns = List.copyOf(columns);
        if (requiredIndexCode == null || !IDENTIFIER.matcher(requiredIndexCode).matches()) throw new IllegalArgumentException("Stable index code is invalid");
        var names = columns.stream().map(Column::name).toList();
        if (names.size() != Set.copyOf(names).size()) throw new IllegalArgumentException("Stable sort columns must be unique");
    }
}
