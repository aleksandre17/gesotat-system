package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
@Api
public class ResponseController {

    @PostMapping()
    public ResponseEntity<String> register() {
        return ResponseEntity.ok("HI");
    }
}
