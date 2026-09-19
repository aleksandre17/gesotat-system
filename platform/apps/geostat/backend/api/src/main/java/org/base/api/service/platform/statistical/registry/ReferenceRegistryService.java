package org.base.api.service.platform.statistical.registry;

import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Lifecycle;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Scope;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Write side of the versioned registry (register Q07, Q09): an author proposes, a steward decides, nobody edits
 * an approved version — a correction is a new version. Meaning is written once into the existing registry
 * tables; {@code platform.statistical_reference} receives only identity, scope and lifecycle.
 *
 * A proposal is idempotent: proposing the identical definition again returns the first proposal; the same
 * identity with another definition is a conflict. {@code ownerProduct == null} means GLOBAL scope, which the
 * caller must be separately authorised for (the HTTP boundary decides that, not this class).
 */
public final class ReferenceRegistryService {

    public enum Failure { ALREADY_DEFINED_DIFFERENTLY, UNKNOWN_NAMESPACE, UNKNOWN_PRODUCT, UNRESOLVED_DEPENDENCY, NOT_FOUND, ILLEGAL_TRANSITION, INVALID_DEFINITION }

    public static final class RegistryException extends RuntimeException {
        private final Failure failure;
        RegistryException(Failure failure, String message) { super(message); this.failure = failure; }
        public Failure failure() { return failure; }
    }

    public record MeasureSpec(Ref conceptRef, Ref unitRef, int precision, int scale, boolean approximate, String title) { }

    public record UnitSpec(String quantityKind, String title) { }

    private final JdbcTemplate control;
    private final TransactionTemplate transaction;
    private final StatisticalRegistry reader;

    public ReferenceRegistryService(JdbcTemplate control, TransactionTemplate transaction, StatisticalRegistry reader) {
        this.control = control;
        this.transaction = transaction;
        this.reader = reader;
    }

    /** Concepts and profiles are identity-only artefacts; their labels live in the metadata plane. */
    public Ref proposeIdentity(Ref ref, String ownerProduct) {
        if (ref.kind() != Ref.Kind.CONCEPT && ref.kind() != Ref.Kind.PROFILE) throw fail(Failure.INVALID_DEFINITION, "only concepts and profiles are identity-only");
        return transaction.execute(status -> insert(ref, ownerProduct, null, null, "identity"));
    }

    public Ref proposeUnit(Ref ref, String ownerProduct, UnitSpec spec) {
        require(ref, Ref.Kind.UNIT);
        if (blank(spec.quantityKind()) || blank(spec.title())) throw fail(Failure.INVALID_DEFINITION, "quantity kind and title are required");
        return transaction.execute(status -> {
            Optional<Ref> existing = existing(ref, ownerProduct, spec.quantityKind() + "|" + spec.title());
            if (existing.isPresent()) return existing.get();
            control.update("INSERT INTO platform.statistical_unit(unit_code,quantity_kind,scale_factor,title,status) VALUES(?,?,1,?,'PROPOSED')", physicalCode(ref), spec.quantityKind(), spec.title());
            long unitId = control.queryForObject("SELECT unit_id FROM platform.statistical_unit WHERE unit_code=?", Long.class, physicalCode(ref));
            return insert(ref, ownerProduct, "STATISTICAL_UNIT", unitId, spec.quantityKind() + "|" + spec.title());
        });
    }

    public Ref proposeMeasure(Ref ref, String ownerProduct, MeasureSpec spec) {
        require(ref, Ref.Kind.MEASURE);
        if (!spec.approximate() && (spec.precision() < 1 || spec.precision() > 28 || spec.scale() < 0 || spec.scale() > 10 || spec.scale() > spec.precision()))
            throw fail(Failure.INVALID_DEFINITION, "an exact measure must fit precision 28 and scale 10");
        if (blank(spec.title()) || spec.conceptRef() == null) throw fail(Failure.INVALID_DEFINITION, "title and conceptRef are required");
        Scope scope = new Scope(ownerProduct == null ? "*" : ownerProduct);
        String definition = spec.conceptRef() + "|" + spec.unitRef() + "|" + spec.precision() + "|" + spec.scale() + "|" + spec.approximate();
        return transaction.execute(status -> {
            Optional<Ref> existing = existing(ref, ownerProduct, definition);
            if (existing.isPresent()) return existing.get();
            // A GLOBAL measure may depend only on GLOBAL entries: the "*" scope sees nothing product-owned.
            long conceptId = referenceId(spec.conceptRef(), scope);
            Long unitId = spec.unitRef() == null ? null : targetId(spec.unitRef(), scope);
            control.update("""
                    INSERT INTO platform.measure(measure_code,value_type,aggregation_default,title_ka,unit_id,concept_reference_id,numeric_precision,numeric_scale,approximate_numeric)
                    VALUES(?,?,'NONE',?,?,?,?,?,?)""", physicalCode(ref), spec.approximate() ? "DOUBLE" : "DECIMAL", spec.title(), unitId, conceptId,
                    spec.approximate() ? null : spec.precision(), spec.approximate() ? null : spec.scale(), spec.approximate());
            long measureId = control.queryForObject("SELECT measure_id FROM platform.measure WHERE measure_code=?", Long.class, physicalCode(ref));
            return insert(ref, ownerProduct, "MEASURE", measureId, definition);
        });
    }

