package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.model.ProfileModel;
import org.base.core.model.request.ChangePasswordRequest;
import org.base.core.model.request.UpdateProfileRequest;
import org.base.core.model.request.UserRequest;
import org.base.core.model.response.UserResponse;
import org.base.core.security.UserPrincipal;
import org.base.core.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public List<UserResponse> listUsers() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public UserResponse createUser(@RequestBody @Valid UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public UserResponse updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileModel> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getUser().getId()));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileModel> updateProfile(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUser().getId(), req));
    }

    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(principal.getUser().getId(), req);
        return ResponseEntity.noContent().build();
    }
}
