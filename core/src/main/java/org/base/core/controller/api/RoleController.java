package org.base.core.controller.api;

import org.base.core.entity.Role;
import org.base.core.repository.PermissionRepository;
import org.base.core.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Role> getRole(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public Role createRole(@RequestBody Role role) {
        return roleRepository.save(role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role updatedRole) {
        return roleRepository.findById(id)
                .map(role -> {
                    role.setName(updatedRole.getName());
                    role.setPermissions(updatedRole.getPermissions());
                    return ResponseEntity.ok(roleRepository.save(role));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public ResponseEntity<Object> deleteRole(@PathVariable Long id) {
        return roleRepository.findById(id)
                .map(role -> {
                    roleRepository.delete(role);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
