package org.base.api.service.platform.statistical.compiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Component.Attachment;
import org.base.api.service.platform.statistical.model.Component.AttachmentLevel;
import org.base.api.service.platform.statistical.model.ContractDraft;
import org.base.api.service.platform.statistical.model.ContractDraft.ConstantBinding;
import org.base.api.service.platform.statistical.model.ContractDraft.ErrorMode;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.model.Representation.TimeFormat;
import org.base.api.service.platform.statistical.model.SemVer;
import org.base.api.service.platform.statistical.model.StructureDefinition;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Closed-grammar parser of the authoring document. Anything the grammar does not name is rejected, and
 * every finding is collected so one round-trip shows the author all shape errors (register Q31, Q32).
 * The published JSON Schema (resources/contracts/statistical-contract-draft.schema.json) states the same shape.
 */
public final class ContractDraftParser {
    private static final Set<String> ROOT = Set.of("profileRef", "dataset", "structure", "constantBindings",
            "policyRefs", "sourceProfile", "errorMode", "presentation");
    private static final Set<String> MEASURE_FORBIDDEN = Set.of("conceptRef", "representation", "unitRef", "unit");

    private final ObjectMapper mapper;

    public ContractDraftParser(ObjectMapper mapper) { this.mapper = mapper; }

    public record Parsed(Optional<ContractDraft> draft, List<ContractIssue> issues) { }

    public Parsed parse(String document) {
        List<ContractIssue> issues = new ArrayList<>();
        JsonNode root;
        try {
            root = mapper.readTree(document);
        } catch (Exception e) {
            return new Parsed(Optional.empty(), List.of(ContractIssue.of(Code.MALFORMED_DOCUMENT, "", "document is not valid JSON")));
        }
        if (root == null || !root.isObject())
            return new Parsed(Optional.empty(), List.of(ContractIssue.of(Code.MALFORMED_DOCUMENT, "", "document must be a JSON object")));

        closed(root, ROOT, "", issues);
        Ref profile = ref(root, "profileRef", "", Ref.Kind.PROFILE, true, issues);
        JsonNode dataset = object(root, "dataset", "", issues);
        String namespace = null, code = null;
        if (dataset != null) {
            closed(dataset, Set.of("namespace", "code"), "/dataset", issues);
            namespace = id(dataset, "namespace", "/dataset", issues);
            code = id(dataset, "code", "/dataset", issues);
        }

        Ref existing = null;
        StructureDefinition inline = null;
        JsonNode structure = object(root, "structure", "", issues);
        if (structure != null) {
            closed(structure, Set.of("existingStructureRef", "inline"), "/structure", issues);
            if (structure.has("existingStructureRef") == structure.has("inline"))
                issues.add(ContractIssue.of(Code.STRUCTURE_CHOICE_AMBIGUOUS, "/structure", "exactly one of existingStructureRef and inline is required"));
            else if (structure.has("inline")) inline = inline(structure.get("inline"), namespace, issues);
            else existing = ref(structure, "existingStructureRef", "/structure", Ref.Kind.DSD, true, issues);
        }

        List<ConstantBinding> constants = new ArrayList<>();
        each(root, "constantBindings", "", issues, (node, path) -> {
            closed(node, Set.of("component", "value", "overridable"), path, issues);
            String component = id(node, "component", path, issues);
            String value = text(node, "value", path, true, issues);
            constants.add(new ConstantBinding(component, value, node.path("overridable").asBoolean(false)));
        });

        List<Ref> policies = new ArrayList<>();
        JsonNode policyRefs = root.get("policyRefs");
        if (policyRefs != null) {
            if (!policyRefs.isArray()) issues.add(ContractIssue.of(Code.INVALID_VALUE, "/policyRefs", "must be an array"));
            else for (int i = 0; i < policyRefs.size(); i++)
                parseRef(policyRefs.get(i).asText(null), "/policyRefs/" + i, Ref.Kind.POLICY, issues).ifPresent(policies::add);
        }

        String sourceProfile = id(root, "sourceProfile", "", issues);
        ErrorMode errorMode = enumOf(ErrorMode.class, text(root, "errorMode", "", false, issues), ErrorMode.ATOMIC_REJECT, "/errorMode", issues);
        Map<String, Map<String, String>> captions = captions(root.get("presentation"), issues);

        if (!issues.isEmpty()) return new Parsed(Optional.empty(), List.copyOf(issues));
        return new Parsed(Optional.of(new ContractDraft(profile, namespace, code, existing, inline, constants, policies,
                sourceProfile, errorMode, captions)), List.of());
    }

