package org.base.core.controller.api;

import jakarta.transaction.Transactional;
import lombok.Getter;
import org.base.core.entity.User;
import org.base.core.repository.RoleRepository;
import org.base.core.repository.UserRepository;
import org.base.core.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    // This class can be used to handle user-related API endpoints
    // For example, you can add methods to get user details, update user information, etc.

    // Example method to get user details
    // @GetMapping("/{id}")
    // public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
    //     UserResponse user = userService.getUserById(id);
    //     return ResponseEntity.ok(user);
    // }

    // Add more methods as needed for user management

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    @Transactional
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(updatedUser.getUsername());
                    if (updatedUser.getPassword() != null) {
                        user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    }
                    //user.setEmail(updatedUser.getEmail());
                    //user.setRoles(updatedUser.getRoles());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Map<String, Object>> roles = userPrincipal.getUser().getRoles().stream()
                .map(role -> Map.of(
                        "name", role.getName(),
                        "permissions", role.getPermissions().stream()
                                .map(permission -> permission.getName())
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userPrincipal.getUsername(), roles);
        return ResponseEntity.ok(response);
    }

    @Getter
    private static class UserInfoResponse {
        private final String username;
        private final List<Map<String, Object>> roles;

        public UserInfoResponse(String username, List<Map<String, Object>> roles) {
            this.username = username;
            this.roles = roles;
        }

    }
}
