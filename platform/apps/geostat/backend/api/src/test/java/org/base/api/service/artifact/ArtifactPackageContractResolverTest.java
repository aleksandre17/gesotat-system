package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactPackageContractResolverTest {
    @Test
    void usesThePhysicalTableBoundToTheApprovedSiteDataset() {
        JdbcTemplate control = mock(JdbcTemplate.class);
        ArtifactPackageContractResolver.DatasetContract bound =
                new ArtifactPackageContractResolver.DatasetContract("SITE_A", 8, "a".repeat(64), 73,
                        "RESOURCE", "__ent_resource");
        when(control.query(contains("contract_table_definition t"), any(RowMapper.class),
                eq("SITE_A"), eq(8), eq("RESOURCE"))).thenReturn(List.of(bound));
        when(control.query(contains("site_contract_field f"), any(RowMapper.class),
                eq("SITE_A"), eq(8), eq("RESOURCE"))).thenReturn(List.of(
                new ArtifactPackageContractResolver.DeclaredField("resource_id", true, "NATURAL"),
                new ArtifactPackageContractResolver.DeclaredField("title", false, null)));

        var resolved = new ArtifactPackageContractResolver(control).resolve("SITE_A", 8, "RESOURCE");

        assertEquals("__ent_resource", resolved.accessTableName());
        assertEquals(List.of("resource_id"), resolved.fields());
        assertEquals(List.of("resource_id"), resolved.keyFields());
        verify(control).query(contains("t.table_definition_id=d.contract_table_definition_id"),
                any(RowMapper.class), eq("SITE_A"), eq(8), eq("RESOURCE"));
    }

    @Test
    void failsClosedWhenNoPhysicalDefinitionIsDirectlyBound() {
        JdbcTemplate control = mock(JdbcTemplate.class);
        when(control.query(contains("contract_table_definition t"), any(RowMapper.class),
                eq("SITE_A"), eq(8), eq("RESOURCE"))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new ArtifactPackageContractResolver(control).resolve("SITE_A", 8, "RESOURCE"));
    }
}
