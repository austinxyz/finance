package com.finance.app.controller;

import com.finance.app.dto.ApiResponse;
import com.finance.app.dto.AssetAccountDTO;
import com.finance.app.dto.AssetRecordDTO;
import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.dto.BatchRecordCheckDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.model.AssetType;
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
            @RequestParam(required = false) Long familyId) {
        List<AssetAccountDTO> accounts = assetService.getAllAccounts(userId, familyId);
        return ApiResponse.success(accounts);
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<AssetAccount> getAccount(@PathVariable Long id) {
        AssetAccount account = assetService.getAccountById(id);
        return ApiResponse.success(account);
    }

    @PostMapping("/accounts")
    public ApiResponse<AssetAccount> createAccount(@RequestBody AssetAccount account) {
        AssetAccount created = assetService.createAccount(account);
        return ApiResponse.success("Account created successfully", created);
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<AssetAccountDTO> updateAccount(
            @PathVariable Long id,
            @RequestBody AssetAccount account) {
        AssetAccount updated = assetService.updateAccount(id, account);
        // Convert to DTO to avoid lazy loading issues
        AssetAccountDTO dto = assetService.convertAccountToDTO(updated);
        return ApiResponse.success("Account updated successfully", dto);
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long id) {
        assetService.deleteAccount(id);
        return ApiResponse.success("Account deleted successfully", null);
    }

    // ========== Record Endpoints ==========

    @GetMapping("/accounts/{accountId}/records")
    public ApiResponse<List<AssetRecordDTO>> getAccountRecords(@PathVariable Long accountId) {
        List<AssetRecordDTO> records = assetService.getAccountRecords(accountId);
        return ApiResponse.success(records);
    }

    @PostMapping("/records")
    public ApiResponse<AssetRecordDTO> createRecord(@RequestBody AssetRecord record) {
        AssetRecordDTO created = assetService.createRecord(record);
        return ApiResponse.success("Record created successfully", created);
    }

    @PutMapping("/records/{id}")
    public ApiResponse<AssetRecordDTO> updateRecord(
            @PathVariable Long id,
            @RequestBody AssetRecord record) {
        AssetRecordDTO updated = assetService.updateRecord(id, record);
        return ApiResponse.success("Record updated successfully", updated);
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        assetService.deleteRecord(id);
        return ApiResponse.success("Record deleted successfully", null);
    }

    // 检查哪些账户在指定日期已有记录
    @PostMapping("/records/batch/check")
    public ApiResponse<List<Long>> checkExistingRecords(@RequestBody BatchRecordCheckDTO checkRequest) {
        LocalDate recordDate = checkRequest.getRecordDate();
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }
        List<Long> existingAccountIds = assetService.checkExistingRecords(recordDate, checkRequest.getAccountIds());
        return ApiResponse.success(existingAccountIds);
    }

    // 批量更新资产记录
    @PostMapping("/records/batch")
    public ApiResponse<Map<String, Object>> batchUpdateRecords(@RequestBody BatchRecordUpdateDTO batchUpdate) {
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
            @RequestParam String date) {
        LocalDate targetDate = LocalDate.parse(date);
        Map<String, Object> result = assetService.getAccountValueAtDate(accountId, targetDate);
        return ApiResponse.success(result);
    }
}