    private StructureDefinition inline(JsonNode node, String namespace, List<ContractIssue> issues) {
        String path = "/structure/inline";
        if (!node.isObject()) { issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "must be an object")); return null; }
        closed(node, Set.of("code", "version", "dimensions", "measures", "attributes"), path, issues);
        String code = id(node, "code", path, issues);
        SemVer version = null;
        try { version = SemVer.parse(text(node, "version", path, true, issues)); }
        catch (IllegalArgumentException e) { issues.add(ContractIssue.of(Code.INVALID_VALUE, path + "/version", e.getMessage())); }

        List<Component> components = new ArrayList<>();
        each(node, "dimensions", path, issues, (n, p) -> {
            closed(n, Set.of("code", "conceptRef", "representation"), p, issues);
            components.add(Component.dimension(id(n, "code", p, issues), ref(n, "conceptRef", p, Ref.Kind.CONCEPT, true, issues),
                    representation(n.get("representation"), p + "/representation", issues)));
        });
        each(node, "measures", path, issues, (n, p) -> {
            for (String forbidden : MEASURE_FORBIDDEN)
                if (n.has(forbidden)) issues.add(ContractIssue.of(Code.REDUNDANT_MEASURE_SEMANTICS, p + "/" + forbidden,
                        "measure semantics come from measureRef and are not restated"));
            closed(n, union(Set.of("code", "measureRef", "required"), MEASURE_FORBIDDEN), p, issues);
            components.add(Component.measure(id(n, "code", p, issues), ref(n, "measureRef", p, Ref.Kind.MEASURE, true, issues),
                    n.path("required").asBoolean(true)));
        });
        each(node, "attributes", path, issues, (n, p) -> {
            closed(n, Set.of("code", "conceptRef", "representation", "attachment", "required"), p, issues);
            components.add(Component.attribute(id(n, "code", p, issues), ref(n, "conceptRef", p, Ref.Kind.CONCEPT, true, issues),
                    representation(n.get("representation"), p + "/representation", issues),
                    attachment(n.get("attachment"), p + "/attachment", issues), n.path("required").asBoolean(false)));
        });
        if (code == null || version == null || namespace == null) return null;
        return new StructureDefinition(new Ref(Ref.Kind.DSD, namespace, code, version), components);
    }

    private Representation representation(JsonNode node, String path, List<ContractIssue> issues) {
        if (node == null || !node.isObject()) { issues.add(ContractIssue.of(Code.MISSING_FIELD, path, "representation is required")); return null; }
        String type = text(node, "type", path, true, issues);
        if (type == null) return null;
        try {
            switch (type) {
                case "CODED" -> {
                    closed(node, Set.of("type", "codelistRef"), path, issues);
                    Ref codelist = ref(node, "codelistRef", path, Ref.Kind.CODELIST, true, issues);
                    return codelist == null ? null : new Representation.Coded(codelist);
                }
                case "TIME_PERIOD" -> {
                    closed(node, Set.of("type", "formats"), path, issues);
                    Set<TimeFormat> formats = EnumSet.noneOf(TimeFormat.class);
                    for (JsonNode f : node.path("formats")) formats.add(TimeFormat.valueOf(f.asText()));
                    return new Representation.TimePeriod(formats);
                }
                case "INTEGER" -> {
                    closed(node, Set.of("type", "min", "max"), path, issues);
                    if (!node.path("min").isIntegralNumber() || !node.path("max").isIntegralNumber() || node.get("min").asLong() > node.get("max").asLong())
                        throw new IllegalArgumentException("integer bounds min <= max are required");
                    return new Representation.IntegerRange(node.get("min").asLong(), node.get("max").asLong());
                }
                case "TEXT" -> {
                    closed(node, Set.of("type", "maxLength"), path, issues);
                    int max = node.path("maxLength").asInt(0);
                    if (max < 1 || max > 255) throw new IllegalArgumentException("maxLength must be within 1..255");
                    return new Representation.BoundedText(max);
                }
                default -> throw new IllegalArgumentException("unknown representation type: " + type);
            }
        } catch (IllegalArgumentException e) {
            issues.add(ContractIssue.of(Code.INVALID_VALUE, path, e.getMessage()));
            return null;
        }
    }

    private Attachment attachment(JsonNode node, String path, List<ContractIssue> issues) {
        if (node == null || !node.isObject()) { issues.add(ContractIssue.of(Code.MISSING_FIELD, path, "attachment is required")); return null; }
        AttachmentLevel level = enumOf(AttachmentLevel.class, text(node, "level", path, true, issues), null, path + "/level", issues);
        if (level == null) return null;
        Set<String> allowed = switch (level) {
            case DIMENSION_GROUP -> Set.of("level", "dimensions");
            case MEASURE -> Set.of("level", "measure");
            default -> Set.of("level");
        };
        closed(node, allowed, path, issues);
        List<String> dimensions = new ArrayList<>();
        for (JsonNode d : node.path("dimensions")) dimensions.add(d.asText());
        return new Attachment(level, dimensions, node.hasNonNull("measure") ? node.get("measure").asText() : null);
    }

    private Map<String, Map<String, String>> captions(JsonNode presentation, List<ContractIssue> issues) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        if (presentation == null) return out;
        closed(presentation, Set.of("captions"), "/presentation", issues);
        Iterator<Map.Entry<String, JsonNode>> components = presentation.path("captions").fields();
        while (components.hasNext()) {
            Map.Entry<String, JsonNode> component = components.next();
            Map<String, String> byLanguage = new LinkedHashMap<>();
            component.getValue().fields().forEachRemaining(e -> byLanguage.put(e.getKey(), e.getValue().asText()));
            out.put(component.getKey(), byLanguage);
        }
        return out;
    }

    private interface ItemParser { void accept(JsonNode node, String path); }

    private static void each(JsonNode parent, String field, String parentPath, List<ContractIssue> issues, ItemParser parser) {
        JsonNode array = parent.get(field);
        if (array == null) return;
        String path = parentPath + "/" + field;
        if (!array.isArray()) { issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "must be an array")); return; }
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).isObject()) parser.accept(array.get(i), path + "/" + i);
            else issues.add(ContractIssue.of(Code.INVALID_VALUE, path + "/" + i, "must be an object"));
        }
    }

    private static void closed(JsonNode node, Set<String> allowed, String path, List<ContractIssue> issues) {
        node.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) issues.add(ContractIssue.of(Code.UNKNOWN_FIELD, path + "/" + name, "field is not part of the grammar"));
        });
    }

    private static JsonNode object(JsonNode parent, String field, String path, List<ContractIssue> issues) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isObject()) {
            issues.add(ContractIssue.of(Code.MISSING_FIELD, path + "/" + field, "object is required"));
            return null;
        }
        return node;
    }

    private static String text(JsonNode parent, String field, String path, boolean required, List<ContractIssue> issues) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) {
            if (required) issues.add(ContractIssue.of(Code.MISSING_FIELD, path + "/" + field, "value is required"));
            return null;
        }
        if (!node.isTextual()) { issues.add(ContractIssue.of(Code.INVALID_VALUE, path + "/" + field, "must be a string")); return null; }
        return node.asText();
    }

    private static String id(JsonNode parent, String field, String path, List<ContractIssue> issues) {
        String value = text(parent, field, path, true, issues);
        if (value != null && !value.matches(Ref.ID)) {
            issues.add(ContractIssue.of(Code.INVALID_VALUE, path + "/" + field, "must be an identifier: letter, then letters, digits or underscore"));
            return null;
        }
        return value;
    }

    private static Ref ref(JsonNode parent, String field, String path, Ref.Kind kind, boolean required, List<ContractIssue> issues) {
        String wire = text(parent, field, path, required, issues);
        return wire == null ? null : parseRef(wire, path + "/" + field, kind, issues).orElse(null);
    }

    private static Optional<Ref> parseRef(String wire, String path, Ref.Kind kind, List<ContractIssue> issues) {
        try {
            Ref ref = Ref.parse(wire);
            if (ref.kind() == kind) return Optional.of(ref);
            issues.add(ContractIssue.of(Code.INVALID_REFERENCE, path, "expected a " + kind.wire() + " reference"));
        } catch (IllegalArgumentException e) {
            issues.add(ContractIssue.of(Code.INVALID_REFERENCE, path, e.getMessage()));
        }
        return Optional.empty();
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String value, E fallback, String path, List<ContractIssue> issues) {
        if (value == null) return fallback;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException e) {
            issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "not one of " + List.of(type.getEnumConstants())));
            return fallback;
        }
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> out = new java.util.HashSet<>(a);
        out.addAll(b);
        return out;
    }
}
