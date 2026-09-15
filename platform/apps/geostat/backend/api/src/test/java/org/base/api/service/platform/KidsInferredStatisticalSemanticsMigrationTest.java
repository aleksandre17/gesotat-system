package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KidsInferredStatisticalSemanticsMigrationTest {
    @Test
    void revisionSevenBindsEveryDiscoveredCarrierWithoutSeedingObservations() throws Exception {
        try(var stream=getClass().getClassLoader().getResourceAsStream("db/platform/023_kids_inferred_statistical_semantics.sql")) {
            assertTrue(stream!=null);
            String sql=new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var carrierBinding=Pattern.compile("\\(N'\\d+',N'KIDS_[A-Z0-9_]+',N'[A-Z0-9_]+'").matcher(sql);
            int count=0;while(carrierBinding.find())count++;
            assertEquals(43,count,"all discovered carriers require an explicit metric and unit inference");
            assertTrue(sql.contains("KIDS_AGGREGATE_QUALITY_V1"));
            assertTrue(sql.contains("KIDS_PUBLIC_AGGREGATE_V1"));
            assertTrue(sql.contains("aggregation='NONE'"));
            assertTrue(sql.contains("DELEGATED_OWNER_AUTHORIZATION_2026_09_10"));
            assertTrue(sql.contains("revision=7"));
            assertFalse(sql.contains("INSERT platform.observation"),"semantic approval must never seed observation values");
        }
    }
}
