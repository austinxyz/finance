package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.BatchRecordCheckDTO;
import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.dto.LiabilityAccountDTO;
import com.finance.app.dto.LiabilityRecordDTO;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityCategory;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.service.LiabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/liabilities")
@RequiredArgsConstructor
@CrossOrigin
public class LiabilityController {

    private final LiabilityService liabilityService;

    // ========== Category Endpoints ==========

    @GetMapping("/categories/types")
    public ApiResponse<List<String>> getCategoryTypes(@RequestParam Long userId) {
        List<String> types = liabilityService.getAllCategoryTypes(userId);
        return ApiResponse.success(types);
    }

    @GetMapping("/categories")
    public ApiResponse<List<LiabilityCategory>> getCategories(@RequestParam Long userId) {
        List<LiabilityCategory> categories = liabilityService.getAllCategories(userId);
        return ApiResponse.success(categories);
    }

    @PostMapping("/categories")
    public ApiResponse<LiabilityCategory> createCategory(@RequestBody LiabilityCategory category) {
        LiabilityCategory created = liabilityService.createCategory(category);
        return ApiResponse.success("Category created successfully", created);
    }

    // ========== Account Endpoints ==========

    @GetMapping("/accounts")
    public ApiResponse<List<LiabilityAccountDTO>> getAccounts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId) {
        List<LiabilityAccountDTO> accounts = liabilityService.getAllAccounts(userId, familyId);
        return ApiResponse.success(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<LiabilityAccount> getAccount(@PathVariable Long id) {
        LiabilityAccount account = liabilityService.getAccountById(id);
        return ApiResponse.success(account);
    }

    @PostMapping("/accounts")
    public ApiResponse<LiabilityAccount> createAccount(@RequestBody LiabilityAccount account) {
        LiabilityAccount created = liabilityService.createAccount(account);
        return ApiResponse.success("Account created successfully", created);
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<LiabilityAccountDTO> updateAccount(
            @PathVariable Long id,
            @RequestBody LiabilityAccount account) {
        LiabilityAccount updated = liabilityService.updateAccount(id, account);
        // Convert to DTO to avoid lazy loading issues
        LiabilityAccountDTO dto = liabilityService.convertAccountToDTO(updated);
        return ApiResponse.success("Account updated successfully", dto);
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long id) {
        liabilityService.deleteAccount(id);
        return ApiResponse.success("Account deleted successfully", null);
    }

    // ========== Record Endpoints ==========

    @GetMapping("/accounts/{accountId}/records")
    public ApiResponse<List<LiabilityRecordDTO>> getAccountRecords(@PathVariable Long accountId) {
        List<LiabilityRecordDTO> records = liabilityService.getAccountRecords(accountId);
        return ApiResponse.success(records);
    }

    @PostMapping("/records")
    public ApiResponse<LiabilityRecord> createRecord(@RequestBody LiabilityRecord record) {
        LiabilityRecord created = liabilityService.createRecord(record);
        return ApiResponse.success("Record created successfully", created);
    }

    @PutMapping("/records/{id}")
    public ApiResponse<LiabilityRecordDTO> updateRecord(
            @PathVariable Long id,
            @RequestBody LiabilityRecord record) {
        LiabilityRecord updated = liabilityService.updateRecord(id, record);
        // Convert to DTO to avoid lazy loading issues
        LiabilityRecordDTO dto = liabilityService.convertToRecordDTO(updated);
        return ApiResponse.success("Record updated successfully", dto);
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        liabilityService.deleteRecord(id);
        return ApiResponse.success("Record deleted successfully", null);
    }

    // 检查哪些账户在指定日期已有记录
    @PostMapping("/records/batch/check")
    public ApiResponse<List<Long>> checkExistingRecords(@RequestBody BatchRecordCheckDTO checkRequest) {
        LocalDate recordDate = checkRequest.getRecordDate();
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }
        List<Long> existingAccountIds = liabilityService.checkExistingRecords(recordDate, checkRequest.getAccountIds());
        return ApiResponse.success(existingAccountIds);
    }

    // 批量更新负债记录
    @PostMapping("/records/batch")
    public ApiResponse<List<LiabilityRecordDTO>> batchUpdateRecords(@RequestBody BatchRecordUpdateDTO batchUpdate) {
        List<LiabilityRecordDTO> records = liabilityService.batchUpdateRecords(batchUpdate);
        return ApiResponse.success("Batch records created successfully", records);
    }

    // 获取指定日期账户的之前值(离该日期最近但不晚于该日期的记录)
    @GetMapping("/accounts/{accountId}/value-at-date")
    public ApiResponse<Map<String, Object>> getAccountValueAtDate(
            @PathVariable Long accountId,
            @RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        Map<String, Object> result = liabilityService.getAccountValueAtDate(accountId, targetDate);
        return ApiResponse.success(result);
    }
}
