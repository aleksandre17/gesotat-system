package org.base.api.controller;

import org.base.api.service.platform.PlatformAsyncOperationService;
import org.base.api.service.platform.ApprovedContractResolver;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Api @RestController @RequestMapping("/platform/operations")
public class PlatformAsyncOperationController {
    private final PlatformAsyncOperationService operations;
    private final ApprovedContractResolver contracts;
    public PlatformAsyncOperationController(PlatformAsyncOperationService operations,ApprovedContractResolver contracts){this.operations=operations;this.contracts=contracts;}
    @PostMapping("/exports") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> submit(@RequestParam(required=false) String contractCode,@RequestParam int pageId,@RequestHeader(value="Idempotency-Key",required=false) String idempotencyKey,@RequestBody Map<String,Object> request,Authentication authentication){contractCode=(contractCode==null||contractCode.isBlank())?contracts.resolve():contractCode;if(contractCode==null||contractCode.isBlank())throw new IllegalStateException("No approved contract is available");return ResponseEntity.accepted().body(operations.submit(contractCode,pageId,idempotencyKey,request,actor(authentication)));}
    @GetMapping("/{operationId}") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> status(@PathVariable long operationId,Authentication authentication){return ResponseEntity.ok(operations.status(operationId,actor(authentication),admin(authentication)));}
    @PostMapping("/{operationId}/cancel") @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Map<String,Object>> cancel(@PathVariable long operationId,Authentication authentication){operations.cancel(operationId,actor(authentication),admin(authentication));return ResponseEntity.ok(operations.status(operationId,actor(authentication),admin(authentication)));}
    private static String actor(Authentication a){return a==null||a.getName()==null||a.getName().isBlank()?"unknown":a.getName().substring(0,Math.min(256,a.getName().length()));}
    private static boolean admin(Authentication a){return a!=null&&a.getAuthorities().stream().anyMatch(x->"ADMIN".equals(x.getAuthority()));}
}
