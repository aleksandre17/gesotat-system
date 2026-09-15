package org.base.core.controller.api;

import org.base.core.entity.Permission;
import org.base.core.repository.PermissionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {
    private final PermissionRepository permissionRepository;

    public PermissionController(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public List<Permission> listPermissions(@RequestParam(required = false) Object id) {
        return permissionRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Permission> getPermission(@PathVariable Long id) {
        return permissionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public Permission createPermission(@RequestBody Permission permission) {
        return permissionRepository.save(permission);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission updatedPermission) {
        return permissionRepository.findById(id)
                .map(permission -> {
                    permission.setName(updatedPermission.getName());
                    return ResponseEntity.ok(permissionRepository.save(permission));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public ResponseEntity<Object> deletePermission(@PathVariable Long id) {
        return permissionRepository.findById(id)
                .map(permission -> {
                    permissionRepository.delete(permission);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
