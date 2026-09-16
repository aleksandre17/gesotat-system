package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** Contract orchestration only; family semantics live behind the execution registry. */
@Service
public class CanonicalPageDataService {
    private final JdbcTemplate control;
    private final ContractMetadataService contractMetadata;
    private final ContractPolicyService contractPolicy;
    private final ContractPhysicalQueryService physical;
    private final ContractProjectionService projections;
    private final CursorTokenService cursors;
    private final ApprovedContractResolver defaultContracts;
    private final ContractPageExecutionRegistry execution;

    public CanonicalPageDataService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,
                                    @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,
                                    ContractMetadataService contractMetadata,
                                    ContractPolicyService contractPolicy,
                                    ContractPhysicalQueryService physical,
                                    ContractProjectionService projections,
                                    CursorTokenService cursors,
                                    ApprovedContractResolver defaultContracts,
                                    ContractPageExecutionRegistry execution) {
        this.control=control; this.contractMetadata=contractMetadata; this.contractPolicy=contractPolicy;
        this.physical=physical; this.projections=projections; this.cursors=cursors;
        this.defaultContracts=defaultContracts; this.execution=Objects.requireNonNull(execution,"execution registry");
    }

    public Map<String,Object> read(int pageId,int page,int limit,String metricCode,String carrierCode,String periodFrom,String periodTo,String ageGroup){
        String contract=defaultContracts.resolve(); if(contract==null) throw new IllegalStateException("No approved contract is available");
        return read(contract,pageId,page,limit,metricCode,carrierCode,periodFrom,periodTo,ageGroup,Map.of());
    }
    public Map<String,Object> read(String contractCode,int pageId,int page,int limit,String metricCode,String carrierCode,String periodFrom,String periodTo,String ageGroup){
        return read(contractCode,pageId,page,limit,metricCode,carrierCode,periodFrom,periodTo,ageGroup,Map.of(),Map.of());
    }
    public Map<String,Object> read(String contractCode,int pageId,int page,int limit,String metricCode,String carrierCode,String periodFrom,String periodTo,String ageGroup,Map<String,Object> filters){
        return read(contractCode,pageId,page,limit,metricCode,carrierCode,periodFrom,periodTo,ageGroup,filters,Map.of());
    }
    public Map<String,Object> read(String contractCode,int pageId,int page,int limit,String metricCode,String carrierCode,String periodFrom,String periodTo,String ageGroup,Map<String,Object> filters,Map<String,Object> where){
        page=Math.max(1,page); limit=Math.min(Math.max(1,limit),1000); Map<String,Object> meta=pageMetadata(contractCode,pageId); long rid=revisionId(meta);
        PageDataAdapterRegistry.ReadContext context=new PageDataAdapterRegistry.ReadContext(meta,page,limit,filters,where,metricCode,carrierCode,periodFrom,periodTo,ageGroup);
        ContractPageExecutionAdapter adapter=execution.require(PageFamily.fromDataFamily(meta.get("data_family")));
        List<Map<String,Object>> rows=adapter.read(context,rid,physical);
        Map<String,Object> out=new LinkedHashMap<>(); out.put("pageId",pageId); out.put("nodeCode",meta.get("node_code")); out.put("contractCode",meta.get("contract_code")); out.put("contractRevision",meta.get("revision")); out.put("datasetCode",meta.get("dataset_code")); out.put("path","/"+meta.get("path_segment")); out.put("data",rows);
        Map<String,Object> pagination=new LinkedHashMap<>(); pagination.put("page",page); pagination.put("limit",limit); pagination.put("returned",rows.size()); Long total=adapter.total(context,rid,physical); if(total!=null){boolean next=((long)page*limit)<total;pagination.put("total",total);pagination.put("hasNext",next);if(next)pagination.put("nextCursor",cursors.issue(String.valueOf(meta.get("contract_code")),pageId,page+1));} out.put("pagination",pagination);
        out.put("schema",Map.of("id","urn:geostat:"+String.valueOf(meta.get("contract_code")).toLowerCase(Locale.ROOT)+":"+String.valueOf(meta.get("dataset_code")).toLowerCase(Locale.ROOT)+":v"+meta.get("revision"),"version",String.valueOf(meta.get("revision")),"mediaType","application/json"));
        contractPolicy.requireGoverned(rid); if(meta.get("dataset_code")!=null)out.put("contractMetadata",contractMetadata.metadata(rid,String.valueOf(meta.get("dataset_code")))); out.put("governanceGates",contractPolicy.gates(rid));
        if(meta.get("response_projection_code")!=null){String p=String.valueOf(meta.get("response_projection_code"));projections.apply(out,p,rid);projections.applyRows(out,p,rid);}
        out.put("relations",meta.get("dataset_code")==null?List.of():control.query("SELECT relation_code FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND (from_dataset_code=? OR to_dataset_code=?) ORDER BY load_priority",(rs,n)->rs.getString(1),rid,String.valueOf(meta.get("dataset_code")),String.valueOf(meta.get("dataset_code"))));
        return out;
    }

    public Map<String,Object> readKeyset(String contractCode,int pageId,int limit,Map<String,Object> filters,List<String> sortKeys,List<Object> lastValues,boolean desc,String cursorSnapshot){
        Map<String,Object> meta=control.queryForMap("SELECT TOP 1 r.contract_code,r.revision,n.node_code,n.node_kind,n.dataset_code,n.path_segment FROM platform.contract_page_binding b JOIN platform.site_contract_revision r ON r.site_contract_revision_id=b.site_contract_revision_id JOIN platform.site_contract_node n ON n.node_id=b.node_id WHERE r.contract_code=? AND r.status='APPROVED' AND b.runtime_page_id=? AND b.status='ACTIVE' ORDER BY r.revision DESC",contractCode,pageId);
        if(cursorSnapshot!=null&&!cursorSnapshot.isBlank()&&!cursorSnapshot.equals(String.valueOf(meta.get("revision"))))throw new IllegalArgumentException("Keyset cursor contract revision is no longer current"); long rid=revisionId(meta); List<Map<String,Object>> rows=physical.rowsKeyset(rid,String.valueOf(meta.get("dataset_code")),limit,filters,sortKeys,lastValues,desc);
        Map<String,Object> out=new LinkedHashMap<>();out.put("pageId",pageId);out.put("nodeCode",meta.get("node_code"));out.put("contractCode",contractCode);out.put("contractRevision",meta.get("revision"));out.put("datasetCode",meta.get("dataset_code"));out.put("data",rows);out.put("pagination",Map.of("mode","KEYSET","limit",Math.min(Math.max(1,limit),1000),"returned",rows.size(),"direction",desc?"BACKWARD":"FORWARD"));contractPolicy.requireGoverned(rid);out.put("contractMetadata",contractMetadata.metadata(rid,String.valueOf(meta.get("dataset_code"))));return out;
    }

    public Map<String,Object> aggregate(String contractCode,int pageId,ContractQueryRequest request){
        Map<String,Object> meta=pageMetadata(contractCode,pageId);long rid=revisionId(meta);PageDataAdapterRegistry.ReadContext context=new PageDataAdapterRegistry.ReadContext(meta,1,1000,request.filters(),Map.of(),null,null,null,null,null);ContractPageExecutionAdapter adapter=execution.require(PageFamily.fromDataFamily(meta.get("data_family")));List<Map<String,Object>> rows=adapter.aggregate(context,rid,physical,request);
        Map<String,Object> out=new LinkedHashMap<>();out.put("pageId",pageId);out.put("contractCode",contractCode);out.put("contractRevision",meta.get("revision"));out.put("datasetCode",meta.get("dataset_code"));out.put("data",rows);out.put("groupBy",request.groupBy());out.put("aggregation",request.aggregation()==null?"COUNT":request.aggregation());contractPolicy.requireGoverned(rid);out.put("governanceGates",contractPolicy.gates(rid));if(meta.get("response_projection_code")!=null){String p=String.valueOf(meta.get("response_projection_code"));projections.apply(out,p,rid);projections.applyRows(out,p,rid);}return out;
    }

    /** Expands only relation codes already validated by the page's persisted contract plan. */
    @SuppressWarnings("unchecked")
    public List<Map<String,Object>> include(String contractCode,int pageId,List<Map<String,Object>> rows,List<String> includeCodes){
        Map<String,Object> meta=pageMetadata(contractCode,pageId); long rid=revisionId(meta);
        return physical.rowsWithIncludes(rid,String.valueOf(meta.get("dataset_code")),rows,includeCodes);
    }

    private Map<String,Object> pageMetadata(String contractCode,int pageId){return control.queryForMap("SELECT TOP 1 r.contract_code,r.revision,n.node_code,n.node_kind,n.dataset_code,d.data_family,n.path_segment,b.response_projection_code FROM platform.contract_page_binding b JOIN platform.site_contract_revision r ON r.site_contract_revision_id=b.site_contract_revision_id JOIN platform.site_contract_node n ON n.node_id=b.node_id LEFT JOIN platform.site_contract_dataset d ON d.site_contract_revision_id=r.site_contract_revision_id AND d.dataset_code=n.dataset_code WHERE r.contract_code=? AND b.runtime_page_id=? AND b.status='ACTIVE' AND r.status='APPROVED' ORDER BY r.revision DESC",contractCode,pageId);}
    private long revisionId(Map<String,Object> meta){Number id=(Number)control.query("SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=? AND revision=?",r->r.next()?r.getLong(1):null,meta.get("contract_code"),meta.get("revision"));if(id==null)throw new IllegalStateException("Approved contract revision is unavailable");return id.longValue();}
}
