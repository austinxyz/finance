package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.User;
import com.finance.app.security.AuthHelper;
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
    private final AuthHelper authHelper;

    @GetMapping
    public ApiResponse<List<User>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        authHelper.requireAdmin(authHeader);

        List<User> users = userService.getAllUsers();
        return ApiResponse.success(users);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long authenticatedUserId = authHelper.getUserIdFromAuth(authHeader);

        // Allow if viewing self OR if admin
        if (!authenticatedUserId.equals(id) && !authHelper.isAdmin(authHeader)) {
            throw new UnauthorizedException("只能查看自己的信息");
        }

        User user = userService.getUserById(id);
        return ApiResponse.success(user);
    }

    @PostMapping
    public ApiResponse<User> createUser(
            @RequestBody User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        authHelper.requireAdmin(authHeader);

        User created = userService.createUser(user);
        return ApiResponse.success("User created successfully", created);
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long authenticatedUserId = authHelper.getUserIdFromAuth(authHeader);

        // Allow if updating self OR if admin
        if (!authenticatedUserId.equals(id) && !authHelper.isAdmin(authHeader)) {
            throw new UnauthorizedException("只能修改自己的信息");
        }

        User updated = userService.updateUser(id, user);
        return ApiResponse.success("User updated successfully", updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        authHelper.requireAdmin(authHeader);

        userService.deleteUser(id);
        return ApiResponse.success("User deleted successfully", null);
    }
}
