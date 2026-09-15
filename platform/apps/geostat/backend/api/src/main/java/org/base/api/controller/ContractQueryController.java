package org.base.api.controller;
import org.base.api.service.platform.*;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.security.MessageDigest;
import java.util.*;
@Api @RestController @RequestMapping("/platform/contracts")
public class ContractQueryController {
 private final ContractQueryPlanService plans; private final CanonicalPageDataService pages; private final ObjectMapper json; private final CursorTokenService cursors; private final QueryAdmissionBudget admission;
 public ContractQueryController(ContractQueryPlanService plans,CanonicalPageDataService pages,ObjectMapper json,CursorTokenService cursors,QueryAdmissionBudget admission){this.plans=plans;this.pages=pages;this.json=json;this.cursors=cursors;this.admission=admission;}
 @PostMapping(value="/{contractCode}/pages/{pageId}/query", produces=MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('READ_RESOURCE')")
 public ResponseEntity<Map<String,Object>> query(@PathVariable String contractCode,@PathVariable int pageId,@RequestBody ContractQueryRequest request,@RequestHeader(value=HttpHeaders.IF_NONE_MATCH,required=false) String ifNoneMatch){
   int p=request.page()==null?1:request.page(), l=request.limit()==null?100:request.limit();
   validateBudget(request,p,l);
   // Every execution mode, including a resumed keyset cursor, must pass the
   // approved contract compiler before any data-plane access.  This prevents
   // cursor requests from bypassing field/aggregation/relation declarations.
   plans.compileForPage(contractCode,pageId,request);
   long queryCost=admission.cost(request,l); admission.admit(request,l);
   List<String> keysetKeys=sortKeys(request); String fingerprint=fingerprint(request, keysetKeys);
   if(request.cursor()!=null && request.cursor().startsWith("KS.")){ CursorTokenService.KeysetState state=cursors.verifyKeyset(request.cursor(),contractCode,pageId,fingerprint); Map<String,Object> keyset=pages.readKeyset(contractCode,pageId,l,request.filters(),keysetKeys,state.values(),state.descending(),state.snapshot()); addNextKeyset(keyset,contractCode,pageId,fingerprint,keysetKeys,state.descending()); return respond(keyset,ifNoneMatch,queryCost); }
   if(request.cursor()!=null && !request.cursor().isBlank()) p=cursors.verify(request.cursor(),contractCode,pageId);
   if(request.aggregation()!=null || !request.groupBy().isEmpty()) return respond(pages.aggregate(contractCode,pageId,request),ifNoneMatch,queryCost);
   Map<String,Object> result=pages.read(contractCode,pageId,p,l,
      text(request.filters(),"metricCode"),text(request.filters(),"carrierCode"),text(request.filters(),"periodFrom"),text(request.filters(),"periodTo"),text(request.filters(),"ageGroup"),request.filters(),request.where());
   if(!request.select().isEmpty()) result.put("data",ContractResponseSerializer.select((List<Map<String,Object>>)result.get("data"),request.select()));
   if(!request.include().isEmpty()) result.put("data",ContractIncludeSerializer.apply((List<Map<String,Object>>)result.get("data"),request.include()));
   if(request.distinct()) result.put("data",ContractResponseSerializer.distinct((List<Map<String,Object>>)result.get("data")));
   if(!request.includeLimits().isEmpty()) result.put("data",ContractIncludeSerializer.limit((List<Map<String,Object>>)result.get("data"),request.includeLimits()));
   if(!keysetKeys.isEmpty() && request.sort()!=null && !request.sort().isBlank()) addNextKeyset(result,contractCode,pageId,fingerprint,keysetKeys,request.descending());
   return respond(result,ifNoneMatch,queryCost);
 }
 private static List<String> sortKeys(ContractQueryRequest r){if(r.orderBy()!=null&&!r.orderBy().isEmpty())return r.orderBy().stream().map(x->String.valueOf(x.get("field"))).toList();return r.sort()==null||r.sort().isBlank()?List.of("id"):List.of(r.sort());}
 private static String fingerprint(ContractQueryRequest r,List<String> keys){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((r.filters().toString()+r.where().toString()+keys.toString()+r.descending()+r.include().toString()+r.select().toString()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 private void addNextKeyset(Map<String,Object> body,String contract,int pageId,String fp,List<String> keys,boolean desc){Object data=body.get("data");if(data instanceof List<?> list&&!list.isEmpty()&&list.get(list.size()-1) instanceof Map<?,?> m){List<Object> vals=new ArrayList<>();for(String k:keys)vals.add((Object)m.get(k));body.put("nextCursor",cursors.issueKeyset(contract,pageId,String.valueOf(body.get("contractRevision")),fp,keys,vals,desc));}}
 private static String text(Map<String,Object> m,String k){Object v=m.get(k);return v==null?null:String.valueOf(v);}
 private static void validateBudget(ContractQueryRequest r,int page,int limit){
   if(page<1||limit<1||limit>1000) throw new IllegalArgumentException("page must be >= 1 and limit must be between 1 and 1000");
   if(r.groupBy().size()>32||r.select().size()>128||r.include().size()>32||r.orderBy().size()>8||r.includeLimits().size()>32||r.filters().size()>64) throw new IllegalArgumentException("Query exceeds contract execution budget");
   int[] whereBudget=treeBudget(r.where());
   if(whereBudget[0]>8||whereBudget[1]>128) throw new IllegalArgumentException("Query predicate exceeds contract execution budget");
   for(var e:r.includeLimits().entrySet()) if(e.getKey()==null||e.getKey().isBlank()||e.getValue()==null||e.getValue()<1||e.getValue()>1000) throw new IllegalArgumentException("include limit must be between 1 and 1000");
 }
 private static int[] treeBudget(Object root){
   if(root==null)return new int[]{0,0};
   int maxDepth=0,nodes=0; Deque<Object[]> stack=new ArrayDeque<>(); stack.push(new Object[]{root,0});
   while(!stack.isEmpty()){Object[] item=stack.pop(); Object value=item[0]; int depth=(Integer)item[1]; nodes++; maxDepth=Math.max(maxDepth,depth); if(nodes>129)return new int[]{maxDepth,nodes};
    if(value instanceof Map<?,?> map) for(var e:map.entrySet()){stack.push(new Object[]{e.getKey(),depth+1});stack.push(new Object[]{e.getValue(),depth+1});}
    else if(value instanceof Iterable<?> it) for(Object child:it)stack.push(new Object[]{child,depth+1});
   } return new int[]{maxDepth,nodes};
 }
 private ResponseEntity<Map<String,Object>> respond(Map<String,Object> body,String ifNoneMatch,long queryCost){try{String tag="\""+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(body)))+"\"";var builder=ResponseEntity.status(ifNoneMatch!=null&&ifNoneMatch.replace("\"","").equals(tag.replace("\"", ""))?HttpStatus.NOT_MODIFIED:HttpStatus.OK).eTag(tag).header(HttpHeaders.CACHE_CONTROL,"private, max-age=60, must-revalidate").header(HttpHeaders.VARY,"Origin, Accept, Accept-Language, Authorization").header("X-Query-Cost",String.valueOf(queryCost)).header("X-Query-Cost-Limit",String.valueOf(admission.maximumCost()));if(ifNoneMatch!=null&&ifNoneMatch.replace("\"","").equals(tag.replace("\"", "")))return builder.build();return builder.body(body);}catch(Exception e){throw new IllegalStateException("Unable to calculate response ETag",e);}}
}
