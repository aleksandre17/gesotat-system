package org.base.api.service.platform.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Validates package declarations against its actual Access schema, without any write or target routing. */
@Service
public class SemanticAccessPackageValidationService {
    private static final Set<String> FAMILIES=Set.of("ENTITY","RELATION","STATISTICAL","GEO","REFERENCE","RAW");
    private static final Set<String> PROJECTION_FAMILIES=Set.of("ENTITY","RELATION","STATISTICAL","STATISTICAL_WIDE_JSON","GEO","REFERENCE","RAW");
    private static final Set<String> LOGICAL_TYPES=Set.of("STRING","TEXT","INTEGER","DECIMAL","BOOLEAN","DATE","DATETIME","JSON","URI","CODE","GEOJSON");
    private static final Set<String> SEMANTIC_ROLES=Set.of("IDENTIFIER","NATURAL_KEY","FOREIGN_KEY","DIMENSION","MEASURE","PERIOD","LOCALIZED_TEXT","LOCATOR","CLASSIFICATION","GEOMETRY","CONTENT","ATTRIBUTE","RAW_PAYLOAD","PROVENANCE","GOVERNANCE","QUALITY","LINEAGE");
    private static final Set<String> KEY_ROLES=Set.of("PRIMARY","UNIQUE","NATURAL","FOREIGN");
    private static final Set<String> CARDINALITIES=Set.of("ONE_TO_ONE","ONE_TO_MANY","MANY_TO_ONE","MANY_TO_MANY");
    private final ObjectMapper json;
    public SemanticAccessPackageValidationService(ObjectMapper json) { this.json=json; }

