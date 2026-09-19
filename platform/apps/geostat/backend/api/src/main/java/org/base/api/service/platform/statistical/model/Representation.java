package org.base.api.service.platform.statistical.model;

import java.util.Set;

/** The form of a value, independent of its meaning. Sealed: a new form is a reviewed profile change. */
public sealed interface Representation {
    String logicalType();

    /** SDMX time-period lexical forms admitted by the profile (register Q19). */
    enum TimeFormat { YEAR, SEMESTER, QUARTER, MONTH, WEEK, DATE, RANGE }

    record Coded(Ref codelistRef) implements Representation {
        public String logicalType() { return "CODED"; }
    }

    record TimePeriod(Set<TimeFormat> formats) implements Representation {
        public TimePeriod {
            formats = Set.copyOf(formats);
            if (formats.isEmpty()) throw new IllegalArgumentException("at least one time format is required");
        }
        public String logicalType() { return "TIME_PERIOD"; }
    }

    /** Exact decimal unless {@code approximate} is declared (register Q23, Q34). */
    record Numeric(int precision, int scale, boolean approximate) implements Representation {
        public String logicalType() { return approximate ? "APPROXIMATE_NUMERIC" : "DECIMAL"; }
    }

    record IntegerRange(long min, long max) implements Representation {
        public String logicalType() { return "INTEGER"; }
    }

    record BoundedText(int maxLength) implements Representation {
        public String logicalType() { return "TEXT"; }
    }
}
