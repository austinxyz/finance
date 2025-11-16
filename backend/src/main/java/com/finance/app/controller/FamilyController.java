package com.finance.app.controller;

import com.finance.app.dto.FamilyDTO;
import com.finance.app.dto.UserDTO;
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

    /**
     * 获取所有家庭列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllFamilies() {
        List<FamilyDTO> families = familyService.getAllFamilies();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", families);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取家庭信息（通过用户ID）
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getFamilyByUserId(
            @PathVariable Long userId) {

        FamilyDTO family = familyService.getFamilyByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", family);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取家庭信息（通过家庭ID）
     */
    @GetMapping("/{familyId}")
    public ResponseEntity<Map<String, Object>> getFamilyById(
            @PathVariable Long familyId) {

        FamilyDTO family = familyService.getFamilyById(familyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", family);

        return ResponseEntity.ok(response);
    }

    /**
     * 保存或更新家庭信息
     */
    @PostMapping("/{familyId}")
    public ResponseEntity<Map<String, Object>> saveFamily(
            @PathVariable Long familyId,
            @RequestBody FamilyDTO dto) {

        FamilyDTO saved = familyService.saveFamily(familyId, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "家庭信息保存成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 创建新家庭
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFamily(
            @RequestBody FamilyDTO dto) {

        FamilyDTO saved = familyService.saveFamily(null, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", saved);
        response.put("message", "家庭创建成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取家庭成员列表
     */
    @GetMapping("/{familyId}/members")
    public ResponseEntity<Map<String, Object>> getFamilyMembers(
            @PathVariable Long familyId) {

        List<UserDTO> members = userService.getFamilyMembers(familyId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", members);

        return ResponseEntity.ok(response);
    }
}
