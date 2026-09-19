package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.access.AuthoringFileService;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.platform.statistical.compiler.ContractDraftParser;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter;
import org.base.api.service.platform.statistical.ingest.StatisticalBindingService;
import org.base.api.service.platform.statistical.ingest.StatisticalLoadService;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.registry.JdbcStatisticalRegistry;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.JdbcContractStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Composition root of the common statistical contract. Everything deployment-specific is a property with a
 * safe default: supported profiles, required caption languages, the numeric envelope of the canonical store
 * and the single-person-team exception (off unless explicitly enabled).
 */
@Configuration
public class StatisticalContractConfiguration {

    @Bean
    StatisticalRegistry statisticalRegistry(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane, ObjectMapper mapper) {
        return new JdbcStatisticalRegistry(controlPlane, mapper);
    }

    /** Every authoring adapter contributes its own declaration; the first that knows the profile answers. */
    @Bean
    ProviderCapabilities.Catalog statisticalProviderCatalog() {
        List<ProviderCapabilities.Catalog> adapters = List.of(AccessAuthoringAdapter.catalog());
        return profile -> adapters.stream().map(a -> a.find(profile)).flatMap(Optional::stream).findFirst();
    }

    @Bean
    StatisticalContractCompiler statisticalContractCompiler(
            StatisticalRegistry registry, ProviderCapabilities.Catalog providers, ObjectMapper mapper,
            @Value("${platform.statistical-contract.supported-profiles:STAT_AGGREGATE}") Set<String> supportedProfiles,
            @Value("${platform.statistical-contract.required-caption-languages:ka}") Set<String> captionLanguages,
            @Value("${platform.statistical-contract.numeric.max-precision:28}") int maxPrecision,
            @Value("${platform.statistical-contract.numeric.max-scale:10}") int maxScale) {
        return new StatisticalContractCompiler(new ContractDraftParser(mapper), registry, providers,
                new StatisticalContractCompiler.Settings(supportedProfiles, new StatisticalContractCompiler.NumericEnvelope(maxPrecision, maxScale), captionLanguages));
    }

    /**
     * Labels come from the classification items the codelist version pins. The column per language is a closed
     * map, never caller text; an unmapped language shows the stable code itself.
     */
    @Bean
    AuthoringFileService.CodelistLabels statisticalCodelistLabels(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        Map<String, String> labelColumn = Map.of("ka", "label_ka", "en", "label_en");
        return (ref, language) -> controlPlane.query("""
                SELECT i.code, i.label_ka, i.label_en FROM platform.classification_item i
                JOIN platform.statistical_reference r ON r.target_type = 'CLASSIFICATION_VERSION' AND r.target_id = i.classification_version_id
                JOIN platform.contract_namespace n ON n.namespace_id = r.namespace_id
                WHERE i.status = 'ACTIVE' AND r.kind = 'CODELIST' AND n.namespace_code = ? AND r.code = ? AND r.version_major = ? AND r.version_minor = ? AND r.version_patch = ?
                ORDER BY COALESCE(i.sort_order, 2147483647), i.code""",
                (rs, i) -> {
                    String label = labelColumn.containsKey(language) ? rs.getString(labelColumn.get(language)) : null;
                    return new AccessAuthoringAdapter.CodeItem(rs.getString("code"), label == null || label.isBlank() ? rs.getString("code") : label);
                }, ref.namespace(), ref.code(), ref.version().major(), ref.version().minor(), ref.version().patch());
    }

    @Bean
    AuthoringFileService statisticalAuthoringFileService(ContractWorkflow workflow, StatisticalRegistry registry, AuthoringFileService.CodelistLabels labels,
            @Value("${platform.statistical-contract.status-concept-code:OBS_STATUS}") String statusConceptCode,
            @Value("${platform.statistical-contract.max-authoring-upload-bytes:262144000}") long maxUploadBytes) {
        return new AuthoringFileService(workflow, registry, labels, statusConceptCode, maxUploadBytes);
    }

    @Bean
    StatisticalBindingService statisticalBindingService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        return new StatisticalBindingService(controlPlane, new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(controlPlane.getDataSource()))));
    }

    /** Load and writer share one Data Plane transaction manager, so a load commits or rolls back as a whole. */
    @Bean
    StatisticalLoadService statisticalLoadService(ContractWorkflow workflow, ContractWorkflow.AccessDecision access, StatisticalRegistry registry,
            StatisticalBindingService bindings, ArtifactObjectStore objects, @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane, ObjectMapper mapper,
            @Value("${platform.statistical-contract.status-concept-code:OBS_STATUS}") String statusConceptCode,
            @Value("${platform.statistical-contract.max-authoring-upload-bytes:262144000}") long maxUploadBytes) {
        TransactionTemplate dataTransaction = new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(dataPlane.getDataSource())));
        return new StatisticalLoadService(workflow, access, registry, bindings, new CanonicalObservationWriter(dataPlane, dataTransaction, mapper), objects,
                dataPlane, dataTransaction, mapper, statusConceptCode, maxUploadBytes);
    }

    @Bean
    ReferenceRegistryService statisticalReferenceRegistryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane, StatisticalRegistry reader) {
        DataSourceTransactionManager transactions = new DataSourceTransactionManager(Objects.requireNonNull(controlPlane.getDataSource()));
        return new ReferenceRegistryService(controlPlane, new TransactionTemplate(transactions), reader);
    }

    @Bean
    ContractWorkflow.Store statisticalContractStore(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        DataSourceTransactionManager transactions = new DataSourceTransactionManager(Objects.requireNonNull(controlPlane.getDataSource()));
        return new JdbcContractStore(controlPlane, new TransactionTemplate(transactions));
    }

    @Bean
    ContractWorkflow statisticalContractWorkflow(ContractWorkflow.Store store, StatisticalContractCompiler compiler, ContractWorkflow.AccessDecision access,
                                                 @Value("${platform.statistical-contract.allow-self-approval:false}") boolean allowSelfApproval) {
        return new ContractWorkflow(store, compiler, access, new ContractWorkflow.Policy(allowSelfApproval), () -> UUID.randomUUID().toString());
    }
}
