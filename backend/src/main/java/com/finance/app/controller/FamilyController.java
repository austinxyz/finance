package com.finance.app.controller;

import com.finance.app.dto.FamilyDTO;
import com.finance.app.dto.UserDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.FamilyService;
import com.finance.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final UserService userService;
    private final AuthHelper authHelper;

    /**
     * 获取所有家庭列表（仅管理员）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllFamilies(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        authHelper.requireAdmin(authHeader);

        List<FamilyDTO> families = familyService.getAllFamilies();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", families);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取家庭信息（通过用户ID）
     * Self or admin only
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getFamilyByUserId(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long authenticatedUserId = authHelper.getUserIdFromAuth(authHeader);

        // Allow if querying self OR if admin
        authHelper.requireAccountAccess(authHeader, userId);

        FamilyDTO family = familyService.getFamilyByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", family);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取家庭信息（通过家庭ID）
     * Own family or admin only
     */
    @GetMapping("/{familyId}")
    public ResponseEntity<Map<String, Object>> getFamilyById(
            @PathVariable Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify family access (own family or admin)
        authHelper.requireFamilyAccess(authHeader, familyId);

        FamilyDTO family = familyService.getFamilyById(familyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", family);

        return ResponseEntity.ok(response);
    }

    /**
     * 保存或更新家庭信息
     * Own family or admin only
     */
    @PostMapping("/{familyId}")
    public ResponseEntity<Map<String, Object>> saveFamily(
            @PathVariable Long familyId,
            @RequestBody FamilyDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify family access (own family or admin)
        authHelper.requireFamilyAccess(authHeader, familyId);

        FamilyDTO saved = familyService.saveFamily(familyId, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "家庭信息保存成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 创建新家庭（仅管理员）
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFamily(
            @RequestBody FamilyDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        authHelper.requireAdmin(authHeader);

        FamilyDTO saved = familyService.saveFamily(null, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "家庭创建成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取家庭成员列表
     * Own family or admin only
     */
    @GetMapping("/{familyId}/members")
    public ResponseEntity<Map<String, Object>> getFamilyMembers(
            @PathVariable Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify family access (own family or admin)
        authHelper.requireFamilyAccess(authHeader, familyId);

        List<UserDTO> members = userService.getFamilyMembers(familyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", members);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取默认家庭
     * Returns authenticated user's family
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefaultFamily(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Use authenticated user's family
        Long familyId = authHelper.getFamilyIdFromAuth(authHeader);
        FamilyDTO family = familyService.getFamilyById(familyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", family);

        return ResponseEntity.ok(response);
    }

    /**
     * 设置默认家庭（仅管理员）
     * In multi-family setups, admin can set system default
     */
    @PostMapping("/{familyId}/set-default")
    public ResponseEntity<Map<String, Object>> setDefaultFamily(
            @PathVariable Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin-only endpoint
        authHelper.requireAdmin(authHeader);

        familyService.setDefaultFamily(familyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "默认家庭设置成功");

        return ResponseEntity.ok(response);
    }
}
