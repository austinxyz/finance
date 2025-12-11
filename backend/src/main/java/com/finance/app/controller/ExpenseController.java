package com.finance.app.controller;

import com.finance.app.dto.expense.*;
import com.finance.app.service.expense.ExpenseService;
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
 * 支出管理Controller
 * 提供支出分类、支出记录的CRUD接口
 */
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Slf4j
public class ExpenseController {

    private final ExpenseService expenseService;

    // ==================== 分类管理API ====================

    /**
     * 获取所有支出分类（大类+子分类）
     * GET /api/expenses/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<ExpenseCategoryDTO> categories = expenseService.getAllCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取支出分类失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取支出分类失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建子分类
     * POST /api/expenses/categories/minor
     */
    @PostMapping("/categories/minor")
    public ResponseEntity<Map<String, Object>> createMinorCategory(
        @Valid @RequestBody CreateMinorCategoryRequest request
    ) {
        try {
            ExpenseCategoryDTO.MinorCategoryDTO category = expenseService.createMinorCategory(request);

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
     * 更新子分类
     * PUT /api/expenses/categories/minor/{id}
     */
    @PutMapping("/categories/minor/{id}")
    public ResponseEntity<Map<String, Object>> updateMinorCategory(
        @PathVariable Long id,
        @Valid @RequestBody CreateMinorCategoryRequest request
    ) {
        try {
            ExpenseCategoryDTO.MinorCategoryDTO category = expenseService.updateMinorCategory(id, request);

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
     * 停用/删除子分类
     * DELETE /api/expenses/categories/minor/{id}
     */
    @DeleteMapping("/categories/minor/{id}")
    public ResponseEntity<Map<String, Object>> disableMinorCategory(@PathVariable Long id) {
        try {
            expenseService.disableMinorCategory(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "子分类处理成功");

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

    // ==================== 支出记录API ====================

    /**
     * 创建支出记录
     * POST /api/expenses/records
     */
    @PostMapping("/records")
    public ResponseEntity<Map<String, Object>> createExpenseRecord(
        @Valid @RequestBody CreateExpenseRecordRequest request
    ) {
        try {
            ExpenseRecordDTO record = expenseService.createExpenseRecord(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "支出记录创建成功");
            response.put("data", record);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("创建支出记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("创建支出记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建支出记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新支出记录
     * PUT /api/expenses/records/{id}
     */
    @PutMapping("/records/{id}")
    public ResponseEntity<Map<String, Object>> updateExpenseRecord(
        @PathVariable Long id,
        @Valid @RequestBody UpdateExpenseRecordRequest request
    ) {
        try {
            ExpenseRecordDTO record = expenseService.updateExpenseRecord(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "支出记录更新成功");
            response.put("data", record);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("更新支出记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("更新支出记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新支出记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量保存支出记录
     * POST /api/expenses/records/batch
     */
    @PostMapping("/records/batch")
    public ResponseEntity<Map<String, Object>> batchSaveExpenseRecords(
        @Valid @RequestBody BatchExpenseRecordRequest request
    ) {
        try {
            List<ExpenseRecordDTO> records = expenseService.batchSaveExpenseRecords(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量保存成功");
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("批量保存支出记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("批量保存支出记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量保存失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询支出记录（按期间）
     * GET /api/expenses/records?familyId=1&period=2024-12
     */
    @GetMapping("/records")
    public ResponseEntity<Map<String, Object>> getExpenseRecordsByPeriod(
        @RequestParam Long familyId,
        @RequestParam String period
    ) {
        try {
            List<ExpenseRecordDTO> records = expenseService.getExpenseRecordsByPeriod(familyId, period);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询支出记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询支出记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询支出记录（按期间范围）
     * GET /api/expenses/records/range?familyId=1&startPeriod=2024-01&endPeriod=2024-12
     */
    @GetMapping("/records/range")
    public ResponseEntity<Map<String, Object>> getExpenseRecordsByPeriodRange(
        @RequestParam Long familyId,
        @RequestParam String startPeriod,
        @RequestParam String endPeriod
    ) {
        try {
            List<ExpenseRecordDTO> records = expenseService.getExpenseRecordsByPeriodRange(
                familyId, startPeriod, endPeriod
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询支出记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询支出记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除支出记录
     * DELETE /api/expenses/records/{id}
     */
    @DeleteMapping("/records/{id}")
    public ResponseEntity<Map<String, Object>> deleteExpenseRecord(@PathVariable Long id) {
        try {
            expenseService.deleteExpenseRecord(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "支出记录删除成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除支出记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除支出记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
