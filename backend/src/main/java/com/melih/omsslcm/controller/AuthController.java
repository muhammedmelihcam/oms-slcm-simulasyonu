package com.melih.omsslcm.controller;

import com.melih.omsslcm.dto.AuthResponseDto;
import com.melih.omsslcm.dto.SigninRequest;
import com.melih.omsslcm.dto.SignupRequest;
import com.melih.omsslcm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Signup and signin")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/signin")
    @Operation(summary = "Sign in and receive a bearer token")
    public AuthResponseDto signin(@Valid @RequestBody SigninRequest request) {
        String token = authService.signin(request.email(), request.password());
        return new AuthResponseDto(token);
    }
}
