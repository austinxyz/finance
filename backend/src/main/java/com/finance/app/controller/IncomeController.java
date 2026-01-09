package com.finance.app.controller;

import com.finance.app.dto.income.*;
import com.finance.app.model.IncomeCategoryMajor;
import com.finance.app.model.IncomeCategoryMinor;
import com.finance.app.service.income.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收入管理Controller
 * 提供收入分类、收入记录的CRUD接口
 */
@RestController
@RequestMapping("/incomes")
@RequiredArgsConstructor
@Slf4j
public class IncomeController {

    private final IncomeService incomeService;

    // ==================== 分类管理API ====================

    /**
     * 获取所有收入大类
     * GET /api/incomes/categories/major
     */
    @GetMapping("/categories/major")
    public ResponseEntity<Map<String, Object>> getAllMajorCategories() {
        try {
            List<IncomeCategoryMajor> categories = incomeService.getAllMajorCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取收入大类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取收入大类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取指定大类的所有小类
     * GET /api/incomes/categories/major/{majorCategoryId}/minor
     */
    @GetMapping("/categories/major/{majorCategoryId}/minor")
    public ResponseEntity<Map<String, Object>> getMinorCategoriesByMajor(
        @PathVariable Long majorCategoryId
    ) {
        try {
            List<IncomeCategoryMinor> categories = incomeService.getMinorCategoriesByMajor(majorCategoryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取收入小类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取收入小类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据家庭ID和大类ID获取小类（包含公共分类和该家庭用户的私有分类）
     * GET /api/incomes/categories/minor?familyId={familyId}&majorCategoryId={majorCategoryId}
     */
    @GetMapping("/categories/minor")
    public ResponseEntity<Map<String, Object>> getMinorCategoriesByFamilyAndMajor(
        @RequestParam Long familyId,
        @RequestParam Long majorCategoryId
    ) {
        try {
            List<IncomeCategoryMinor> categories = incomeService.getMinorCategoriesByFamilyAndMajor(familyId, majorCategoryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取收入小类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取收入小类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有收入分类（大类+小类组合）
     * GET /api/incomes/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<IncomeCategoryDTO> categories = incomeService.getAllCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取收入分类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取收入分类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建收入小类
     * POST /api/incomes/categories/minor
     */
    @PostMapping("/categories/minor")
    public ResponseEntity<Map<String, Object>> createMinorCategory(@RequestBody Map<String, Object> request) {
        try {
            Long majorCategoryId = Long.valueOf(request.get("majorCategoryId").toString());
            String name = request.get("name").toString();
            String chineseName = request.get("chineseName").toString();
            Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
            String description = request.get("description") != null ? request.get("description").toString() : null;

            IncomeCategoryMinor category = incomeService.createMinorCategory(
                majorCategoryId, name, chineseName, userId, description
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "子分类创建成功");
            response.put("data", category);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("创建子分类失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("创建子分类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建子分类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新收入小类
     * PUT /api/incomes/categories/minor/{id}
     */
    @PutMapping("/categories/minor/{id}")
    public ResponseEntity<Map<String, Object>> updateMinorCategory(
        @PathVariable Long id,
        @RequestBody Map<String, Object> request
    ) {
        try {
            String name = request.get("name").toString();
            String chineseName = request.get("chineseName").toString();
            Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
            String description = request.get("description") != null ? request.get("description").toString() : null;

            IncomeCategoryMinor category = incomeService.updateMinorCategory(
                id, name, chineseName, userId, description
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "子分类更新成功");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("更新子分类失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("更新子分类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新子分类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除收入小类
     * DELETE /api/incomes/categories/minor/{id}
     */
    @DeleteMapping("/categories/minor/{id}")
    public ResponseEntity<Map<String, Object>> deleteMinorCategory(@PathVariable Long id) {
        try {
            incomeService.disableMinorCategory(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "子分类删除成功");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("删除子分类失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("删除子分类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除子分类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 收入记录API ====================

    /**
     * 创建收入记录
     * POST /api/incomes/records
     */
    @PostMapping("/records")
    public ResponseEntity<Map<String, Object>> createIncomeRecord(
        @Valid @RequestBody CreateIncomeRecordRequest request
    ) {
        try {
            IncomeRecordDTO record = incomeService.createIncomeRecord(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "收入记录创建成功");
            response.put("data", record);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("创建收入记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("创建收入记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建收入记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新收入记录
     * PUT /api/incomes/records/{id}
     */
    @PutMapping("/records/{id}")
    public ResponseEntity<Map<String, Object>> updateIncomeRecord(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIncomeRecordRequest request
    ) {
        try {
            IncomeRecordDTO record = incomeService.updateIncomeRecord(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "收入记录更新成功");
            response.put("data", record);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("更新收入记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("更新收入记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新收入记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量保存收入记录
     * POST /api/incomes/records/batch
     */
    @PostMapping("/records/batch")
    public ResponseEntity<Map<String, Object>> batchSaveIncomeRecords(
        @Valid @RequestBody BatchIncomeRecordRequest request
    ) {
        try {
            List<IncomeRecordDTO> records = incomeService.batchSaveIncomeRecords(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量保存成功");
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("批量保存收入记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("批量保存收入记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量保存失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询收入记录（按期间）
     * GET /api/incomes/records?familyId=1&period=2024-12
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getIncomeRecordsByPeriod(
        @RequestParam Long familyId,
        @RequestParam String period
    ) {
        try {
            List<IncomeRecordDTO> records = incomeService.getIncomeRecordsByPeriod(familyId, period);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询收入记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询收入记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询收入记录（按期间范围）
     * GET /api/incomes/records/range?familyId=1&startPeriod=2024-01&endPeriod=2024-12
     */
    @GetMapping("/records/range")
    public ResponseEntity<Map<String, Object>> getIncomeRecordsByPeriodRange(
        @RequestParam Long familyId,
        @RequestParam String startPeriod,
        @RequestParam String endPeriod
    ) {
        try {
            List<IncomeRecordDTO> records = incomeService.getIncomeRecordsByPeriodRange(
                familyId, startPeriod, endPeriod
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询收入记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询收入记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除收入记录
     * DELETE /api/incomes/records/{id}
     */
    @DeleteMapping("/records/{id}")
    public ResponseEntity<Map<String, Object>> deleteIncomeRecord(@PathVariable Long id) {
        try {
            incomeService.deleteIncomeRecord(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "收入记录删除成功");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("删除收入记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("删除收入记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除收入记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
