package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class ContractCompatibilityService {
    private final JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();
    private static final String REVISION_SQL="SELECT revision,status,contract_checksum,contract_document_json FROM platform.site_contract_revision WHERE contract_code=? AND revision=?";
    public ContractCompatibilityService(@Qualifier("primaryJdbcTemplate") JdbcTemplate db){this.db=db;}
    public Map<String,Object> compare(String code,int from,int to){
        if(code==null||code.isBlank()||from<1||to<1||from==to) throw new IllegalArgumentException("Two distinct positive contract revisions are required");
        Map<String,Object> a=db.queryForMap(REVISION_SQL,code,from);
        Map<String,Object> b=db.queryForMap(REVISION_SQL,code,to);
        Long oldId=db.queryForObject("SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=? AND revision=?",Long.class,code,from); Long newId=db.queryForObject("SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=? AND revision=?",Long.class,code,to);
        Map<String,String> oldDatasets=datasets(oldId),newDatasets=datasets(newId);
        List<String> addedDatasets=new ArrayList<>(),removedDatasets=new ArrayList<>(),changedDatasets=new ArrayList<>();
        for(String d:newDatasets.keySet())if(!oldDatasets.containsKey(d))addedDatasets.add(d);for(String d:oldDatasets.keySet())if(!newDatasets.containsKey(d))removedDatasets.add(d);for(String d:oldDatasets.keySet())if(newDatasets.containsKey(d)&&!Objects.equals(oldDatasets.get(d),newDatasets.get(d)))changedDatasets.add(d);
        Map<String,String> oldFields=fields(oldId),newFields=fields(newId); List<String> added=new ArrayList<>(),removed=new ArrayList<>(),changed=new ArrayList<>();
        for(String f:newFields.keySet())if(!oldFields.containsKey(f))added.add(f);for(String f:oldFields.keySet())if(!newFields.containsKey(f))removed.add(f);for(String f:oldFields.keySet())if(newFields.containsKey(f)&&!Objects.equals(oldFields.get(f),newFields.get(f)))changed.add(f);
        Map<String,String> oldRelations=relations(oldId),newRelations=relations(newId);List<String> removedRelations=oldRelations.keySet().stream().filter(x->!newRelations.containsKey(x)).toList();List<String> addedRelations=newRelations.keySet().stream().filter(x->!oldRelations.containsKey(x)).toList();List<String> changedRelations=oldRelations.keySet().stream().filter(x->newRelations.containsKey(x)&&!Objects.equals(oldRelations.get(x),newRelations.get(x))).toList();
        List<String> breaking=new ArrayList<>();removed.forEach(x->breaking.add("REMOVED_FIELD:"+x));changed.forEach(x->breaking.add("CHANGED_FIELD_TYPE_REQUIRED_SEMANTICS:"+x));removedRelations.forEach(x->breaking.add("REMOVED_RELATION:"+x));changedRelations.forEach(x->breaking.add("CHANGED_RELATION_CARDINALITY_OR_ENDPOINT:"+x));removedDatasets.forEach(x->breaking.add("REMOVED_DATASET:"+x));changedDatasets.forEach(x->breaking.add("CHANGED_DATASET_FAMILY_OR_GRAIN:"+x));
        SemanticCompatibilityAnalyzer.Result semantic = semantic(a.get("contract_document_json"), b.get("contract_document_json"));
        semantic.breakingChanges().forEach(x -> breaking.add("SEMANTIC:" + x));
        boolean breakingChange=!breaking.isEmpty();
        boolean approvalRequired=breakingChange||!"APPROVED".equalsIgnoreCase(String.valueOf(b.get("status")))||!"APPROVED".equalsIgnoreCase(String.valueOf(a.get("status")));
        Map<String,Object> out=new LinkedHashMap<>();out.put("contractCode",code);out.put("fromRevision",from);out.put("toRevision",to);out.put("compatibility",breakingChange?"BREAKING":"BACKWARD_COMPATIBLE");out.put("breakingChanges",breaking);out.put("addedDatasets",addedDatasets);out.put("removedDatasets",removedDatasets);out.put("changedDatasets",changedDatasets);out.put("addedFields",added);out.put("removedFields",removed);out.put("changedFields",changed);out.put("addedRelations",addedRelations);out.put("removedRelations",removedRelations);out.put("changedRelations",changedRelations);out.put("semanticCompatibility",semantic.compatibility());out.put("semanticBreakingChanges",semantic.breakingChanges());out.put("semanticCompatibleChanges",semantic.compatibleChanges());out.put("semanticApprovalRequired",semantic.approvalRequired());out.put("semanticMigrationGuidance",semantic.migrationGuidance());out.put("fromStatus",a.get("status"));out.put("toStatus",b.get("status"));out.put("fromChecksum",a.get("contract_checksum"));out.put("toChecksum",b.get("contract_checksum"));out.put("checksumChanged",!Objects.equals(a.get("contract_checksum"),b.get("contract_checksum")));out.put("approvalRequired",approvalRequired||semantic.approvalRequired());return out;
    }
    private SemanticCompatibilityAnalyzer.Result semantic(Object oldDocument, Object newDocument) {
        try {
            return SemanticCompatibilityAnalyzer.compare(document(oldDocument), document(newDocument));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Contract semantic document is malformed", ex);
        }
    }
    private Map<String,Object> document(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return Map.of();
        if (value instanceof Map<?,?> map) {
            Map<String,Object> out = new LinkedHashMap<>();
            map.forEach((k,v) -> out.put(String.valueOf(k), v));
            return out;
        }
        try { return json.readValue(String.valueOf(value), new TypeReference<LinkedHashMap<String,Object>>() {}); }
        catch (Exception ex) { throw new IllegalArgumentException("contract_document_json is invalid", ex); }
    }
    private Map<String,String> datasets(long revision){return db.query("SELECT dataset_code,CONCAT(data_family,':',business_grain,':',natural_key_expression,':',row_role) FROM platform.site_contract_dataset WHERE site_contract_revision_id=?",(r,n)->Map.entry(r.getString(1),r.getString(2)),revision).stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(x,y)->x,LinkedHashMap::new));}
    private Map<String,String> fields(long revision){return db.query("SELECT CONCAT(d.dataset_code,'.',f.field_name),CONCAT(f.logical_type,':',CAST(f.required AS VARCHAR(1)),':',COALESCE(f.semantic_role,''),':',COALESCE(f.key_role,''),':',COALESCE(f.classifier_scheme_code,''),':',COALESCE(f.normalization_rule,''),':',CAST(f.ordinal AS VARCHAR(12)),':',COALESCE(f.source_expression,'')) FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id WHERE d.site_contract_revision_id=?",(r,n)->Map.entry(r.getString(1),r.getString(2)),revision).stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(x,y)->x,LinkedHashMap::new));}
    private Map<String,String> relations(long revision){return db.query("SELECT relation_code,CONCAT(from_dataset_code,'.',from_field_name,'>',to_dataset_code,'.',to_field_name,':',relation_kind,':',cardinality,':',enforcement_policy) FROM platform.site_contract_relation WHERE site_contract_revision_id=?",(r,n)->Map.entry(r.getString(1),r.getString(2)),revision).stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue,(x,y)->x,LinkedHashMap::new));}
}
