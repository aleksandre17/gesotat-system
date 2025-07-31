package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.anotation.Sign;
import org.base.core.entity.Permission;
import org.base.core.model.request.SignRequest;
import org.base.core.model.response.SignResponse;
import org.base.core.model.request.RegisterRequest;
import org.base.core.model.response.TokenResponse;
import org.base.core.security.UserPrincipal;
import org.base.core.service.SignService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@Sign
@Api
@RequestMapping("/sign")
public class SignController {

    private final SignService service;

    @PostMapping("/register")
    public ResponseEntity<SignResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<SignResponse> createSignToken(
            @RequestBody
            @Valid SignRequest request
    ) {
        return ResponseEntity.ok(service.createSignToken(request));

    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestHeader("Authorization") String refreshToken) {
        return ResponseEntity.ok(service.refresh(refreshToken));
    }


    @GetMapping("/user")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Map<String, Object>> roles = userPrincipal.getUser().getRoles().stream()
                .map(role -> Map.of(
                        "name", role.getName(),
                        "permissions", role.getPermissions().stream()
                                .map(Permission::getName)
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