    public SemanticAccessPreview validate(File accessFile, SemanticAccessPackage pack) {
        List<SemanticAccessIssue> issues=new ArrayList<>();
        try(Database database= new DatabaseBuilder(accessFile).setReadOnly(true).open()) {
            Map<String,Table> sourceTables=new HashMap<>();
            for(String name:database.getTableNames()) sourceTables.put(name.toLowerCase(Locale.ROOT),database.getTable(name));
            Map<String,SemanticAccessDataset> datasets=new HashMap<>();
            Map<String,Set<String>> fields=new HashMap<>();
            for(SemanticAccessDataset dataset:pack.datasets()) {
                String code=key(dataset.datasetCode());
                if(datasets.putIfAbsent(code,dataset)!=null) issue(issues,"DUPLICATE_DATASET_CODE",dataset.datasetCode(),"dataset_code must be unique");
                if(!FAMILIES.contains(dataset.dataFamily().toUpperCase(Locale.ROOT))) issue(issues,"UNSUPPORTED_DATA_FAMILY",dataset.datasetCode(),"Unsupported data family: "+dataset.dataFamily());
                Table source=sourceTables.get(key(dataset.accessTableName()));
                if(source==null || dataset.accessTableName().startsWith("__gs_")) issue(issues,"MISSING_SOURCE_TABLE",dataset.datasetCode(),"Declared source table is absent or technical: "+dataset.accessTableName());
                fields.put(code,new HashSet<>());
            }
            for(SemanticAccessField field:pack.fields()) {
                Set<String> declared=fields.get(key(field.datasetCode()));
                if(declared==null) { issue(issues,"UNKNOWN_FIELD_DATASET",field.datasetCode(),"Field refers to an undeclared dataset"); continue; }
                if(!declared.add(key(field.fieldName()))) issue(issues,"DUPLICATE_FIELD",field.datasetCode(),"Field is declared more than once: "+field.fieldName());
                SemanticAccessDataset dataset=datasets.get(key(field.datasetCode()));
                Table source=dataset==null?null:sourceTables.get(key(dataset.accessTableName()));
                if(source!=null && source.getColumns().stream().noneMatch(column->column.getName().equalsIgnoreCase(field.fieldName()))) issue(issues,"MISSING_SOURCE_COLUMN",field.datasetCode(),"Declared field is absent: "+field.fieldName());
                if(!LOGICAL_TYPES.contains(field.logicalType().toUpperCase(Locale.ROOT))) issue(issues,"UNSUPPORTED_LOGICAL_TYPE",field.datasetCode(),"Unsupported logical_type: "+field.logicalType());
                if(!SEMANTIC_ROLES.contains(field.semanticRole().toUpperCase(Locale.ROOT))) issue(issues,"UNSUPPORTED_SEMANTIC_ROLE",field.datasetCode(),"Unsupported semantic_role: "+field.semanticRole());
            }
            for(SemanticAccessDataset dataset:pack.datasets()) {
                Table source=sourceTables.get(key(dataset.accessTableName()));
                if(source==null) continue;
                Set<String> declared=fields.get(key(dataset.datasetCode()));
                for(var column:source.getColumns()) if(!declared.contains(key(column.getName()))) issue(issues,"UNDECLARED_SOURCE_COLUMN",dataset.datasetCode(),"Source column is not declared: "+column.getName());
            }
            Set<String> keyedFields=new HashSet<>();
            for(SemanticAccessKey key:pack.keys()) {
                if(!hasField(fields,key.datasetCode(),key.fieldName())) issue(issues,"INVALID_KEY_FIELD",key.datasetCode(),"Key field is not declared: "+key.fieldName());
                if(!KEY_ROLES.contains(key.keyRole().toUpperCase(Locale.ROOT))) issue(issues,"UNSUPPORTED_KEY_ROLE",key.datasetCode(),"Unsupported key_role: "+key.keyRole());
                if(key.keyOrder()<1) issue(issues,"INVALID_KEY_ORDER",key.datasetCode(),"key_order must be positive");
                if(!keyedFields.add(key(key.datasetCode())+"|"+key(key.keyRole())+"|"+key.keyOrder())) issue(issues,"DUPLICATE_KEY_ORDER",key.datasetCode(),"key_role/key_order must be unique within a dataset");
            }
            for(SemanticAccessRelation relation:pack.relations()) {
                if(!CARDINALITIES.contains(relation.cardinality().toUpperCase(Locale.ROOT))) issue(issues,"UNSUPPORTED_CARDINALITY",relation.relationshipCode(),"Unsupported cardinality: "+relation.cardinality());
                if(!hasField(fields,relation.fromDatasetCode(),relation.fromField())||!hasField(fields,relation.toDatasetCode(),relation.toField())) issue(issues,"INVALID_RELATION_ENDPOINT",relation.relationshipCode(),"Relation endpoint must reference declared dataset fields");
                if(!isReferenceableKey(pack.keys(),relation.toDatasetCode(),relation.toField())) issue(issues,"UNKEYED_RELATION_TARGET",relation.relationshipCode(),"Relation target must be declared as PRIMARY, UNIQUE, or NATURAL key");
            }
            Set<String> projectionCodes=new HashSet<>();
            Map<String,SemanticAccessProjection> projections=new HashMap<>();
            for(SemanticAccessProjection projection:pack.projections()) {
                if(!projectionCodes.add(key(projection.projectionCode()))) issue(issues,"DUPLICATE_PROJECTION_CODE",projection.projectionCode(),"projection_code must be unique");
                projections.putIfAbsent(key(projection.projectionCode()),projection);
                if(!datasets.containsKey(key(projection.datasetCode()))) issue(issues,"UNKNOWN_PROJECTION_DATASET",projection.projectionCode(),"Projection dataset is not declared");
                if(!PROJECTION_FAMILIES.contains(projection.projectionFamily().toUpperCase(Locale.ROOT))) issue(issues,"UNSUPPORTED_PROJECTION_FAMILY",projection.projectionCode(),"Unsupported projection_family: "+projection.projectionFamily());
                if(!Set.of("DRAFT","READY").contains(projection.approvalState().toUpperCase(Locale.ROOT))) issue(issues,"INVALID_APPROVAL_STATE",projection.projectionCode(),"approval_state must be DRAFT or READY");
                try { if(!json.readTree(projection.mappingJson()).isObject()) issue(issues,"INVALID_MAPPING_JSON",projection.projectionCode(),"mapping_json must be a JSON object"); }
                catch(Exception error) { issue(issues,"INVALID_MAPPING_JSON",projection.projectionCode(),"mapping_json is invalid JSON"); }
            }
            Set<String> bindingKeys=new HashSet<>();
            for(SemanticAccessStatisticalBinding binding:pack.statisticalBindings()) {
                String subject=binding.sourceDatasetCode()+"/"+binding.sourceExternalKey();
                SemanticAccessDataset dataset=datasets.get(key(binding.sourceDatasetCode()));
                if(dataset==null) issue(issues,"UNKNOWN_STATISTICAL_BINDING_DATASET",subject,"Binding source dataset is not declared");
                if(binding.sourceExternalKey().isBlank()) issue(issues,"BLANK_STATISTICAL_BINDING_KEY",subject,"Binding source key must not be blank");
                SemanticAccessProjection projection=projections.get(key(binding.projectionCode()));
                if(projection==null) issue(issues,"UNKNOWN_STATISTICAL_BINDING_PROJECTION",subject,"Binding projection is not declared");
                else {
                    if(!"STATISTICAL_WIDE_JSON".equalsIgnoreCase(projection.projectionFamily())) issue(issues,"INVALID_STATISTICAL_BINDING_PROJECTION",subject,"Binding must use a STATISTICAL_WIDE_JSON projection");
                    if(dataset!=null&&!projection.datasetCode().equalsIgnoreCase(dataset.datasetCode())) issue(issues,"STATISTICAL_BINDING_DATASET_MISMATCH",subject,"Binding dataset and projection dataset differ");
                    if("DRAFT".equalsIgnoreCase(projection.approvalState())&&"READY".equalsIgnoreCase(binding.bindingState())) issue(issues,"STATISTICAL_BINDING_PREMATURE_READY",subject,"A binding cannot be READY when its projection is DRAFT");
                }
                if(!Set.of("DRAFT","READY").contains(binding.bindingState().toUpperCase(Locale.ROOT))) issue(issues,"INVALID_STATISTICAL_BINDING_STATE",subject,"binding_state must be DRAFT or READY");
                if(binding.dataflowCode().isBlank()||binding.evidenceKind().isBlank()) issue(issues,"INCOMPLETE_STATISTICAL_BINDING",subject,"dataflow_code and evidence_kind are required");
                if(!bindingKeys.add(key(binding.sourceDatasetCode())+"|"+key(binding.sourceExternalKey())+"|"+key(binding.projectionCode()))) issue(issues,"DUPLICATE_STATISTICAL_BINDING",subject,"Only one binding per source carrier and projection is allowed");
            }
            Set<String> schemaKeys=new HashSet<>(); for(SemanticAccessMetadataSchema s:pack.metadataSchemas()) {
                if(!schemaKeys.add(key(s.namespaceCode())+"|"+key(s.schemaCode())+"|"+s.revision())) issue(issues,"DUPLICATE_METADATA_SCHEMA",s.schemaCode(),"Metadata schema identity must be unique");
                if(s.revision()<1||!Set.of("DRAFT","READY","APPROVED").contains(s.approvalState().toUpperCase(Locale.ROOT))) issue(issues,"INVALID_METADATA_SCHEMA_STATE",s.schemaCode(),"Invalid metadata schema state");
                try {if(!json.readTree(s.schemaJson()).isObject()) issue(issues,"INVALID_METADATA_SCHEMA_JSON",s.schemaCode(),"schema_json must be an object");}catch(Exception e){issue(issues,"INVALID_METADATA_SCHEMA_JSON",s.schemaCode(),"schema_json is invalid JSON");}
            }
            Set<String> assertionKeys=new HashSet<>(); Set<String> types=Set.of("TEXT","NUMBER","BOOLEAN","JSON","URI","DATE","DATETIME","CODE");
            for(SemanticAccessMetadataAssertion a:pack.metadataAssertions()) {
                String k=key(a.subjectType())+"|"+key(a.subjectCode())+"|"+a.subjectRevision()+"|"+key(a.namespaceCode())+"|"+key(a.propertyCode())+"|"+String.valueOf(a.languageTag())+"|"+a.ordinal();
                if(!assertionKeys.add(k)) issue(issues,"DUPLICATE_METADATA_ASSERTION",a.subjectCode(),"Metadata assertion identity must be unique");
                if(a.ordinal()<1||!types.contains(a.valueType().toUpperCase(Locale.ROOT))) issue(issues,"INVALID_METADATA_VALUE_TYPE",a.propertyCode(),"Unsupported metadata value type or ordinal");
                if("JSON".equalsIgnoreCase(a.valueType())&&a.valueJson()!=null) try{json.readTree(a.valueJson());}catch(Exception e){issue(issues,"INVALID_METADATA_VALUE_JSON",a.propertyCode(),"value_json is invalid JSON");}
            }
        } catch(Exception error) { issue(issues,"ACCESS_SCHEMA_READ_FAILURE",pack.packageCode(),error.getMessage()); }
        return new SemanticAccessPreview(pack.productCode(),pack.packageCode(),pack.packageVersion(),issues.isEmpty(),List.copyOf(issues),pack.datasets().size(),pack.fields().size(),pack.relations().size(),pack.projections().size());
    }
    private static boolean hasField(Map<String,Set<String>> fields,String dataset,String field){Set<String> declared=fields.get(key(dataset));return declared!=null&&declared.contains(key(field));}
    private static boolean isReferenceableKey(List<SemanticAccessKey> keys,String dataset,String field){return keys.stream().anyMatch(key->key(key.datasetCode()).equals(key(dataset))&&key(key.fieldName()).equals(key(field))&&Set.of("PRIMARY","UNIQUE","NATURAL").contains(key.keyRole().toUpperCase(Locale.ROOT)));}
    private static String key(String value){return value==null?"":value.toLowerCase(Locale.ROOT);}
    private static void issue(List<SemanticAccessIssue> issues,String code,String subject,String message){issues.add(new SemanticAccessIssue(code,subject,message));}
}
