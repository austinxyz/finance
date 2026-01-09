package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.BatchRecordCheckDTO;
import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.dto.LiabilityAccountDTO;
import com.finance.app.dto.LiabilityRecordDTO;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.model.LiabilityType;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.liability.LiabilityService;
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
    private final AuthHelper authHelper;

    // ========== Liability Type Endpoints ==========

    @GetMapping("/types")
    public ApiResponse<List<Map<String, Object>>> getLiabilityTypes() {
        List<LiabilityType> liabilityTypes = liabilityService.getAllLiabilityTypes();

        List<Map<String, Object>> typeData = liabilityTypes.stream()
            .map(liabilityType -> {
                Map<String, Object> typeMap = new java.util.HashMap<>();
                typeMap.put("categoryId", liabilityType.getId());
                typeMap.put("categoryName", liabilityType.getChineseName());
                typeMap.put("categoryType", liabilityType.getType());
                typeMap.put("categoryIcon", liabilityType.getIcon());
                typeMap.put("displayOrder", liabilityType.getDisplayOrder());
                typeMap.put("color", liabilityType.getColor());
                return typeMap;
            })
            .collect(java.util.stream.Collectors.toList());

        return ApiResponse.success(typeData);
    }

    // ========== Account Endpoints ==========

    @GetMapping("/accounts")
    public ApiResponse<List<LiabilityAccountDTO>> getAccounts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Get authenticated user's family_id
        Long authenticatedFamilyId = authHelper.getFamilyIdFromAuth(authHeader);

        // Use authenticated family_id (ignore query params for security)
        List<LiabilityAccountDTO> accounts = liabilityService.getAllAccounts(userId, authenticatedFamilyId);
        return ApiResponse.success(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<LiabilityAccount> getAccount(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        LiabilityAccount account = liabilityService.getAccountById(id);

        // Verify account access
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        return ApiResponse.success(account);
    }

    @PostMapping("/accounts")
    public ApiResponse<LiabilityAccount> createAccount(
            @RequestBody LiabilityAccount account,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Set userId from authenticated user
        Long userId = authHelper.getUserIdFromAuth(authHeader);
        account.setUserId(userId);

        LiabilityAccount created = liabilityService.createAccount(account);
        return ApiResponse.success("Account created successfully", created);
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<LiabilityAccountDTO> updateAccount(
            @PathVariable Long id,
            @RequestBody LiabilityAccount account,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access for the existing account
        LiabilityAccount existing = liabilityService.getAccountById(id);
        authHelper.requireAccountAccess(authHeader, existing.getUserId());

        // Prevent userId tampering
        account.setUserId(existing.getUserId());

        LiabilityAccount updated = liabilityService.updateAccount(id, account);
        // Convert to DTO to avoid lazy loading issues
        LiabilityAccountDTO dto = liabilityService.convertAccountToDTO(updated);
        return ApiResponse.success("Account updated successfully", dto);
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access
        LiabilityAccount account = liabilityService.getAccountById(id);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        liabilityService.deleteAccount(id);
        return ApiResponse.success("Account deleted successfully", null);
    }

    // ========== Record Endpoints ==========

    @GetMapping("/accounts/{accountId}/records")
    public ApiResponse<List<LiabilityRecordDTO>> getAccountRecords(
            @PathVariable Long accountId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access
        LiabilityAccount account = liabilityService.getAccountById(accountId);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        List<LiabilityRecordDTO> records = liabilityService.getAccountRecords(accountId);
        return ApiResponse.success(records);
    }

    @PostMapping("/records")
    public ApiResponse<LiabilityRecord> createRecord(
            @RequestBody LiabilityRecord record,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access for the account
        LiabilityAccount account = liabilityService.getAccountById(record.getAccountId());
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        LiabilityRecord created = liabilityService.createRecord(record);
        return ApiResponse.success("Record created successfully", created);
    }

    @PutMapping("/records/{id}")
    public ApiResponse<LiabilityRecordDTO> updateRecord(
            @PathVariable Long id,
            @RequestBody LiabilityRecord record,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Get existing record and verify account access
        LiabilityRecordDTO existingRecord = liabilityService.getRecordById(id);
        LiabilityAccount account = liabilityService.getAccountById(existingRecord.getAccountId());
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        LiabilityRecord updated = liabilityService.updateRecord(id, record);
        // Convert to DTO to avoid lazy loading issues
        LiabilityRecordDTO dto = liabilityService.convertToRecordDTO(updated);
        return ApiResponse.success("Record updated successfully", dto);
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Get existing record and verify account access
        LiabilityRecordDTO existingRecord = liabilityService.getRecordById(id);
        LiabilityAccount account = liabilityService.getAccountById(existingRecord.getAccountId());
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        liabilityService.deleteRecord(id);
        return ApiResponse.success("Record deleted successfully", null);
    }

    // 检查哪些账户在指定日期已有记录
    @PostMapping("/records/batch/check")
    public ApiResponse<List<Long>> checkExistingRecords(
            @RequestBody BatchRecordCheckDTO checkRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify all accounts belong to user's family
        for (Long accountId : checkRequest.getAccountIds()) {
            LiabilityAccount account = liabilityService.getAccountById(accountId);
            authHelper.requireAccountAccess(authHeader, account.getUserId());
        }

        LocalDate recordDate = checkRequest.getRecordDate();
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }
        List<Long> existingAccountIds = liabilityService.checkExistingRecords(recordDate, checkRequest.getAccountIds());
        return ApiResponse.success(existingAccountIds);
    }

    // 批量更新负债记录
    @PostMapping("/records/batch")
    public ApiResponse<List<LiabilityRecordDTO>> batchUpdateRecords(
            @RequestBody BatchRecordUpdateDTO batchUpdate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify all accounts belong to user's family
        for (BatchRecordUpdateDTO.AccountUpdate accountUpdate : batchUpdate.getAccounts()) {
            LiabilityAccount account = liabilityService.getAccountById(accountUpdate.getAccountId());
            authHelper.requireAccountAccess(authHeader, account.getUserId());
        }

        List<LiabilityRecordDTO> records = liabilityService.batchUpdateRecords(batchUpdate);
        return ApiResponse.success("Batch records created successfully", records);
    }

    // 获取指定日期账户的之前值(离该日期最近但不晚于该日期的记录)
    @GetMapping("/accounts/{accountId}/value-at-date")
    public ApiResponse<Map<String, Object>> getAccountValueAtDate(
            @PathVariable Long accountId,
            @RequestParam String date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access
        LiabilityAccount account = liabilityService.getAccountById(accountId);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        LocalDate targetDate = LocalDate.parse(date);
        Map<String, Object> result = liabilityService.getAccountValueAtDate(accountId, targetDate);
        return ApiResponse.success(result);
    }
}
