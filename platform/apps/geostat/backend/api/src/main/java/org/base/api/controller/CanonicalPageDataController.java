package org.base.api.controller;

import org.base.api.service.platform.CanonicalPageDataService;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.base.api.service.platform.CursorTokenService;
import org.base.api.service.platform.ApprovedContractResolver;
import org.base.api.security.tenancy.TenantScoped;
import org.base.api.security.tenancy.TenantScopeExemption;

@TenantScoped
@Api
@RestController
@RequestMapping("/platform/pages")
public class CanonicalPageDataController {
    private final CanonicalPageDataService pages;
    private final ObjectMapper json;
    private final CursorTokenService cursors;
    private final ApprovedContractResolver contracts;
    public CanonicalPageDataController(CanonicalPageDataService pages,ObjectMapper json,CursorTokenService cursors,ApprovedContractResolver contracts){this.pages=pages;this.json=json;this.cursors=cursors;this.contracts=contracts;}
    @GetMapping(value="/{pageId}/data", produces=MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasAuthority('READ_RESOURCE')")
    @TenantScopeExemption(value = TenantScopeExemption.Kind.DEFAULT_CONTRACT, reason = "Without an explicit contractCode the controller serves the platform's approved default contract; the interceptor resolves and enforces that same contract.")
    public ResponseEntity<Map<String,Object>> data(@PathVariable int pageId,
        @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="100") int limit,
        @RequestParam(required=false) String metricCode, @RequestParam(required=false) String carrierCode,
        @RequestParam(required=false) String periodFrom, @RequestParam(required=false) String periodTo,
        @RequestParam(required=false) String ageGroup, @RequestParam(required=false) String resourceId,
        @RequestParam(required=false) String goalId, @RequestParam(required=false) String glossaryId,
        @RequestParam(required=false) String cursor, @RequestParam(required=false) String contractCode,
        @RequestHeader(value=HttpHeaders.IF_NONE_MATCH,required=false) String ifNoneMatch) {
        contractCode = (contractCode == null || contractCode.isBlank()) ? contracts.resolve() : contractCode;
        if (page < 1 || limit < 1 || limit > 1000 || pageId < 1 || contractCode == null || contractCode.isBlank()) {
            throw new IllegalArgumentException("pageId/contractCode are required and page must be >= 1 with limit between 1 and 1000");
        }
        if (cursor != null && !cursor.isBlank()) page = cursors.verify(cursor,contractCode,pageId);
        Map<String,Object> filters=new java.util.LinkedHashMap<>(); if(resourceId!=null)filters.put("resourceId",resourceId); if(goalId!=null)filters.put("goalId",goalId); if(glossaryId!=null)filters.put("glossaryId",glossaryId);
        Map<String,Object> body=pages.read(contractCode,pageId,page,limit,metricCode,carrierCode,periodFrom,periodTo,ageGroup,filters);
        String etag=etag(body); if(ifNoneMatch!=null && ifNoneMatch.replace("\"","").equals(etag)) return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).header(HttpHeaders.CACHE_CONTROL,"private, max-age=60, must-revalidate").header(HttpHeaders.VARY,"Origin, Accept, Accept-Language, Authorization").build();
        return ResponseEntity.ok().eTag(etag).header(HttpHeaders.CACHE_CONTROL,"private, max-age=60, must-revalidate").header(HttpHeaders.VARY,"Origin, Accept, Accept-Language, Authorization").body(body);
    }
    private String etag(Map<String,Object> body){try{return "\""+java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(body)))+"\"";}catch(Exception e){throw new IllegalStateException("Unable to calculate response ETag",e);}}
}
