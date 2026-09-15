package org.base.api.controller;

import org.base.api.service.platform.*;
import org.base.core.anotation.Api;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** SDMX-shaped compatibility facade backed by the governed contract/page engine. */
@Api @RestController @RequestMapping("/sdmx")
public class SdmxCompatibilityController {
    private final JdbcTemplate control; private final CanonicalPageDataService pages; private final ContractIntrospectionService introspection; private final ApprovedContractResolver contracts;
    public SdmxCompatibilityController(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,CanonicalPageDataService pages,ContractIntrospectionService introspection,ApprovedContractResolver contracts){this.control=control;this.pages=pages;this.introspection=introspection;this.contracts=contracts;}
    @GetMapping("/data/{flow}/{key}") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> data(@PathVariable String flow,@PathVariable String key,@RequestParam(required=false) String contractCode,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="100") int limit){
        contractCode=resolveContract(contractCode);
        Map<String,Object> binding=control.queryForMap("SELECT TOP 1 r.revision,r.contract_code,b.runtime_page_id FROM platform.site_contract_revision r JOIN platform.contract_page_binding b ON b.site_contract_revision_id=r.site_contract_revision_id JOIN platform.site_contract_node n ON n.node_id=b.node_id WHERE r.contract_code=? AND r.status='APPROVED' AND b.status='ACTIVE' AND n.dataset_code=? ORDER BY r.revision DESC",contractCode,flow);
        Map<String,Object> filters=new LinkedHashMap<>(); String[] parts=key==null?new String[0]:key.split("\\.");
        List<String> dimensions=control.query("SELECT c.component_code FROM platform.statistical_dataflow f JOIN platform.statistical_dsd d ON d.dataflow_id=f.dataflow_id JOIN platform.statistical_component c ON c.dsd_id=d.dsd_id WHERE f.dataflow_code=? AND d.status IN ('APPROVED','PROVISIONAL_APPROVED') AND c.component_role='DIMENSION' AND c.status IN ('APPROVED','PROVISIONAL_APPROVED') ORDER BY c.component_order",(rs,n)->rs.getString(1),flow);
        for(int i=0;i<parts.length && i<dimensions.size();i++) if(!parts[i].equals("*")) filters.put("dimension."+dimensions.get(i),parts[i]);
        if(parts.length>dimensions.size() && dimensions.isEmpty()) throw new IllegalArgumentException("SDMX key has no declared dimensions for dataflow: "+flow);
        Map<String,Object> result=pages.read(contractCode,((Number)binding.get("runtime_page_id")).intValue(),page,limit,null,null,null,null,null,filters);
        return ResponseEntity.ok(Map.of("data",result.get("data"),"meta",Map.of("contractCode",contractCode,"contractRevision",binding.get("revision"),"flow",flow,"key",key),"pagination",result.get("pagination"),"structure",result.getOrDefault("contractMetadata",Map.of())));
    }
    @GetMapping("/dataflow/{flow}") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> dataflow(@PathVariable String flow,@RequestParam(required=false) String contractCode){return ResponseEntity.ok(flowStructure(resolveContract(contractCode),flow));}
    @GetMapping("/datastructure/{dataset}") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> structure(@PathVariable String dataset,@RequestParam(required=false) String contractCode){Map<String,Object> c=introspection.contract(resolveContract(contractCode),null);return ResponseEntity.ok(Map.of("dataset",dataset,"contract",c));}
    @GetMapping("/codelist/{scheme}") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> codelist(@PathVariable String scheme,@RequestParam(required=false) String contractCode){contractCode=resolveContract(contractCode);Map<String,Object> binding=control.queryForMap("SELECT TOP 1 b.runtime_page_id FROM platform.site_contract_revision r JOIN platform.contract_page_binding b ON b.site_contract_revision_id=r.site_contract_revision_id JOIN platform.site_contract_node n ON n.node_id=b.node_id WHERE r.contract_code=? AND r.status='APPROVED' AND b.status='ACTIVE' AND n.node_kind='REFERENCE_REGISTRY' ORDER BY r.revision DESC,b.runtime_page_id",contractCode);int pageId=((Number)binding.get("runtime_page_id")).intValue();Map<String,Object> p=pages.read(contractCode,pageId,1,1000,null,null,null,null,null,Map.of("schemeCode",scheme));return ResponseEntity.ok(Map.of("scheme",scheme,"items",p.get("data"),"pagination",p.get("pagination")));}
    private String resolveContract(String value){String resolved=(value==null||value.isBlank())?contracts.resolve():value;if(resolved==null||resolved.isBlank())throw new IllegalStateException("No approved contract is available");return resolved;}
    private Map<String,Object> flowStructure(String code,String flow){Map<String,Object> c=introspection.contract(code,null);return Map.of("dataflowCode",flow,"contractCode",code,"revision",c.get("revision"),"status",c.get("status"),"pages",c.get("pages"));}
}
