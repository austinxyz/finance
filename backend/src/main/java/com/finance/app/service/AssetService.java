package com.finance.app.service;

import com.finance.app.dto.AssetAccountDTO;
import com.finance.app.dto.AssetRecordDTO;
import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetCategory;
import com.finance.app.model.AssetRecord;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetCategoryRepository;
import com.finance.app.repository.AssetRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetAccountRepository accountRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetRecordRepository recordRepository;
    private final com.finance.app.repository.UserRepository userRepository;

    // ========== Category Operations ==========

    public List<String> getAllCategoryTypes(Long userId) {
        List<AssetCategory> categories = categoryRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return categories.stream()
                .map(AssetCategory::getType)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<AssetCategory> getAllCategories(Long userId) {
        return categoryRepository.findByUserIdOrderByDisplayOrderAsc(userId);
    }

    public AssetCategory createCategory(AssetCategory category) {
        return categoryRepository.save(category);
    }

    // ========== Account Operations ==========

    public List<AssetAccountDTO> getAllAccounts(Long userId) {
        List<AssetAccount> accounts;
        if (userId == null) {
            accounts = accountRepository.findByIsActiveTrue();
        } else {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }
        return accounts.stream().map(this::convertAccountToDTO).collect(Collectors.toList());
    }

    public AssetAccount getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
    }

    @Transactional
    public AssetAccount createAccount(AssetAccount account) {
        return accountRepository.save(account);
    }

    @Transactional
    public AssetAccount updateAccount(Long accountId, AssetAccount accountDetails) {
        AssetAccount account = getAccountById(accountId);
        account.setUserId(accountDetails.getUserId());
        account.setCategoryId(accountDetails.getCategoryId());
        account.setAccountName(accountDetails.getAccountName());
        account.setAccountNumber(accountDetails.getAccountNumber());
        account.setInstitution(accountDetails.getInstitution());
        account.setCurrency(accountDetails.getCurrency());
        account.setNotes(accountDetails.getNotes());
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        AssetAccount account = getAccountById(accountId);

        // 检查是否有关联的资产记录
        boolean hasRecords = recordRepository.existsByAccountId(accountId);

        if (hasRecords) {
            // 如果有记录，只能标记为inactive（软删除）
            account.setIsActive(false);
            accountRepository.save(account);
        } else {
            // 如果没有记录，可以真正删除
            accountRepository.delete(account);
        }
    }

    // ========== Record Operations ==========

    public List<AssetRecordDTO> getAccountRecords(Long accountId) {
        List<AssetRecord> records = recordRepository.findByAccountIdOrderByRecordDateDesc(accountId);
        return records.stream().map(this::convertToRecordDTO).collect(Collectors.toList());
    }

    @Transactional
    public AssetRecordDTO createRecord(AssetRecord record) {
        // 检查是否已存在同一日期的记录
        if (recordRepository.existsByAccountIdAndRecordDate(record.getAccountId(), record.getRecordDate())) {
            throw new RuntimeException("Record already exists for this date");
        }

        // 自动从账户获取userId
        AssetAccount account = getAccountById(record.getAccountId());
        record.setUserId(account.getUserId());

        // 如果没有设置基准货币金额，自动计算
        if (record.getAmountInBaseCurrency() == null && record.getExchangeRate() != null) {
            record.setAmountInBaseCurrency(
                record.getAmount().multiply(record.getExchangeRate())
            );
        }

        AssetRecord savedRecord = recordRepository.save(record);
        return convertToRecordDTO(savedRecord);
    }

    @Transactional
    public AssetRecordDTO updateRecord(Long recordId, AssetRecord recordDetails) {
        AssetRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found with id: " + recordId));

        record.setAmount(recordDetails.getAmount());
        record.setQuantity(recordDetails.getQuantity());
        record.setUnitPrice(recordDetails.getUnitPrice());
        record.setNotes(recordDetails.getNotes());

        // 重新计算基准货币金额
        if (record.getExchangeRate() != null) {
            record.setAmountInBaseCurrency(
                record.getAmount().multiply(record.getExchangeRate())
            );
        }

        AssetRecord updatedRecord = recordRepository.save(record);
        return convertToRecordDTO(updatedRecord);
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        recordRepository.deleteById(recordId);
    }

    // 检查哪些账户在指定日期已有记录
    public List<Long> checkExistingRecords(LocalDate recordDate, List<Long> accountIds) {
        List<Long> existingAccountIds = new ArrayList<>();
        for (Long accountId : accountIds) {
            if (recordRepository.existsByAccountIdAndRecordDate(accountId, recordDate)) {
                existingAccountIds.add(accountId);
            }
        }
        return existingAccountIds;
    }

    // 获取指定日期账户的之前值(离该日期最近但不晚于该日期的记录)
    public java.util.Map<String, Object> getAccountValueAtDate(Long accountId, LocalDate targetDate) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();

        // 检查该日期是否已有记录
        boolean hasExactRecord = recordRepository.existsByAccountIdAndRecordDate(accountId, targetDate);
        result.put("hasExactRecord", hasExactRecord);

        if (hasExactRecord) {
            // 如果该日期已有记录,返回该记录的值
            var record = recordRepository.findByAccountIdAndRecordDate(accountId, targetDate);
            record.ifPresent(r -> {
                result.put("amount", r.getAmount());
                result.put("recordDate", r.getRecordDate());
                result.put("currency", r.getCurrency());
                result.put("exchangeRate", r.getExchangeRate());
            });
        } else {
            // 查找该日期之前最近的记录
            var previousRecord = recordRepository.findLatestByAccountIdBeforeOrOnDate(accountId, targetDate);
            previousRecord.ifPresent(r -> {
                result.put("amount", r.getAmount());
                result.put("recordDate", r.getRecordDate());
                result.put("currency", r.getCurrency());
                result.put("exchangeRate", r.getExchangeRate());
            });
        }

        return result;
    }

    // 批量更新资产记录
    @Transactional
    public List<AssetRecord> batchUpdateRecords(BatchRecordUpdateDTO batchUpdate) {
        LocalDate recordDate = batchUpdate.getRecordDate();
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }

        List<AssetRecord> savedRecords = new ArrayList<>();
        final LocalDate finalRecordDate = recordDate;
        boolean overwriteExisting = batchUpdate.getOverwriteExisting() != null && batchUpdate.getOverwriteExisting();

        for (BatchRecordUpdateDTO.AccountUpdate accountUpdate : batchUpdate.getAccounts()) {
            // 获取账户信息
            AssetAccount account = getAccountById(accountUpdate.getAccountId());

            // 检查是否已存在该日期的记录
            var existingRecordOpt = recordRepository.findByAccountIdAndRecordDate(
                accountUpdate.getAccountId(), finalRecordDate);

            AssetRecord record;
            boolean isUpdate = false;
            if (existingRecordOpt.isPresent()) {
                if (overwriteExisting) {
                    // 覆盖已存在的记录 (UPDATE)
                    record = existingRecordOpt.get();
                    isUpdate = true;
                } else {
                    // 跳过这个账户
                    continue;
                }
            } else {
                // 创建新记录 (INSERT)
                record = new AssetRecord();
                record.setAccountId(accountUpdate.getAccountId());
                record.setUserId(account.getUserId());
                record.setRecordDate(finalRecordDate);
                isUpdate = false;
            }

            // 更新记录数据
            record.setAmount(accountUpdate.getAmount());
            record.setQuantity(accountUpdate.getQuantity());
            record.setUnitPrice(accountUpdate.getUnitPrice());

            // 设置币种和汇率
            String currency = accountUpdate.getCurrency() != null ? accountUpdate.getCurrency() : account.getCurrency();
            record.setCurrency(currency);

            BigDecimal exchangeRate = accountUpdate.getExchangeRate();
            if (exchangeRate == null) {
                exchangeRate = BigDecimal.ONE; // 默认汇率为1
            }
            record.setExchangeRate(exchangeRate);

            // 计算基准货币金额
            BigDecimal amountInBaseCurrency = accountUpdate.getAmount().multiply(exchangeRate);
            record.setAmountInBaseCurrency(amountInBaseCurrency);

            // 保存记录
            AssetRecord saved = recordRepository.save(record);
            savedRecords.add(saved);
        }

        return savedRecords;
    }

    // ========== Helper Methods ==========

    public AssetAccountDTO convertAccountToDTO(AssetAccount account) {
        AssetAccountDTO dto = new AssetAccountDTO();
        dto.setId(account.getId());
        dto.setUserId(account.getUserId());
        dto.setCategoryId(account.getCategoryId());
        dto.setAccountName(account.getAccountName());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setInstitution(account.getInstitution());
        dto.setCurrency(account.getCurrency());
        dto.setNotes(account.getNotes());
        dto.setIsActive(account.getIsActive());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());

        // 获取用户信息
        userRepository.findById(account.getUserId())
                .ifPresent(user -> dto.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername()));

        // 获取类别信息
        if (account.getCategory() != null) {
            dto.setCategoryName(account.getCategory().getName());
            dto.setCategoryType(account.getCategory().getType());
        }

        // 获取最新金额和记录日期
        recordRepository.findLatestByAccountId(account.getId())
                .ifPresent(record -> {
                    dto.setLatestAmount(record.getAmount());  // 原币种金额
                    dto.setLatestAmountInBaseCurrency(record.getAmountInBaseCurrency());  // 基准货币金额
                    dto.setLatestRecordDate(record.getRecordDate());
                });

        return dto;
    }

    private AssetRecordDTO convertToRecordDTO(AssetRecord record) {
        AssetRecordDTO dto = new AssetRecordDTO();
        dto.setId(record.getId());
        dto.setAccountId(record.getAccountId());
        dto.setRecordDate(record.getRecordDate());
        dto.setAmount(record.getAmount());
        dto.setQuantity(record.getQuantity());
        dto.setUnitPrice(record.getUnitPrice());
        dto.setCurrency(record.getCurrency());
        dto.setExchangeRate(record.getExchangeRate());
        dto.setAmountInBaseCurrency(record.getAmountInBaseCurrency());
        dto.setNotes(record.getNotes());
        dto.setAttachmentUrl(record.getAttachmentUrl());

        if (record.getAccount() != null) {
            dto.setAccountName(record.getAccount().getAccountName());
        }

        return dto;
    }
}
