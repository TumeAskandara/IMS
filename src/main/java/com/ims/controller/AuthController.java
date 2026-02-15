package com.ims.controller;

import com.ims.dto.request.LoginRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.dto.response.AuthResponse;
import com.ims.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller
 * 
 * Handles user authentication and authorization operations
 * Base URL: /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * User Login
     * 
     * POST /api/v1/auth/login
     * 
     * Authenticates a user and returns a JWT access token.
     * 
     * Request Body:
     * {
     *   "username": "admin",
     *   "password": "password123"
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "accessToken": "eyJhbGc...",
     *     "tokenType": "Bearer",
     *     "userId": 1,
     *     "username": "admin",
     *     "fullName": "System Administrator",
     *     "role": "ADMIN",
     *     "branchId": 1,
     *     "branchName": "Main Branch"
     *   }
     * }
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and get access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Logout
     * 
     * POST /api/v1/auth/logout
     * 
     * Invalidates the user's current session.
     * Requires: Authorization header with Bearer token
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate current session")
    public ResponseEntity<ApiResponse<String>> logout() {
        // In a stateless JWT system, logout is typically handled client-side
        // by removing the token. Server-side blacklisting can be implemented with Redis
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
