package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.User;
import com.finance.app.service.AuthService;
import com.finance.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * Helper method to extract JWT token from Authorization header
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @GetMapping
    public ApiResponse<List<User>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        String token = extractToken(authHeader);
        if (!authService.isAdminByToken(token)) {
            throw new UnauthorizedException("需要管理员权限");
        }

        List<User> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        String token = extractToken(authHeader);
        if (!authService.isAdminByToken(token)) {
            throw new UnauthorizedException("需要管理员权限");
        }

        User user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @PostMapping
    public ApiResponse<User> createUser(
            @RequestBody User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        String token = extractToken(authHeader);
        if (!authService.isAdminByToken(token)) {
            throw new UnauthorizedException("需要管理员权限");
        }

        User created = userService.createUser(user);
        return ApiResponse.success("User created successfully", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        String token = extractToken(authHeader);
        if (!authService.isAdminByToken(token)) {
            throw new UnauthorizedException("需要管理员权限");
        }

        User updated = userService.updateUser(id, user);
        return ApiResponse.success("User updated successfully", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        String token = extractToken(authHeader);
        if (!authService.isAdminByToken(token)) {
            throw new UnauthorizedException("需要管理员权限");
        }

        userService.deleteUser(id);
        return ApiResponse.success("User deleted successfully", null);
    }
}
