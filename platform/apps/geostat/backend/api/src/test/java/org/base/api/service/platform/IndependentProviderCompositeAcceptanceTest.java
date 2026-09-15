package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Provider-neutral acceptance using a non-default schema and composite identity. */
class IndependentProviderCompositeAcceptanceTest {
    @Test void alternateSchemaCompositeKeyAndCrossFamilyProjectionReplay() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:alt_provider;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "");
        var db = new JdbcTemplate(ds);
        db.execute("DROP ALL OBJECTS");
        db.execute("CREATE SCHEMA ALT_SITE");
        db.execute("CREATE TABLE ALT_SITE.entity_record (site_code VARCHAR(20), record_no INT, classifier_code VARCHAR(20), label VARCHAR(100), private_note VARCHAR(100), PRIMARY KEY(site_code,record_no))");
        db.execute("CREATE TABLE ALT_SITE.reference_item (code VARCHAR(20) PRIMARY KEY, title VARCHAR(100), secret VARCHAR(100))");
        db.execute("CREATE TABLE ALT_SITE.stat_observation (site_code VARCHAR(20), record_no INT, metric_value INT, unit VARCHAR(20), secret VARCHAR(100))");
        db.update("INSERT INTO ALT_SITE.reference_item VALUES ('GOAL','Goal label','hidden')");
        db.update("INSERT INTO ALT_SITE.entity_record VALUES ('ALT',7,'GOAL','Education','hidden')");
        db.update("INSERT INTO ALT_SITE.stat_observation VALUES ('ALT',7,12,'COUNT','hidden')");

        Set<String> declared = Set.of("site_code", "record_no", "classifier_code", "label");
        var plan = ContractQueryCompiler.compile(Map.of("site_code", "ALT", "record_no", 7), declared, "record_no", false);
        String sql = "SELECT site_code AS \"site_code\",record_no AS \"record_no\",classifier_code AS \"classifier_code\",label AS \"label\" FROM ALT_SITE.entity_record" + plan.whereSql().replace("[", "").replace("]", "") + plan.orderSql().replace("[", "").replace("]", "");
        List<Map<String,Object>> root = db.queryForList(sql, plan.parameters().toArray());
        List<Map<String,Object>> refs = db.queryForList("SELECT code AS \"classifier_code\",title AS \"title\" FROM ALT_SITE.reference_item");
        List<Map<String,Object>> stats = db.queryForList("SELECT site_code AS \"site_code\",record_no AS \"record_no\",metric_value AS \"metric_value\",unit AS \"unit\" FROM ALT_SITE.stat_observation");
        var withReference = ContractRelationGraphExecutor.attach(root, "classifier", refs, "classifier_code", "classifier_code", false);
        var withStats = ContractRelationGraphExecutor.attach(withReference, "observations", stats, "record_no", "record_no", true);
        var response = ContractIncludeSerializer.apply(withStats, List.of("label", "classifier.title", "observations.metric_value"));

        assertEquals(1, response.size());
        assertEquals("Education", response.get(0).get("label"));
        assertEquals("Goal label", ((Map<?,?>) response.get(0).get("classifier")).get("title"));
        assertEquals(12, ((Map<?,?>) ((List<?>) response.get(0).get("observations")).get(0)).get("metric_value"));
        assertFalse(response.get(0).containsKey("private_note"));
        assertFalse(((Map<?,?>) response.get(0).get("classifier")).containsKey("secret"));
        assertFalse(((Map<?,?>) ((List<?>) response.get(0).get("observations")).get(0)).containsKey("unit"));
    }
}
