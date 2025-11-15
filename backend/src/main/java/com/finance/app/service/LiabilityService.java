package com.finance.app.service;

import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.dto.LiabilityAccountDTO;
import com.finance.app.dto.LiabilityRecordDTO;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityCategory;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.repository.LiabilityAccountRepository;
import com.finance.app.repository.LiabilityCategoryRepository;
import com.finance.app.repository.LiabilityRecordRepository;
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
public class LiabilityService {

    private final LiabilityAccountRepository accountRepository;
    private final LiabilityCategoryRepository categoryRepository;
    private final LiabilityRecordRepository recordRepository;
    private final com.finance.app.repository.UserRepository userRepository;

    // ========== Category Operations ==========

    public List<String> getAllCategoryTypes(Long userId) {
        List<LiabilityCategory> categories = categoryRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return categories.stream()
                .map(LiabilityCategory::getType)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<LiabilityCategory> getAllCategories(Long userId) {
        return categoryRepository.findByUserIdOrderByDisplayOrderAsc(userId);
    }

    public LiabilityCategory createCategory(LiabilityCategory category) {
        return categoryRepository.save(category);
    }

    // ========== Account Operations ==========

    public List<LiabilityAccountDTO> getAllAccounts(Long userId) {
        List<LiabilityAccount> accounts;
        if (userId == null) {
            accounts = accountRepository.findAll().stream()
                    .filter(LiabilityAccount::getIsActive)
                    .collect(Collectors.toList());
        } else {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        }
        return accounts.stream().map(this::convertAccountToDTO).collect(Collectors.toList());
    }

    public LiabilityAccount getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));
    }

    @Transactional
    public LiabilityAccount createAccount(LiabilityAccount account) {
        return accountRepository.save(account);
    }

    @Transactional
    public LiabilityAccount updateAccount(Long accountId, LiabilityAccount accountDetails) {
        LiabilityAccount account = getAccountById(accountId);
        account.setUserId(accountDetails.getUserId());
        account.setCategoryId(accountDetails.getCategoryId());
        account.setAccountName(accountDetails.getAccountName());
        account.setAccountNumber(accountDetails.getAccountNumber());
        account.setInstitution(accountDetails.getInstitution());
        account.setCurrency(accountDetails.getCurrency());
        account.setInterestRate(accountDetails.getInterestRate());
        account.setOriginalAmount(accountDetails.getOriginalAmount());
        account.setStartDate(accountDetails.getStartDate());
        account.setEndDate(accountDetails.getEndDate());
        account.setMonthlyPayment(accountDetails.getMonthlyPayment());
        account.setNotes(accountDetails.getNotes());
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        LiabilityAccount account = getAccountById(accountId);

        // 检查是否有关联的负债记录
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

    public List<LiabilityRecordDTO> getAccountRecords(Long accountId) {
        List<LiabilityRecord> records = recordRepository.findByAccountIdOrderByRecordDateDesc(accountId);
        return records.stream().map(this::convertToRecordDTO).collect(Collectors.toList());
    }

    @Transactional
    public LiabilityRecord createRecord(LiabilityRecord record) {
        // 检查是否已存在同一日期的记录
        if (recordRepository.existsByAccountIdAndRecordDate(record.getAccountId(), record.getRecordDate())) {
            throw new RuntimeException("Record already exists for this date");
        }

        // 自动从账户获取userId
        LiabilityAccount account = getAccountById(record.getAccountId());
        record.setUserId(account.getUserId());

        // 如果没有设置基准货币余额，自动计算
        if (record.getBalanceInBaseCurrency() == null && record.getExchangeRate() != null) {
            record.setBalanceInBaseCurrency(
                record.getOutstandingBalance().multiply(record.getExchangeRate())
            );
        }

        return recordRepository.save(record);
    }

    @Transactional
    public LiabilityRecord updateRecord(Long recordId, LiabilityRecord recordDetails) {
        LiabilityRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found with id: " + recordId));

        // Update basic fields
        record.setRecordDate(recordDetails.getRecordDate());
        record.setOutstandingBalance(recordDetails.getOutstandingBalance());
        record.setCurrency(recordDetails.getCurrency());
        record.setExchangeRate(recordDetails.getExchangeRate());
        record.setPaymentAmount(recordDetails.getPaymentAmount());
        record.setPrincipalPayment(recordDetails.getPrincipalPayment());
        record.setInterestPayment(recordDetails.getInterestPayment());
        record.setNotes(recordDetails.getNotes());

        // 重新计算基准货币余额
        if (record.getExchangeRate() != null && record.getOutstandingBalance() != null) {
            record.setBalanceInBaseCurrency(
                record.getOutstandingBalance().multiply(record.getExchangeRate())
            );
        }

        return recordRepository.save(record);
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
                result.put("amount", r.getOutstandingBalance());
                result.put("recordDate", r.getRecordDate());
                result.put("currency", r.getCurrency());
                result.put("exchangeRate", r.getExchangeRate());
            });
        } else {
            // 查找该日期之前最近的记录
            var previousRecord = recordRepository.findLatestByAccountIdBeforeOrOnDate(accountId, targetDate);
            previousRecord.ifPresent(r -> {
                result.put("amount", r.getOutstandingBalance());
                result.put("recordDate", r.getRecordDate());
                result.put("currency", r.getCurrency());
                result.put("exchangeRate", r.getExchangeRate());
            });
        }

        return result;
    }

    // 批量更新负债记录
    @Transactional
    public List<LiabilityRecordDTO> batchUpdateRecords(BatchRecordUpdateDTO batchUpdate) {
        LocalDate recordDate = batchUpdate.getRecordDate();
        System.out.println("=== 批量更新负债记录 ===");
        System.out.println("接收到的日期: " + batchUpdate.getRecordDate());
        System.out.println("处理后的日期: " + recordDate);
        if (recordDate == null) {
            recordDate = LocalDate.now();
            System.out.println("使用当前日期: " + recordDate);
        }

        List<LiabilityRecord> savedRecords = new ArrayList<>();
        final LocalDate finalRecordDate = recordDate;
        boolean overwriteExisting = batchUpdate.getOverwriteExisting() != null && batchUpdate.getOverwriteExisting();

        for (BatchRecordUpdateDTO.AccountUpdate accountUpdate : batchUpdate.getAccounts()) {
            // 获取账户信息
            LiabilityAccount account = getAccountById(accountUpdate.getAccountId());

            // 检查是否已存在该日期的记录
            LiabilityRecord record = null;
            var existingRecordOpt = recordRepository.findByAccountIdAndRecordDate(
                accountUpdate.getAccountId(), finalRecordDate);

            if (existingRecordOpt.isPresent()) {
                if (overwriteExisting) {
                    // 覆盖已存在的记录
                    record = existingRecordOpt.get();
                } else {
                    // 跳过这个账户
                    continue;
                }
            }

            if (record == null) {
                // 创建新记录
                record = new LiabilityRecord();
                record.setAccountId(accountUpdate.getAccountId());
                record.setUserId(account.getUserId());
                record.setRecordDate(finalRecordDate);
            }

            // 更新记录数据 - 对于负债，使用amount字段作为余额
            record.setOutstandingBalance(accountUpdate.getAmount());

            // 设置币种和汇率
            String currency = accountUpdate.getCurrency() != null ? accountUpdate.getCurrency() : account.getCurrency();
            record.setCurrency(currency);

            BigDecimal exchangeRate = accountUpdate.getExchangeRate();
            if (exchangeRate == null) {
                exchangeRate = BigDecimal.ONE; // 默认汇率为1
            }
            record.setExchangeRate(exchangeRate);

            // 计算基准货币余额
            BigDecimal balanceInBaseCurrency = accountUpdate.getAmount().multiply(exchangeRate);
            record.setBalanceInBaseCurrency(balanceInBaseCurrency);

            // 保存记录
            LiabilityRecord saved = recordRepository.save(record);
            savedRecords.add(saved);
        }

        // 转换为DTO返回，避免序列化懒加载代理对象
        return savedRecords.stream()
                .map(this::convertToRecordDTO)
                .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========

    public LiabilityAccountDTO convertAccountToDTO(LiabilityAccount account) {
        LiabilityAccountDTO dto = new LiabilityAccountDTO();
        dto.setId(account.getId());
        dto.setUserId(account.getUserId());
        dto.setCategoryId(account.getCategoryId());
        dto.setAccountName(account.getAccountName());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setInstitution(account.getInstitution());
        dto.setCurrency(account.getCurrency());
        dto.setInterestRate(account.getInterestRate());
        dto.setOriginalAmount(account.getOriginalAmount());
        dto.setStartDate(account.getStartDate());
        dto.setEndDate(account.getEndDate());
        dto.setMonthlyPayment(account.getMonthlyPayment());
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

        // 获取最新余额和记录日期
        recordRepository.findLatestByAccountId(account.getId())
                .ifPresent(record -> {
                    dto.setLatestBalance(record.getOutstandingBalance());  // 原币种余额
                    dto.setLatestBalanceInBaseCurrency(record.getBalanceInBaseCurrency());  // 基准货币余额
                    dto.setLatestRecordDate(record.getRecordDate());
                });

        return dto;
    }

    public LiabilityRecordDTO convertToRecordDTO(LiabilityRecord record) {
        LiabilityRecordDTO dto = new LiabilityRecordDTO();
        dto.setId(record.getId());
        dto.setAccountId(record.getAccountId());
        dto.setRecordDate(record.getRecordDate());
        dto.setOutstandingBalance(record.getOutstandingBalance());
        dto.setCurrency(record.getCurrency());
        dto.setExchangeRate(record.getExchangeRate());
        dto.setBalanceInBaseCurrency(record.getBalanceInBaseCurrency());
        dto.setPaymentAmount(record.getPaymentAmount());
        dto.setPrincipalPayment(record.getPrincipalPayment());
        dto.setInterestPayment(record.getInterestPayment());
        dto.setNotes(record.getNotes());

        if (record.getAccount() != null) {
            dto.setAccountName(record.getAccount().getAccountName());
        }

        return dto;
    }
}
