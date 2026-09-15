package org.base.api.service.platform;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
/** Compiles and validates a query against the approved contract before a family adapter executes it. */
@Service
public class ContractQueryPlanService {
 private final ContractMetadataService metadata;
 private final JdbcTemplate db;
 private final ContractRuntimeValidator validator;
 public ContractQueryPlanService(ContractMetadataService metadata,@Qualifier("primaryJdbcTemplate") JdbcTemplate db,ContractRuntimeValidator validator){this.metadata=metadata;this.db=db;this.validator=validator;}
 public ContractQueryRequest compileForPage(String contractCode,int pageId,ContractQueryRequest r){
   Map<String,Object> x=db.queryForMap("SELECT TOP 1 b.site_contract_revision_id,b.dataset_code FROM platform.contract_page_binding b JOIN platform.site_contract_revision r ON r.site_contract_revision_id=b.site_contract_revision_id WHERE r.contract_code=? AND r.status='APPROVED' AND b.runtime_page_id=? AND b.status='ACTIVE' ORDER BY r.revision DESC",contractCode,pageId);
   long revision=((Number)x.get("site_contract_revision_id")).longValue(); String dataset=String.valueOf(x.get("dataset_code")); validator.validate(revision,dataset); return compile(revision,dataset,r);
 }
 public ContractQueryRequest compile(long revisionId,String dataset,ContractQueryRequest r){
   Map<String,Object> m=metadata.metadata(revisionId,dataset); Set<String> fields=new HashSet<>();
   for(Object x:(List<?>)m.getOrDefault("fields",List.of())) fields.add(String.valueOf(((Map<?,?>)x).get("name")));
   Set<String> aliases=new HashSet<>(db.query("SELECT alias_code FROM platform.contract_query_alias WHERE site_contract_revision_id=? AND dataset_code=? AND status='APPROVED'",(rs,n)->rs.getString(1),revisionId,dataset));
   Set<String> includes=new HashSet<>(db.query("SELECT relation_code FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND (from_dataset_code=? OR to_dataset_code=?)",(rs,n)->rs.getString(1),revisionId,dataset,dataset));
   // Query operations are advertised by the approved metric registry, not by
   // a family name. COUNT remains the universal cardinality operation.
   Set<String> allowedAggregations=new LinkedHashSet<>(List.of("COUNT"));
   allowedAggregations.addAll(db.query("SELECT DISTINCT m.aggregation FROM platform.metric m JOIN platform.dataset d ON d.dataset_id=m.source_dataset_id JOIN platform.site_contract_dataset cd ON cd.dataset_code=d.dataset_code WHERE cd.site_contract_revision_id=? AND d.dataset_code=? AND m.status IN ('APPROVED','PROVISIONAL_APPROVED') AND m.aggregation IS NOT NULL ORDER BY m.aggregation",(rs,n)->rs.getString(1),revisionId,dataset));
   Map<String,Object> executableFilters=new LinkedHashMap<>();
   for(var e:r.filters().entrySet()) if(fields.contains(e.getKey()) || matchesAlias(aliases,e.getKey())) executableFilters.put(e.getKey(),e.getValue()); else throw new IllegalArgumentException("Field is not declared by contract: "+e.getKey());
   Set<String> compilerFields=new HashSet<>(fields);compilerFields.addAll(aliases);ContractQueryCompiler.compile(executableFilters,compilerFields,r.sort(),r.descending());
   if (r.orderBy() != null) for (Map<String,Object> order : r.orderBy()) {
     String field=String.valueOf(order.get("field"));
     if (!fields.contains(field) && !matchesAlias(aliases,field))
       throw new IllegalArgumentException("Sort field is not declared by contract: "+field);
     String direction=String.valueOf(order.getOrDefault("direction", "ASC")).toUpperCase(Locale.ROOT);
     if (!Set.of("ASC","DESC").contains(direction))
       throw new IllegalArgumentException("Sort direction must be ASC or DESC");
   }
   if(!r.where().isEmpty()) ContractQueryCompiler.compileWhere(r.where(),compilerFields);
   for(String g:r.groupBy()) if(!fields.contains(g) && !matchesAlias(aliases,g)) throw new IllegalArgumentException("Group field is not declared by contract: "+g);
   for(String s:r.select()) if(!fields.contains(s)) throw new IllegalArgumentException("Selected field is not declared by contract: "+s);
   for(String include:r.include()) if(!includes.contains(include)) throw new IllegalArgumentException("Include is not declared by contract: "+include);
   for(String include:r.includeLimits().keySet()) if(!includes.contains(include)) throw new IllegalArgumentException("Include limit targets an undeclared relation: "+include);
   if(r.aggregation()!=null&&!allowedAggregations.contains(r.aggregation().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Aggregation is not declared by contract: "+r.aggregation());
   return r;
 }
 private static boolean matchesAlias(Set<String> aliases,String key){return aliases.contains(key)||(aliases.contains("dimension.*")&&key.startsWith("dimension."));}
}
