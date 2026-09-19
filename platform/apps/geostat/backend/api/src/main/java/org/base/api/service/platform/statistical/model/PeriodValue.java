package org.base.api.service.platform.statistical.model;

import org.base.api.service.platform.statistical.model.Representation.TimeFormat;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.IsoFields;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One SDMX time period resolved to a closed date interval. The lexical form is kept verbatim for lineage;
 * {@code start}/{@code endInclusive} are the deterministic storage projection (register Q19).
 */
public record PeriodValue(TimeFormat format, String lexical, LocalDate start, LocalDate endInclusive) {
    private static final Pattern YEAR = Pattern.compile("(\\d{4})");
    private static final Pattern SEMESTER = Pattern.compile("(\\d{4})-S([12])");
    private static final Pattern QUARTER = Pattern.compile("(\\d{4})-Q([1-4])");
    private static final Pattern MONTH = Pattern.compile("(\\d{4})-M?(0[1-9]|1[0-2])");
    private static final Pattern WEEK = Pattern.compile("(\\d{4})-W(0[1-9]|[1-4]\\d|5[0-3])");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern RANGE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})/(P[0-9YMWD]+)");

    /** Empty when the text is not a period in one of the admitted formats; never guesses. */
    public static Optional<PeriodValue> parse(String lexical, Set<TimeFormat> admitted) {
        if (lexical == null) return Optional.empty();
        String text = lexical.trim();
        try {
            for (TimeFormat format : TimeFormat.values()) {
                if (!admitted.contains(format)) continue;
                Optional<PeriodValue> value = tryFormat(format, text);
                if (value.isPresent()) return value;
            }
        } catch (DateTimeException | ArithmeticException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<PeriodValue> tryFormat(TimeFormat format, String text) {
        Matcher m;
        switch (format) {
            case YEAR -> {
                if ((m = YEAR.matcher(text)).matches()) return months(format, text, year(m), 1, 12);
            }
            case SEMESTER -> {
                if ((m = SEMESTER.matcher(text)).matches()) return months(format, text, year(m), (index(m) - 1) * 6 + 1, 6);
            }
            case QUARTER -> {
                if ((m = QUARTER.matcher(text)).matches()) return months(format, text, year(m), (index(m) - 1) * 3 + 1, 3);
            }
            case MONTH -> {
                if ((m = MONTH.matcher(text)).matches()) return months(format, text, year(m), index(m), 1);
            }
            case WEEK -> {
                if ((m = WEEK.matcher(text)).matches()) {
                    LocalDate monday = LocalDate.of(year(m), 1, 4).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, index(m)).with(DayOfWeek.MONDAY);
                    if (monday.get(IsoFields.WEEK_BASED_YEAR) != year(m)) return Optional.empty(); // week 53 of a 52-week year
                    return Optional.of(new PeriodValue(format, text, monday, monday.plusDays(6)));
                }
            }
            case DATE -> {
                if (DATE.matcher(text).matches()) {
                    LocalDate day = LocalDate.parse(text);
                    return Optional.of(new PeriodValue(format, text, day, day));
                }
            }
            case RANGE -> {
                if ((m = RANGE.matcher(text)).matches()) {
                    LocalDate start = LocalDate.parse(m.group(1));
                    Period duration = Period.parse(m.group(2));
                    if (duration.isZero() || duration.isNegative()) return Optional.empty();
                    return Optional.of(new PeriodValue(format, text, start, start.plus(duration).minusDays(1)));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<PeriodValue> months(TimeFormat format, String text, int year, int firstMonth, int length) {
        LocalDate start = LocalDate.of(year, firstMonth, 1);
        return Optional.of(new PeriodValue(format, text, start, start.plusMonths(length).minusDays(1)));
    }

    private static int year(Matcher m) { return Integer.parseInt(m.group(1)); }

    private static int index(Matcher m) { return Integer.parseInt(m.group(2)); }
}
