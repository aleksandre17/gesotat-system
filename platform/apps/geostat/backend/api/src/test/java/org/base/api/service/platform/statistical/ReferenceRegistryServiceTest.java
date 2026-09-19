package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.registry.JdbcStatisticalRegistry;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.Failure;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.MeasureSpec;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.RegistryException;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.UnitSpec;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Read side and write side meet: what the service registers, the compiler can pin and approve. */
class ReferenceRegistryServiceTest {
    private JdbcTemplate jdbc;
    private ReferenceRegistryService service;
    private JdbcStatisticalRegistry reader;

    @BeforeEach void schema() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA platform");
        jdbc.execute("CREATE TABLE platform.contract_namespace(namespace_id BIGINT IDENTITY PRIMARY KEY, namespace_code VARCHAR(64) UNIQUE)");
        jdbc.execute("CREATE TABLE platform.data_product(product_id BIGINT IDENTITY PRIMARY KEY, product_code VARCHAR(120) UNIQUE)");
        jdbc.execute("""
                CREATE TABLE platform.statistical_reference(reference_id BIGINT IDENTITY PRIMARY KEY, kind VARCHAR(16) NOT NULL, namespace_id BIGINT NOT NULL,
                  code VARCHAR(120) NOT NULL, version_major INT NOT NULL, version_minor INT NOT NULL, version_patch INT NOT NULL, lifecycle_status VARCHAR(16) NOT NULL,
                  owner_product_id BIGINT, target_type VARCHAR(48), target_id BIGINT, decided_at TIMESTAMP, decided_by VARCHAR(255), definition_digest CHAR(64),
                  UNIQUE(kind,namespace_id,code,version_major,version_minor,version_patch))""");
        jdbc.execute("CREATE TABLE platform.statistical_unit(unit_id BIGINT IDENTITY PRIMARY KEY, unit_code VARCHAR(120) NOT NULL UNIQUE, quantity_kind VARCHAR(24) NOT NULL, scale_factor DECIMAL(28,10) NOT NULL, title VARCHAR(255) NOT NULL, status VARCHAR(24) NOT NULL)");
        jdbc.execute("""
                CREATE TABLE platform.measure(measure_id BIGINT IDENTITY PRIMARY KEY, measure_code VARCHAR(120) NOT NULL UNIQUE, value_type VARCHAR(24) NOT NULL,
                  aggregation_default VARCHAR(24) NOT NULL, title_ka VARCHAR(255) NOT NULL, unit_id BIGINT, concept_reference_id BIGINT, numeric_precision INT,
                  numeric_scale INT, approximate_numeric BIT DEFAULT 0 NOT NULL)""");
        jdbc.execute("CREATE TABLE platform.classification_item(classification_item_id BIGINT IDENTITY PRIMARY KEY, classification_version_id BIGINT, code VARCHAR(128), status VARCHAR(24))");
        jdbc.execute("""
                CREATE TABLE platform.statistical_component(component_id BIGINT IDENTITY PRIMARY KEY, dsd_id BIGINT, component_code VARCHAR(120), component_role VARCHAR(24),
                  component_order INT, required BIT, measure_id BIGINT, classification_version_id BIGINT, attachment_level VARCHAR(24), constraint_json VARCHAR(4000),
                  concept_reference_id BIGINT, representation_type VARCHAR(24))""");
        for (String ns : new String[]{"SHARED", "LAND", "LABOUR"}) jdbc.update("INSERT INTO platform.contract_namespace(namespace_code) VALUES(?)", ns);
        for (String p : new String[]{LABOUR.productCode(), LAND.productCode()}) jdbc.update("INSERT INTO platform.data_product(product_code) VALUES(?)", p);
        reader = new JdbcStatisticalRegistry(jdbc, new ObjectMapper());
        service = new ReferenceRegistryService(jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)), reader);
    }

    private void items(long version, String... codes) {
        for (String code : codes) jdbc.update("INSERT INTO platform.classification_item(classification_version_id,code,status) VALUES(?,?,'ACTIVE')", version, code);
    }

    private Ref approvedIdentity(String wire, String owner) { Ref ref = service.proposeIdentity(Ref.parse(wire), owner); service.approve(ref, "steward"); return ref; }

    private static Failure failure(Runnable action) { return assertThrows(RegistryException.class, action::run).failure(); }

    @Test void whatIsRegisteredCanBePinnedAndApprovedByTheCompiler() {
        for (String wire : new String[]{"profile:SHARED:STAT_AGGREGATE(1.0.0)", "concept:SHARED:TIME_PERIOD(1.0.0)", "concept:SHARED:REF_AREA(1.0.0)", "concept:SHARED:SEX(1.0.0)",
                "concept:SHARED:OBS_STATUS(1.0.0)", "concept:SHARED:EMPLOYED(1.0.0)", "concept:SHARED:UNEMPLOYED(1.0.0)", "concept:SHARED:SOURCE_NOTE(1.0.0)"})
            approvedIdentity(wire, null);
        Ref persons = service.proposeUnit(Ref.parse("unit:SHARED:PERSONS(1.0.0)"), null, new UnitSpec("COUNT", "Persons"));
        service.approve(persons, "steward");
        for (String code : new String[]{"EMPLOYED", "UNEMPLOYED"}) {
            Ref measure = service.proposeMeasure(Ref.parse("measure:SHARED:" + code + "(1.0.0)"), null,
                    new MeasureSpec(Ref.parse("concept:SHARED:" + code + "(1.0.0)"), persons, 18, 0, false, code));
            service.approve(measure, "steward");
        }
        items(11, "GE", "GE_TB", "GE_KA"); items(12, "F", "M", "_T"); items(13, "A", "E", "M", "O", "P");
        service.approve(service.proposeCodelist(CL_AREA, null, 11), "steward");
        service.approve(service.proposeCodelist(CL_SEX, null, 12), "steward");
        service.approve(service.proposeCodelist(CL_OBS_STATUS, null, 13), "steward");

        String draft = labourDraft().replace("\"policyRefs\":[\"policy:SHARED:QUALITY_BASELINE(1.0.0)\"],", "");
        var fromDatabase = compiler(reader).compile(draft, LABOUR, Mode.APPROVAL);
        assertTrue(fromDatabase.accepted(), fromDatabase.issues().toString());
        assertEquals(compiler(registry()).compile(draft, LABOUR, Mode.APPROVAL).plan().orElseThrow().semanticDigest(), fromDatabase.plan().orElseThrow().semanticDigest(),
                "registered through the service or seeded in memory: the same contract");
    }

    @Test void proposalIsIdempotentAndAnExactVersionIsDefinedOnce() {
        Ref concept = approvedIdentity("concept:SHARED:EMPLOYED(1.0.0)", null);
        Ref unit = service.proposeUnit(Ref.parse("unit:SHARED:PERSONS(1.0.0)"), null, new UnitSpec("COUNT", "Persons"));
        assertEquals(unit, service.proposeUnit(unit, null, new UnitSpec("COUNT", "Persons")), "double submit");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM platform.statistical_unit", Integer.class));
        assertEquals(Failure.ALREADY_DEFINED_DIFFERENTLY, failure(() -> service.proposeUnit(unit, null, new UnitSpec("MASS", "Persons"))));
        assertEquals(Failure.ALREADY_DEFINED_DIFFERENTLY, failure(() -> service.proposeUnit(unit, LAND.productCode(), new UnitSpec("COUNT", "Persons"))), "scope is part of the definition");

        MeasureSpec spec = new MeasureSpec(concept, unit, 18, 0, false, "Employed");
        Ref measure = service.proposeMeasure(Ref.parse("measure:SHARED:EMPLOYED(1.0.0)"), null, spec);
        assertEquals(Failure.ALREADY_DEFINED_DIFFERENTLY, failure(() -> service.proposeMeasure(measure, null, new MeasureSpec(concept, unit, 18, 2, false, "Employed"))));
        Ref corrected = service.proposeMeasure(Ref.parse("measure:SHARED:EMPLOYED(1.1.0)"), null, new MeasureSpec(concept, unit, 18, 2, false, "Employed"));
        assertNotEquals(measure, corrected, "a correction is a new version");
    }

    @Test void stewardDecidesInDependencyOrderAndDecisionsAreOneWay() {
        Ref concept = service.proposeIdentity(Ref.parse("concept:SHARED:EMPLOYED(1.0.0)"), null);
        Ref unit = service.proposeUnit(Ref.parse("unit:SHARED:PERSONS(1.0.0)"), null, new UnitSpec("COUNT", "Persons"));
        Ref measure = service.proposeMeasure(Ref.parse("measure:SHARED:EMPLOYED(1.0.0)"), null, new MeasureSpec(concept, unit, 18, 0, false, "Employed"));
        assertEquals(Failure.UNRESOLVED_DEPENDENCY, failure(() -> service.approve(measure, "steward")), "a measure cannot outrun its concept and unit");
        service.approve(concept, "steward");
        service.approve(unit, "steward");
        service.approve(measure, "steward");
        service.approve(measure, "steward"); // replay of the same decision
        assertEquals(Lifecycle.APPROVED, reader.lifecycle(measure, LABOUR).orElseThrow());
        service.supersede(measure, "steward");
        assertEquals(Failure.ILLEGAL_TRANSITION, failure(() -> service.approve(measure, "steward")), "superseded never returns");
        assertEquals(Failure.NOT_FOUND, failure(() -> service.approve(Ref.parse("measure:SHARED:NOPE(1.0.0)"), "steward")));
        assertEquals(Failure.INVALID_DEFINITION, failure(() -> service.approve(concept, " ")));
    }

    @Test void scopeRulesHoldOnTheWriteSide() {
        Ref landConcept = approvedIdentity("concept:LAND:AREA_SIZE(1.0.0)", LAND.productCode());
        Ref hectare = service.proposeUnit(Ref.parse("unit:SHARED:HECTARE(1.0.0)"), null, new UnitSpec("AREA", "Hectare"));
        assertEquals(Failure.UNRESOLVED_DEPENDENCY, failure(() -> service.proposeMeasure(Ref.parse("measure:SHARED:AREA_SIZE(1.0.0)"), null,
                new MeasureSpec(landConcept, hectare, 28, 10, false, "Area"))), "a GLOBAL measure cannot depend on a product-owned concept");
        assertEquals(Failure.UNRESOLVED_DEPENDENCY, failure(() -> service.proposeMeasure(Ref.parse("measure:LABOUR:AREA_SIZE(1.0.0)"), LABOUR.productCode(),
                new MeasureSpec(landConcept, hectare, 28, 10, false, "Area"))), "nor can another product");
        Ref owned = service.proposeMeasure(Ref.parse("measure:LAND:AREA_SIZE(1.0.0)"), LAND.productCode(), new MeasureSpec(landConcept, hectare, 28, 10, false, "Area"));
        assertTrue(reader.lifecycle(owned, LAND).isPresent());
        assertTrue(reader.lifecycle(owned, LABOUR).isEmpty());
    }

    @Test void invalidDefinitionsAreRefusedBeforeAnythingIsWritten() {
        Ref concept = approvedIdentity("concept:SHARED:X(1.0.0)", null);
        assertEquals(Failure.INVALID_DEFINITION, failure(() -> service.proposeMeasure(Ref.parse("measure:SHARED:X(1.0.0)"), null, new MeasureSpec(concept, null, 30, 12, false, "x"))));
        assertEquals(Failure.INVALID_DEFINITION, failure(() -> service.proposeIdentity(Ref.parse("unit:SHARED:X(1.0.0)"), null)));
        assertEquals(Failure.UNKNOWN_NAMESPACE, failure(() -> service.proposeIdentity(Ref.parse("concept:NOWHERE:X(1.0.0)"), null)));
        assertEquals(Failure.UNKNOWN_PRODUCT, failure(() -> service.proposeIdentity(Ref.parse("concept:SHARED:Y(1.0.0)"), "NO_SUCH_PRODUCT")));
        assertEquals(Failure.UNRESOLVED_DEPENDENCY, failure(() -> service.proposeCodelist(Ref.parse("codelist:SHARED:EMPTY(1.0.0)"), null, 999)));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM platform.measure", Integer.class));
        Set<String> kinds = jdbc.queryForList("SELECT kind FROM platform.statistical_reference", String.class).stream().collect(Collectors.toSet());
        assertEquals(Set.of("CONCEPT"), kinds);
    }
}
