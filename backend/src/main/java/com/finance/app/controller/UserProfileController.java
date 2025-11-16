package com.finance.app.controller;

import com.finance.app.dto.UserProfileDTO;
import com.finance.app.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    // 获取用户配置
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestParam(required = false) Long userId) {

        // 如果没有提供userId，使用默认的userId（简化处理，实际应该从认证信息获取）
        Long effectiveUserId = (userId != null) ? userId : 1L;

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
            @RequestBody UserProfileDTO dto) {

        // 如果没有提供userId，使用默认的userId
        Long effectiveUserId = (userId != null) ? userId : 1L;

        UserProfileDTO saved = userProfileService.saveUserProfile(effectiveUserId, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "用户配置保存成功");

        return ResponseEntity.ok(response);
    }
}