    /** Binds an exact codelist version to an existing classification version; the items stay where they are. */
    public Ref proposeCodelist(Ref ref, String ownerProduct, long classificationVersionId) {
        require(ref, Ref.Kind.CODELIST);
        return transaction.execute(status -> {
            Optional<Ref> existing = existing(ref, ownerProduct, String.valueOf(classificationVersionId));
            if (existing.isPresent()) return existing.get();
            Integer items = control.queryForObject("SELECT COUNT(*) FROM platform.classification_item WHERE classification_version_id=? AND status='ACTIVE'", Integer.class, classificationVersionId);
            if (items == null || items == 0) throw fail(Failure.UNRESOLVED_DEPENDENCY, "the classification version has no active items");
            return insert(ref, ownerProduct, "CLASSIFICATION_VERSION", classificationVersionId, String.valueOf(classificationVersionId));
        });
    }

    public void approve(Ref ref, String steward) { decide(ref, steward, Lifecycle.PROPOSED, Lifecycle.APPROVED); }

    public void supersede(Ref ref, String steward) { decide(ref, steward, Lifecycle.APPROVED, Lifecycle.SUPERSEDED); }

    private void decide(Ref ref, String steward, Lifecycle from, Lifecycle to) {
        if (blank(steward)) throw fail(Failure.INVALID_DEFINITION, "the deciding steward is required");
        transaction.executeWithoutResult(status -> {
            // Approval requires approved dependencies: a measure cannot outrun its concept or unit.
            if (to == Lifecycle.APPROVED && ref.kind() == Ref.Kind.MEASURE) {
                Integer pending = control.queryForObject("""
                        SELECT COUNT(*) FROM platform.statistical_reference d
                        WHERE d.lifecycle_status <> 'APPROVED' AND (d.reference_id = (SELECT m.concept_reference_id FROM platform.measure m WHERE m.measure_id = ?)
                           OR (d.target_type = 'STATISTICAL_UNIT' AND d.target_id = (SELECT m.unit_id FROM platform.measure m WHERE m.measure_id = ?)))""",
                        Integer.class, targetOf(ref), targetOf(ref));
                if (pending != null && pending > 0) throw fail(Failure.UNRESOLVED_DEPENDENCY, "approve the concept and the unit first");
            }
            Object[] id = args(ref);
            int rows = control.update(identityWhere("UPDATE platform.statistical_reference SET lifecycle_status=?, decided_at=COALESCE(decided_at,?), decided_by=COALESCE(decided_by,?)") + " AND lifecycle_status=?",
                    to.name(), Timestamp.from(Instant.now()), steward, id[0], id[1], id[2], id[3], id[4], id[5], from.name());
            if (rows == 1) return;
            Optional<String> current = lifecycleOf(ref);
            if (current.isEmpty()) throw fail(Failure.NOT_FOUND, "reference not found: " + ref);
            if (!current.get().equals(to.name())) throw fail(Failure.ILLEGAL_TRANSITION, "not allowed from " + current.get());
        });
    }

    /** Owning product code, {@code "*"} for GLOBAL, empty when the reference does not exist. */
    public Optional<String> ownerOf(Ref ref) {
        return one(control.queryForList(identityWhere("SELECT COALESCE((SELECT p.product_code FROM platform.data_product p WHERE p.product_id = owner_product_id), '*') FROM platform.statistical_reference"), String.class, args(ref)));
    }

    // ---- internals

