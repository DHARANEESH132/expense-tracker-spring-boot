package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.AuthDto.*;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, profile, and password operations")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Retrieve authenticated user profile")
    public ResponseEntity<ApiResponse<Map<String, UserSummary>>> getMe(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserSummary user = authService.getMe(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Authenticated user profile retrieved.", Map.of("user", user)));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user name")
    public ResponseEntity<ApiResponse<Map<String, UserSummary>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserSummary user = authService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully.", Map.of("user", user)));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password with current password verification")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully. Please log in with your new password.", null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate client session")
    public ResponseEntity<ApiResponse<Object>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }
}
