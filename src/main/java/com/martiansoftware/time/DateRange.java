package com.martiansoftware.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wraps around a range of LocalDates and provides a simple text-based way to
 * construct them.
 * 
 * Specific dates may be specified as yyyy-MM-dd (e.g., "2015-10-10");
 * Specific date ranges may be specified as yyyy-MM-dd-yyyy-MM-dd (e.g., "2016-01-01-2016-12-31")
 * Modifiers may be added to dates by appending "+", "-", or "+-" and one or more adjustments
 *   consisting of a number and "y", "m", "w", or "d" specifying years, months, weeks, or days, respectively.
 *   (e.g., "2015-10-10+-3w2d")
 * Modifiers with no preceding date are applied to the current date.
 * @author mlamb
 */
public class DateRange {
    private static final Map<Pattern, Function<Matcher, DateRange>> _parsers = new java.util.LinkedHashMap<>();
    private static final Pattern MODIFIER_SPLITTER;
    
    static {
        String DATE_RE = "(?<date>[0-9]{4}-[0-9]{2}-[0-9]{2})";    
        String PLUSMINUS_RE = "(?<pm>[+-]+)";
        String MODIFIER_RE = "((?<magnitude>[0-9]+)(?<units>[ymwd]))";

        Pattern DATE = Pattern.compile(String.format("^%s$", DATE_RE));
        Pattern DATERANGE = Pattern.compile(String.format("^%s-%s", DATE_RE.replace("date", "date1"), DATE_RE.replace("date", "date2")));
        Pattern MODIFIERS = Pattern.compile(String.format("^%s?(?<modifiers>%s+)$", PLUSMINUS_RE, MODIFIER_RE)); // (1)=plusminus, (2)=all modifiers
        Pattern DATE_AND_MODIFIERS = Pattern.compile(String.format("^%s%s(?<modifiers>%s+)", DATE_RE, PLUSMINUS_RE, MODIFIER_RE));// (1)=date, (2)=plusminus, (3)=all modifiers
        MODIFIER_SPLITTER = Pattern.compile(MODIFIER_RE);
        
        _parsers.put(DATE, DateRange::forDate);
        _parsers.put(DATERANGE, DateRange::forDateRange);
        _parsers.put(MODIFIERS, DateRange::forModifiers);
        _parsers.put(DATE_AND_MODIFIERS, DateRange::forDateAndModifiers);
    }

    private final LocalDate _from, _to;

    private DateRange(LocalDate from, LocalDate to) {
        if (from.compareTo(to) <= 0) {
            _from = from; _to = to;
        } else {
            _from = to; _to = from;
        }
    }
    private DateRange(LocalDate d) { this(d, d); }
    
    public LocalDate from() { return _from; }
    public LocalDate to() { return _to; }
    
    public static String[] regexes() {
        return _parsers.keySet().stream().map((p) -> p.pattern()).collect(Collectors.toCollection(java.util.ArrayList::new)).toArray(new String[0]);
    }
    
    public static DateRange forQuery(String q) {
        return _parsers.entrySet().stream()
            .flatMap((e) -> { 
                Matcher m = e.getKey().matcher(q);
                if (m.matches()) return Stream.of(e.getValue().apply(m));
                return Stream.empty();
            })
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid date query: '" + q + "'"));
    }
    
    private static DateRange forDate(Matcher m) { return new DateRange(LocalDate.parse(m.group("date"))); }
    private static DateRange forDateRange(Matcher m) { return new DateRange(LocalDate.parse(m.group("date1")), LocalDate.parse(m.group("date2"))); }
    private static DateRange forModifiers(Matcher m) { return forLocalDateAndModifiers(LocalDate.now(), m.group("pm"), m.group("modifiers")); }
    private static DateRange forDateAndModifiers(Matcher m) {return forLocalDateAndModifiers(LocalDate.parse(m.group(1)), m.group(2), m.group(3)); }

    private static DateRange forLocalDateAndModifiers(LocalDate d, String pm, String modifiers) {
        LocalDate from = d, to = d;        
        Matcher m = MODIFIER_SPLITTER.matcher(modifiers);
        long years = 0, months = 0, weeks = 0, days = 0;
        while (m.find()) {
            long adj = Long.parseLong(m.group("magnitude"));
            switch(m.group("units").toLowerCase()) {
                case "y": years += adj; break;
                case "m": months += adj; break;
                case "w": weeks += adj; break;
                case "d": days += adj; break;
                default: throw new RuntimeException("Unrecognized date modifier unit: " + m.group("units"));
            }
        }
        if (pm == null) pm = "-";
        if (pm.contains("-")) from = from.minusYears(years).minusMonths(months).minusWeeks(weeks).minusDays(days);
        if (pm.contains("+")) to = to.plusYears(years).plusMonths(months).plusWeeks(weeks).plusDays(days);
        return new DateRange(from, to);
    }
    
    @Override public String toString() {
        return String.format("[%s, %s]", _from, _to);
    }
    
    @Override public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this._from);
        hash = 59 * hash + Objects.hashCode(this._to);
        return hash;
    }

    @Override public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DateRange other = (DateRange) obj;
        if (!Objects.equals(this._from, other._from)) {
            return false;
        }
        if (!Objects.equals(this._to, other._to)) {
            return false;
        }
        return true;
    }
    
}
