package com.finance.app.controller;

import com.finance.app.model.PropertyRecord;
import com.finance.app.service.PropertyRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/property-records")
@CrossOrigin
public class PropertyRecordController {

    @Autowired
    private PropertyRecordService propertyRecordService;

    /**
     * 创建房产记录
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody PropertyRecord propertyRecord) {
        Map<String, Object> response = new HashMap<>();
        try {
            PropertyRecord created = propertyRecordService.create(propertyRecord);
            response.put("success", true);
            response.put("data", created);
            response.put("message", "房产记录创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新房产记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody PropertyRecord propertyRecord) {
        Map<String, Object> response = new HashMap<>();
        try {
            PropertyRecord updated = propertyRecordService.update(id, propertyRecord);
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "房产记录更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除房产记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            propertyRecordService.delete(id);
            response.put("success", true);
            response.put("message", "房产记录删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取房产记录详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            PropertyRecord record = propertyRecordService.findById(id)
                    .orElseThrow(() -> new RuntimeException("房产记录不存在"));
            response.put("success", true);
            response.put("data", record);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据资产账户ID获取房产记录
     */
    @GetMapping("/by-asset/{assetAccountId}")
    public ResponseEntity<Map<String, Object>> getByAssetAccountId(@PathVariable Long assetAccountId) {
        Map<String, Object> response = new HashMap<>();
        try {
            PropertyRecord record = propertyRecordService.findByAssetAccountId(assetAccountId)
                    .orElseThrow(() -> new RuntimeException("该资产账户没有关联的房产记录"));
            response.put("success", true);
            response.put("data", record);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取家庭所有房产记录
     */
    @GetMapping("/family/{familyId}")
    public ResponseEntity<Map<String, Object>> getByFamilyId(@PathVariable Long familyId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<PropertyRecord> records = propertyRecordService.findByFamilyId(familyId);
            response.put("success", true);
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取指定年份购买的房产记录
     */
    @GetMapping("/family/{familyId}/year/{year}")
    public ResponseEntity<Map<String, Object>> getByFamilyIdAndYear(
            @PathVariable Long familyId,
            @PathVariable Integer year) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<PropertyRecord> records = propertyRecordService.findByFamilyIdAndPurchaseYear(familyId, year);
            response.put("success", true);
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