    private Ref insert(Ref ref, String ownerProduct, String targetType, Long targetId, String definition) {
        Optional<Ref> existing = existing(ref, ownerProduct, definition);
        if (existing.isPresent()) return existing.get();
        Long namespaceId = one(control.queryForList("SELECT namespace_id FROM platform.contract_namespace WHERE namespace_code=?", Long.class, ref.namespace()))
                .orElseThrow(() -> fail(Failure.UNKNOWN_NAMESPACE, "namespace is not registered: " + ref.namespace()));
        Long ownerId = ownerProduct == null ? null : one(control.queryForList("SELECT product_id FROM platform.data_product WHERE product_code=?", Long.class, ownerProduct))
                .orElseThrow(() -> fail(Failure.UNKNOWN_PRODUCT, "product not found"));
        try {
            control.update("INSERT INTO platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch,lifecycle_status,owner_product_id,target_type,target_id,definition_digest) VALUES(?,?,?,?,?,?,'PROPOSED',?,?,?,?)",
                    ref.kind().name(), namespaceId, ref.code(), ref.version().major(), ref.version().minor(), ref.version().patch(), ownerId, targetType, targetId, digest(ownerProduct, definition));
        } catch (DuplicateKeyException raced) {
            return existing(ref, ownerProduct, definition).orElseThrow(() -> fail(Failure.ALREADY_DEFINED_DIFFERENTLY, "this exact version already exists with another definition"));
        }
        return ref;
    }

    private Optional<Ref> existing(Ref ref, String ownerProduct, String definition) {
        List<String> digests = control.queryForList(identityWhere("SELECT definition_digest FROM platform.statistical_reference"), String.class, args(ref));
        if (digests.isEmpty()) return Optional.empty();
        if (!digest(ownerProduct, definition).equals(digests.get(0))) throw fail(Failure.ALREADY_DEFINED_DIFFERENTLY, "this exact version already exists with another definition");
        return Optional.of(ref);
    }

    private static String digest(String ownerProduct, String definition) {
        return org.base.api.service.platform.statistical.canonical.CanonicalJson.digest("geostat.stat-reference-definition.v1", List.of(ownerProduct == null ? "*" : ownerProduct, definition));
    }

    private long referenceId(Ref ref, Scope scope) {
        if (reader.lifecycle(ref, scope).isEmpty()) throw fail(Failure.UNRESOLVED_DEPENDENCY, "dependency does not resolve in this scope: " + ref);
        return control.queryForObject(identityWhere("SELECT reference_id FROM platform.statistical_reference"), Long.class, args(ref));
    }

    private long targetId(Ref ref, Scope scope) {
        if (reader.lifecycle(ref, scope).isEmpty()) throw fail(Failure.UNRESOLVED_DEPENDENCY, "dependency does not resolve in this scope: " + ref);
        return targetOf(ref);
    }

    private long targetOf(Ref ref) {
        return one(control.queryForList(identityWhere("SELECT target_id FROM platform.statistical_reference"), Long.class, args(ref)))
                .orElseThrow(() -> fail(Failure.NOT_FOUND, "reference not found: " + ref));
    }

    private Optional<String> lifecycleOf(Ref ref) {
        return one(control.queryForList(identityWhere("SELECT lifecycle_status FROM platform.statistical_reference"), String.class, args(ref)));
    }

    private static String identityWhere(String head) {
        return head + " WHERE kind=? AND namespace_id=(SELECT namespace_id FROM platform.contract_namespace WHERE namespace_code=?) AND code=? AND version_major=? AND version_minor=? AND version_patch=?";
    }

    private static Object[] args(Ref ref) {
        return new Object[]{ref.kind().name(), ref.namespace(), ref.code(), ref.version().major(), ref.version().minor(), ref.version().patch()};
    }

    /** Surrogate for the legacy unique code columns; the logical identity lives in the reference row. */
    private static String physicalCode(Ref ref) {
        String code = ref.namespace() + "." + ref.code() + "." + ref.version();
        if (code.length() > 120) throw fail(Failure.INVALID_DEFINITION, "namespace, code and version together exceed 120 characters");
        return code;
    }

    private static void require(Ref ref, Ref.Kind kind) { if (ref.kind() != kind) throw fail(Failure.INVALID_DEFINITION, "expected a " + kind.wire() + " reference"); }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static <T> Optional<T> one(List<T> rows) { return rows.size() == 1 ? Optional.ofNullable(rows.get(0)) : Optional.empty(); }

    private static RegistryException fail(Failure failure, String message) { return new RegistryException(failure, message); }
}
