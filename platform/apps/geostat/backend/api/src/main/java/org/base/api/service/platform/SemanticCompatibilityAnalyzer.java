package org.base.api.service.platform;

import java.util.*;

/** Deterministic, provider-neutral semantic compatibility rules for contract revisions. */
public final class SemanticCompatibilityAnalyzer {
    public record Result(String compatibility, List<String> breakingChanges, List<String> compatibleChanges) {
        public boolean breaking() { return "BREAKING".equals(compatibility); }
        public boolean approvalRequired() { return breaking(); }
        public String migrationGuidance() {
            return breaking() ? "Create a new approved revision, migrate consumers, then supersede the prior revision." : "No consumer migration is required; publish the compatible revision after normal approval.";
        }
    }

    private SemanticCompatibilityAnalyzer() { }

    public static Result compare(Map<String, ?> from, Map<String, ?> to) {
        Objects.requireNonNull(from, "from"); Objects.requireNonNull(to, "to");
        List<String> breaking = new ArrayList<>(), compatible = new ArrayList<>();
        compareScalar("metric", from, to, breaking, compatible);
        compareScalar("unit", from, to, breaking, compatible);
        compareScalar("aggregation", from, to, breaking, compatible);
        compareScalar("qualityPolicy", from, to, breaking, compatible);
        compareScalar("privacyPolicy", from, to, breaking, compatible);
        compareScalar("responseShape", from, to, breaking, compatible);
        compareSet("dimensions", from, to, breaking, compatible, true);
        compareSet("allowedFilters", from, to, breaking, compatible, true);
        compareSet("allowedIncludes", from, to, breaking, compatible, true);
        compareSet("policies", from, to, breaking, compatible, true);
        compareSet("codeLists", from, to, breaking, compatible, true);
        compareSet("projectionFields", from, to, breaking, compatible, true);
        compareSet("responseFields", from, to, breaking, compatible, true);
        compareObject("projection", from.get("projection"), to.get("projection"), "projection", breaking, compatible);
        compareObject("responseSchema", from.get("responseSchema"), to.get("responseSchema"), "responseSchema", breaking, compatible);
        return new Result(breaking.isEmpty() ? "BACKWARD_COMPATIBLE" : "BREAKING", List.copyOf(breaking), List.copyOf(compatible));
    }

    /** Compares nested declarative metadata without interpreting provider-specific values. */
    private static void compareObject(String key, Object oldValue, Object newValue, String path,
                                      List<String> breaking, List<String> compatible) {
        if (oldValue == null && newValue == null) return;
        if (!(oldValue instanceof Map<?, ?> oldMap) || !(newValue instanceof Map<?, ?> newMap)) {
            if (!Objects.equals(oldValue, newValue)) breaking.add(path + " changed");
            else compatible.add(path + " unchanged");
            return;
        }
        Set<String> oldKeys = stringKeys(oldMap, path), newKeys = stringKeys(newMap, path);
        for (String removed : difference(oldKeys, newKeys)) breaking.add(path + " removed: " + removed);
        for (String added : difference(newKeys, oldKeys)) compatible.add(path + " added: " + added);
        for (String common : intersection(oldKeys, newKeys)) {
            Object a = oldMap.get(common), b = newMap.get(common);
            String child = path + "." + common;
            if (a instanceof Map<?, ?> && b instanceof Map<?, ?>) compareObject(common, a, b, child, breaking, compatible);
            else if (!Objects.equals(a, b)) breaking.add(child + " changed");
            else compatible.add(child + " unchanged");
        }
    }

    private static Set<String> stringKeys(Map<?, ?> map, String path) {
        Set<String> keys = new TreeSet<>();
        for (Object key : map.keySet()) {
            if (key == null || String.valueOf(key).isBlank()) throw new IllegalArgumentException(path + " contains a blank object key");
            keys.add(String.valueOf(key));
        }
        return keys;
    }
    private static Set<String> difference(Set<String> left, Set<String> right) { Set<String> out = new TreeSet<>(left); out.removeAll(right); return out; }
    private static Set<String> intersection(Set<String> left, Set<String> right) { Set<String> out = new TreeSet<>(left); out.retainAll(right); return out; }

    private static void compareScalar(String key, Map<String, ?> from, Map<String, ?> to, List<String> breaking, List<String> compatible) {
        if (!from.containsKey(key) && !to.containsKey(key)) return;
        Object a = from.get(key), b = to.get(key);
        if (!Objects.equals(a, b)) breaking.add(key + " changed"); else compatible.add(key + " unchanged");
    }

    private static void compareSet(String key, Map<String, ?> from, Map<String, ?> to, List<String> breaking, List<String> compatible, boolean removalBreaking) {
        Set<String> oldSet = asSet(from.get(key)), newSet = asSet(to.get(key));
        if (oldSet.equals(newSet)) { if (!oldSet.isEmpty()) compatible.add(key + " unchanged"); return; }
        Set<String> removed = new TreeSet<>(oldSet); removed.removeAll(newSet);
        Set<String> added = new TreeSet<>(newSet); added.removeAll(oldSet);
        if (removalBreaking && !removed.isEmpty()) breaking.add(key + " removed: " + removed);
        if (!added.isEmpty()) compatible.add(key + " added: " + added);
    }

    private static Set<String> asSet(Object value) {
        if (value == null) return Set.of();
        if (!(value instanceof Iterable<?> iterable)) throw new IllegalArgumentException("Semantic field must be an array: " + value);
        Set<String> result = new TreeSet<>();
        for (Object item : iterable) { if (item == null || String.valueOf(item).isBlank()) throw new IllegalArgumentException("Semantic array contains blank value"); result.add(String.valueOf(item)); }
        return result;
    }
}
