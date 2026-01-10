package com.finance.app.controller;

import com.finance.app.dto.UserProfileDTO;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthHelper authHelper;

    // 获取用户配置
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long authenticatedUserId = authHelper.getUserIdFromAuth(authHeader);

        // Allow if accessing self OR if admin
        Long effectiveUserId = (userId != null) ? userId : authenticatedUserId;
        if (!authenticatedUserId.equals(effectiveUserId) && !authHelper.isAdmin(authHeader)) {
            throw new UnauthorizedException("只能查看自己的配置");
        }

        UserProfileDTO profile = userProfileService.getUserProfile(effectiveUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", profile);

        return ResponseEntity.ok(response);
    }

    // 保存用户配置
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveUserProfile(
            @RequestParam(required = false) Long userId,
            @RequestBody UserProfileDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long authenticatedUserId = authHelper.getUserIdFromAuth(authHeader);

        // Allow if updating self OR if admin
        Long effectiveUserId = (userId != null) ? userId : authenticatedUserId;
        if (!authenticatedUserId.equals(effectiveUserId) && !authHelper.isAdmin(authHeader)) {
            throw new UnauthorizedException("只能修改自己的配置");
        }

        UserProfileDTO saved = userProfileService.saveUserProfile(effectiveUserId, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "用户配置保存成功");

        return ResponseEntity.ok(response);
    }
}
