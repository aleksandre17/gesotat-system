package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.anotation.Sign;
import org.base.core.model.request.SignRequest;
import org.base.core.model.response.SignResponse;
import org.base.core.model.request.RegisterRequest;
import org.base.core.service.SignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}