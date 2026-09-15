package org.base.api.service.platform.access;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SemanticAccessContractCompatibilityTest {
    @Test
    void preservedHistoricalRevisionStillResolvesItsImmutableBindings() {
        JdbcTemplate control=mock(JdbcTemplate.class, call -> {
            if(!call.getMethod().getName().equals("queryForList")) return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
            String sql=call.getArgument(0);
            if(sql.contains("WHERE c.contract_code")) return List.of(Map.of("contract_id",77L,"contract_revision",6,"status","REVIEW_REQUIRED","product_code","KIDS_PORTAL"));
            if(sql.contains("FROM platform.ingestion_contract_revision")) return List.of(Map.of("ingestion_contract_revision_id",55L,"lifecycle_status","REVIEW_REQUIRED"));
            if(sql.contains("FROM platform.contract_revision_source")) return List.of(Map.of("dataset_code","KIDS_RESOURCE","source_locator","ACCESS.kids_resource"));
            return List.of();
        });
        var pack=new SemanticAccessPackage("KIDS_PORTAL","KIDS_PORTAL_V1",5,"kids-r5","4.3.0",
                List.of(new SemanticAccessDataset("KIDS_RESOURCE","kids_resource","ENTITY","one resource")),
                List.of(),List.of(),List.of(),List.of(),List.of());

        var issues=new SemanticAccessControlPlaneResolver(control).validate(pack);
        assertTrue(issues.isEmpty(), () -> "historical revision issues: " + issues);
    }
}
