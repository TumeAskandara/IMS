package com.ims.controller;

import com.ims.dto.request.UserRequest;
import com.ims.dto.response.ApiResponse;
import com.ims.entity.User;
import com.ims.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User Management Controller
 * 
 * Manages user accounts, roles, and profiles
 * Base URL: /api/v1/users
 * 
 * Access Control:
 * - GET endpoints: Accessible by all authenticated users
 * - POST, PUT, DELETE: Restricted to ADMIN role only
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * Get All Users (Paginated)
     * 
     * GET /api/v1/users?page=0&size=10&sort=createdAt,desc
     * 
     * Query Parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - sort: Sort field and direction (default: id,asc)
     * 
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "content": [...users...],
     *     "totalElements": 50,
     *     "totalPages": 5,
     *     "number": 0,
     *     "size": 10
     *   }
     * }
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve paginated list of users")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Get User by ID
     * 
     * GET /api/v1/users/{id}
     * 
     * Path Variable:
     * - id: User ID
     * 
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "id": 1,
     *     "username": "john.doe",
     *     "fullName": "John Doe",
     *     "email": "john@example.com",
     *     "role": "MANAGER",
     *     "branch": {...}
     *   }
     * }
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * Create User
     * 
     * POST /api/v1/users
     * 
     * Requires: ADMIN role
     * 
     * Request Body:
     * {
     *   "username": "newuser",
     *   "password": "SecurePass123!",
     *   "fullName": "New User",
     *   "email": "newuser@example.com",
     *   "phoneNumber": "+1234567890",
     *   "role": "SELLER",
     *   "branchId": 1,
     *   "isActive": true
     * }
     * 
     * Response: 201 Created
     * {
     *   "success": true,
     *   "message": "User created successfully",
     *   "data": {...created user...}
     * }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user account (Admin only)")
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody UserRequest request) {
        User createdUser = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", createdUser));
    }

    /**
     * Update User
     * 
     * PUT /api/v1/users/{id}
     * 
     * Requires: ADMIN role
     * 
     * Path Variable:
     * - id: User ID to update
     * 
     * Request Body: Same as Create User
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "User updated successfully",
     *   "data": {...updated user...}
     * }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update an existing user (Admin only)")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request
    ) {
        User updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    /**
     * Delete User (Soft Delete)
     * 
     * DELETE /api/v1/users/{id}
     * 
     * Requires: ADMIN role
     * 
     * Path Variable:
     * - id: User ID to delete
     * 
     * Response: 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }
}
