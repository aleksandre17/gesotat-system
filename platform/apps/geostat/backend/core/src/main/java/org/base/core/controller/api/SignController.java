package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.anotation.Sign;
import org.base.core.entity.User;
import org.base.core.model.request.SignRequest;
import org.base.core.model.response.SignResponse;
import org.base.core.model.request.RegisterRequest;
import org.base.core.model.response.TokenResponse;
import org.base.core.security.UserPrincipal;
import org.base.core.service.SignService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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

        User user = userPrincipal.getUser();

        List<Map<String, Object>> roles = user.getRoles().stream()
                .map(role -> {
                    Map<String, Object> roleMap = new LinkedHashMap<>();
                    roleMap.put("id", role.getId());
                    roleMap.put("name", role.getName());
                    roleMap.put("permissions", role.getPermissions().stream()
                            .map(p -> Map.<String, Object>of("id", p.getId(), "name", p.getName()))
                            .collect(Collectors.toList()));
                    return roleMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserInfoResponse(
                user.getId(),
                user.getUsername(),
                roles
        ));
    }

    @Getter
    @AllArgsConstructor
    private static class UserInfoResponse {
        private final Long id;
        private final String username;
        private final List<Map<String, Object>> roles;
    }


}