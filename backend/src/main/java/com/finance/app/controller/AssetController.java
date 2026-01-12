package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.AssetAccountDTO;
import com.finance.app.dto.AssetRecordDTO;
import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.dto.BatchRecordCheckDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.AssetType;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.asset.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
@CrossOrigin
public class AssetController {

    private final AssetService assetService;
    private final AuthHelper authHelper;

    // ========== Asset Type Endpoints ==========

    @GetMapping("/types")
    public ApiResponse<List<Map<String, Object>>> getAssetTypes() {
        List<AssetType> assetTypes = assetService.getAllAssetTypes();

        List<Map<String, Object>> typeData = assetTypes.stream()
            .map(assetType -> {
                Map<String, Object> typeMap = new HashMap<>();
                typeMap.put("categoryId", assetType.getId());
                typeMap.put("categoryName", assetType.getChineseName());
                typeMap.put("categoryType", assetType.getType());
                typeMap.put("categoryIcon", assetType.getIcon());
                typeMap.put("displayOrder", assetType.getDisplayOrder());
                typeMap.put("color", assetType.getColor());
                return typeMap;
            })
            .collect(Collectors.toList());

        return ApiResponse.success(typeData);
    }

    // ========== Account Endpoints ==========

    @GetMapping("/accounts")
    public ApiResponse<List<AssetAccountDTO>> getAccounts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long familyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Admin can view any family, regular user can only view their own family
        Long authorizedFamilyId = authHelper.getAuthorizedFamilyId(authHeader, familyId);

        List<AssetAccountDTO> accounts = assetService.getAllAccounts(userId, authorizedFamilyId);
        return ApiResponse.success(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<AssetAccount> getAccount(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        AssetAccount account = assetService.getAccountById(id);

        // Verify account access (userId belongs to authenticated user's family)
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        return ApiResponse.success(account);
    }

    @PostMapping("/accounts")
    public ApiResponse<AssetAccount> createAccount(
            @RequestBody AssetAccount account,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Set userId from authenticated user
        Long userId = authHelper.getUserIdFromAuth(authHeader);
        account.setUserId(userId);

        AssetAccount created = assetService.createAccount(account);
        return ApiResponse.success("Account created successfully", created);
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<AssetAccountDTO> updateAccount(
            @PathVariable Long id,
            @RequestBody AssetAccount account,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access for the existing account
        AssetAccount existing = assetService.getAccountById(id);
        authHelper.requireAccountAccess(authHeader, existing.getUserId());

        // Prevent userId tampering
        account.setUserId(existing.getUserId());

        AssetAccount updated = assetService.updateAccount(id, account);
        // Convert to DTO to avoid lazy loading issues
        AssetAccountDTO dto = assetService.convertAccountToDTO(updated);
        return ApiResponse.success("Account updated successfully", dto);
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access
        AssetAccount account = assetService.getAccountById(id);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        assetService.deleteAccount(id);
        return ApiResponse.success("Account deleted successfully", null);
    }

    // ========== Record Endpoints ==========

    @GetMapping("/accounts/{accountId}/records")
    public ApiResponse<List<AssetRecordDTO>> getAccountRecords(
            @PathVariable Long accountId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access
        AssetAccount account = assetService.getAccountById(accountId);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        List<AssetRecordDTO> records = assetService.getAccountRecords(accountId);
        return ApiResponse.success(records);
    }

    @PostMapping("/records")
    public ApiResponse<AssetRecordDTO> createRecord(
            @RequestBody AssetRecord record,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access for the account
        AssetAccount account = assetService.getAccountById(record.getAccountId());
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        AssetRecordDTO created = assetService.createRecord(record);
        return ApiResponse.success("Record created successfully", created);
    }

    @PutMapping("/records/{id}")
    public ApiResponse<AssetRecordDTO> updateRecord(
            @PathVariable Long id,
            @RequestBody AssetRecord record,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Get existing record and verify account access
        AssetRecordDTO existingRecord = assetService.getRecordById(id);
        AssetAccount account = assetService.getAccountById(existingRecord.getAccountId());
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        AssetRecordDTO updated = assetService.updateRecord(id, record);
        return ApiResponse.success("Record updated successfully", updated);
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Get existing record and verify account access
        AssetRecordDTO existingRecord = assetService.getRecordById(id);
        AssetAccount account = assetService.getAccountById(existingRecord.getAccountId());
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        assetService.deleteRecord(id);
        return ApiResponse.success("Record deleted successfully", null);
    }

    // 检查哪些账户在指定日期已有记录
    @PostMapping("/records/batch/check")
    public ApiResponse<List<Long>> checkExistingRecords(
            @RequestBody BatchRecordCheckDTO checkRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify all accounts belong to user's family
        for (Long accountId : checkRequest.getAccountIds()) {
            AssetAccount account = assetService.getAccountById(accountId);
            authHelper.requireAccountAccess(authHeader, account.getUserId());
        }

        LocalDate recordDate = checkRequest.getRecordDate();
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }
        List<Long> existingAccountIds = assetService.checkExistingRecords(recordDate, checkRequest.getAccountIds());
        return ApiResponse.success(existingAccountIds);
    }

    // 批量更新资产记录
    @PostMapping("/records/batch")
    public ApiResponse<Map<String, Object>> batchUpdateRecords(
            @RequestBody BatchRecordUpdateDTO batchUpdate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify all accounts belong to user's family
        for (BatchRecordUpdateDTO.AccountUpdate accountUpdate : batchUpdate.getAccounts()) {
            AssetAccount account = assetService.getAccountById(accountUpdate.getAccountId());
            authHelper.requireAccountAccess(authHeader, account.getUserId());
        }

        List<AssetRecord> records = assetService.batchUpdateRecords(batchUpdate);
        Map<String, Object> result = new HashMap<>();
        result.put("count", records.size());
        result.put("recordDate", batchUpdate.getRecordDate());
        return ApiResponse.success("Batch records created successfully", result);
    }

    // 获取指定日期账户的之前值(离该日期最近但不晚于该日期的记录)
    @GetMapping("/accounts/{accountId}/value-at-date")
    public ApiResponse<Map<String, Object>> getAccountValueAtDate(
            @PathVariable Long accountId,
            @RequestParam String date,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Verify account access
        AssetAccount account = assetService.getAccountById(accountId);
        authHelper.requireAccountAccess(authHeader, account.getUserId());

        LocalDate targetDate = LocalDate.parse(date);
        Map<String, Object> result = assetService.getAccountValueAtDate(accountId, targetDate);
        return ApiResponse.success(result);
    }
}
