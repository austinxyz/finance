package com.finance.app.controller;

import com.finance.app.dto.expense.BatchBudgetRequest;
import com.finance.app.dto.expense.ExpenseBudgetDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.expense.ExpenseBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支出预算Controller
 */
@RestController
@RequestMapping("/expense-budgets")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin
public class ExpenseBudgetController {

    private final ExpenseBudgetService budgetService;
    private final AuthHelper authHelper;

    /**
     * 获取指定家庭、年份、货币的预算
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBudgets(
            @RequestParam(required = false) Long familyId,
            @RequestParam Integer budgetYear,
            @RequestParam String currency,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // Use getAuthorizedFamilyId to support admin family switching
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

            List<ExpenseBudgetDTO> budgets = budgetService.getBudgets(authorizedFamilyId, budgetYear, currency);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", budgets);
            response.put("message", "查询成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询预算失败", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询预算失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 批量保存预算
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchSaveBudgets(
            @Valid @RequestBody BatchBudgetRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // Use getAuthorizedFamilyId to support admin family switching
            Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, request.getFamilyId());
            request.setFamilyId(authorizedFamilyId);

            List<ExpenseBudgetDTO> savedBudgets = budgetService.batchSaveBudgets(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", savedBudgets);
            response.put("message", "保存成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量保存预算失败", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "保存预算失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除预算
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBudget(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Get existing budget to verify family access
            ExpenseBudgetDTO existingBudget = budgetService.getBudgetById(id);
            authHelper.requireFamilyAccess(authHeader, existingBudget.getFamilyId());

            budgetService.deleteBudget(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除预算失败", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除预算失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }
}
