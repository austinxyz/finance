package com.finance.app.controller;

import com.finance.app.dto.investment.*;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetType;
import com.finance.app.model.InvestmentTransaction;
import com.finance.app.repository.AssetTypeRepository;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.asset.AssetAccountService;
import com.finance.app.service.investment.InvestmentTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投资交易Controller
 * 提供投资账户查询、投资交易记录的CRUD接口
 */
@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
@Slf4j
public class InvestmentTransactionController {

    private final InvestmentTransactionService investmentTransactionService;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetAccountService assetAccountService;
    private final AuthHelper authHelper;

    // ==================== 投资账户查询API ====================

    /**
     * 获取家庭所有投资账户列表
     * GET /api/investments/accounts?familyId={familyId}
     */
    @GetMapping("/accounts")
    public ResponseEntity<Map<String, Object>> getInvestmentAccounts(
        @RequestParam Long familyId
    ) {
        try {
            List<InvestmentAccountDTO> accounts = investmentTransactionService.getInvestmentAccounts(familyId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", accounts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取投资账户列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取投资账户列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据大类获取投资账户
     * GET /api/investments/accounts/by-category?familyId={familyId}&categoryId={categoryId}
     */
    @GetMapping("/accounts/by-category")
    public ResponseEntity<Map<String, Object>> getInvestmentAccountsByCategory(
        @RequestParam Long familyId,
        @RequestParam Long categoryId
    ) {
        try {
            List<InvestmentAccountDTO> accounts = investmentTransactionService
                .getInvestmentAccountsByCategory(familyId, categoryId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", accounts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取投资账户列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取投资账户列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有投资类别（从asset_type表获取is_investment = TRUE的大类）
     * GET /api/investments/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getInvestmentCategories() {
        try {
            List<AssetType> assetTypes = assetTypeRepository.findByIsInvestmentTrueOrderByDisplayOrderAsc();

            // 转换为前端需要的格式
            List<Map<String, Object>> categoryData = assetTypes.stream()
                .map(assetType -> {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("categoryId", assetType.getId());
                    categoryMap.put("categoryName", assetType.getChineseName());
                    categoryMap.put("categoryType", assetType.getType());
                    categoryMap.put("categoryIcon", assetType.getIcon());
                    categoryMap.put("displayOrder", assetType.getDisplayOrder());
                    categoryMap.put("color", assetType.getColor());
                    return categoryMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categoryData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取投资类别列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取投资类别列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== 投资交易记录API ====================

    /**
     * 获取账户的交易记录
     * GET /api/investments/transactions?accountId={accountId}&startPeriod={startPeriod}&endPeriod={endPeriod}
     */
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactionsByAccount(
        @RequestParam Long accountId,
        @RequestParam(required = false) String startPeriod,
        @RequestParam(required = false) String endPeriod
    ) {
        try {
            List<InvestmentTransactionDTO> transactions = investmentTransactionService
                .getTransactionsByAccount(accountId, startPeriod, endPeriod);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", transactions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取投资交易记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取投资交易记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建投资交易记录
     * POST /api/investments/transactions
     */
    @PostMapping("/transactions")
    public ResponseEntity<Map<String, Object>> createTransaction(
        @Valid @RequestBody CreateInvestmentTransactionRequest request
    ) {
        try {
            InvestmentTransactionDTO transaction = investmentTransactionService.createTransaction(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "投资交易记录创建成功");
            response.put("data", transaction);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("创建投资交易记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("创建投资交易记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建投资交易记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新投资交易记录
     * PUT /api/investments/transactions/{id}
     */
    @PutMapping("/transactions/{id}")
    public ResponseEntity<Map<String, Object>> updateTransaction(
        @PathVariable Long id,
        @Valid @RequestBody CreateInvestmentTransactionRequest request
    ) {
        try {
            InvestmentTransactionDTO transaction = investmentTransactionService.updateTransaction(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "投资交易记录更新成功");
            response.put("data", transaction);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("更新投资交易记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("更新投资交易记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新投资交易记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除投资交易记录
     * DELETE /api/investments/transactions/{id}
     */
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Map<String, Object>> deleteTransaction(@PathVariable Long id) {
        try {
            investmentTransactionService.deleteTransaction(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "投资交易记录删除成功");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("删除投资交易记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("删除投资交易记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除投资交易记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 批量保存投资交易记录
     * POST /api/investments/transactions/batch
     */
    @PostMapping("/transactions/batch")
    public ResponseEntity<Map<String, Object>> batchSaveTransactions(
        @Valid @RequestBody BatchInvestmentTransactionRequest request
    ) {
        try {
            Map<String, Object> result = investmentTransactionService.batchSaveTransactions(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量保存成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("批量保存投资交易记录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("批量保存投资交易记录失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量保存投资交易记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
