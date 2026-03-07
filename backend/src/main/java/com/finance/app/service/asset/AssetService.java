package com.finance.app.service.asset;

import com.finance.app.dto.AssetAccountDTO;
import com.finance.app.dto.AssetRecordDTO;
import com.finance.app.dto.BatchRecordUpdateDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetRecord;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.AssetTypeRepository;
import com.finance.app.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetAccountRepository accountRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetRecordRepository recordRepository;
    private final com.finance.app.repository.UserRepository userRepository;
    private final com.finance.app.repository.LiabilityAccountRepository liabilityAccountRepository;
    private final ExchangeRateService exchangeRateService;
    private final com.finance.app.service.DataProtectionService dataProtectionService;

    // ========== Asset Type Operations ==========

    public List<com.finance.app.model.AssetType> getAllAssetTypes() {
        return assetTypeRepository.findAllByOrderByDisplayOrderAsc();
    }

    // ========== Account Operations ==========

    public List<AssetAccountDTO> getAllAccounts(Long userId, Long familyId) {
        List<AssetAccount> accounts;
        // 优先级：familyId > userId > 所有账户
        if (familyId != null) {
            accounts = accountRepository.findByFamilyIdAndIsActiveTrue(familyId);
        } else if (userId != null) {
            accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        } else {
            accounts = accountRepository.findByIsActiveTrue();
        }
        if (accounts.isEmpty()) return List.of();

        // 批量预加载所有依赖数据，避免 N+1 查询
        List<Long> accountIds = accounts.stream().map(AssetAccount::getId).collect(Collectors.toList());
        Set<Long> userIds = accounts.stream().map(AssetAccount::getUserId).collect(Collectors.toSet());

        // 1. 批量加载用户
        Map<Long, com.finance.app.model.User> userMap = userRepository.findAllById(userIds)
                .stream().collect(Collectors.toMap(com.finance.app.model.User::getId, u -> u));

        // 2. 批量加载每个账户的最新记录
        Map<Long, AssetRecord> latestRecordMap = recordRepository.findLatestByAccountIds(accountIds)
                .stream().collect(Collectors.toMap(AssetRecord::getAccountId, r -> r, (a, b) -> a));

        // 3. 批量加载汇率 (按 currency+date 去重)
        Map<String, BigDecimal> exchangeRateCache = new HashMap<>();
        latestRecordMap.values().forEach(record -> {
            String currency = record.getCurrency();
            LocalDate date = record.getRecordDate();
            if (currency != null && !currency.equalsIgnoreCase("USD") && date != null) {
                String key = currency + ":" + date;
                exchangeRateCache.computeIfAbsent(key, k -> exchangeRateService.getExchangeRate(currency, date));
            }
        });

        // 4. 批量加载关联负债账户
        Set<Long> linkedIds = accounts.stream()
                .map(AssetAccount::getLinkedLiabilityAccountId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> linkedAccountNameMap = linkedIds.isEmpty() ? Map.of() :
                liabilityAccountRepository.findAllById(linkedIds).stream()
                        .collect(Collectors.toMap(
                                com.finance.app.model.LiabilityAccount::getId,
                                com.finance.app.model.LiabilityAccount::getAccountName));

        // 用预加载数据构建 DTO（无额外 DB 查询）
        return accounts.stream().map(account -> {
            AssetAccountDTO dto = new AssetAccountDTO();
            dto.setId(account.getId());
            dto.setUserId(account.getUserId());
            dto.setAssetTypeId(account.getAssetTypeId());
            dto.setAccountName(account.getAccountName());
            dto.setAccountNumber(account.getAccountNumber());
            dto.setInstitution(account.getInstitution());
            dto.setCurrency(account.getCurrency());
            dto.setNotes(account.getNotes());
            dto.setIsActive(account.getIsActive());
            dto.setTaxStatus(account.getTaxStatus());
            dto.setCreatedAt(account.getCreatedAt());
            dto.setUpdatedAt(account.getUpdatedAt());

            com.finance.app.model.User user = userMap.get(account.getUserId());
            if (user != null) {
                dto.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername());
            }

            if (account.getAssetType() != null) {
                dto.setAssetTypeName(account.getAssetType().getChineseName());
                dto.setAssetTypeCode(account.getAssetType().getType());
                dto.setAssetTypeIcon(account.getAssetType().getIcon());
            }

            AssetRecord latest = latestRecordMap.get(account.getId());
            if (latest != null) {
                dto.setLatestAmount(latest.getAmount());
                dto.setLatestRecordDate(latest.getRecordDate());
                if (latest.getAmount() != null && latest.getCurrency() != null) {
                    String key = latest.getCurrency() + ":" + latest.getRecordDate();
                    BigDecimal rate = exchangeRateCache.getOrDefault(key, BigDecimal.ONE);
                    dto.setLatestAmountInBaseCurrency(latest.getAmount().multiply(rate));
                }
            }

            dto.setLinkedLiabilityAccountId(account.getLinkedLiabilityAccountId());
            if (account.getLinkedLiabilityAccountId() != null) {
                dto.setLinkedLiabilityAccountName(linkedAccountNameMap.get(account.getLinkedLiabilityAccountId()));
            }

            return dto;
        }).collect(Collectors.toList());
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
        account.setAssetTypeId(accountDetails.getAssetTypeId());
        account.setAccountName(accountDetails.getAccountName());
        account.setAccountNumber(accountDetails.getAccountNumber());
        account.setInstitution(accountDetails.getInstitution());
        account.setCurrency(accountDetails.getCurrency());
        account.setNotes(accountDetails.getNotes());
        account.setTaxStatus(accountDetails.getTaxStatus());
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        AssetAccount account = getAccountById(accountId);

        // 数据保护：检查是否属于受保护的家庭
        Long familyId = getFamilyIdByUserId(account.getUserId());
        dataProtectionService.validateDeleteOperation(familyId, "删除资产账户: " + account.getAccountName());

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

        AssetRecord savedRecord = recordRepository.save(record);
        return convertToRecordDTO(savedRecord);
    }

    public AssetRecordDTO getRecordById(Long recordId) {
        AssetRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found with id: " + recordId));
        return convertToRecordDTO(record);
    }

    @Transactional
    public AssetRecordDTO updateRecord(Long recordId, AssetRecord recordDetails) {
        AssetRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found with id: " + recordId));

        record.setRecordDate(recordDetails.getRecordDate());
        record.setAmount(recordDetails.getAmount());
        record.setQuantity(recordDetails.getQuantity());
        record.setUnitPrice(recordDetails.getUnitPrice());
        record.setNotes(recordDetails.getNotes());

        AssetRecord updatedRecord = recordRepository.save(record);
        return convertToRecordDTO(updatedRecord);
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        AssetRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new RuntimeException("Record not found with id: " + recordId));

        AssetAccount account = getAccountById(record.getAccountId());
        Long familyId = getFamilyIdByUserId(account.getUserId());
        dataProtectionService.validateDeleteOperation(familyId, "删除资产记录");

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
            });
        } else {
            // 查找该日期之前最近的记录
            var previousRecord = recordRepository.findLatestByAccountIdBeforeOrOnDate(accountId, targetDate);
            previousRecord.ifPresent(r -> {
                result.put("amount", r.getAmount());
                result.put("recordDate", r.getRecordDate());
                result.put("currency", r.getCurrency());
            });
        }

        return result;
    }

    // 批量获取多个账户在指定日期的之前值（性能优化：一次查询代替N次）
    public Map<Long, Map<String, Object>> batchGetAccountValuesAtDate(List<Long> accountIds, LocalDate targetDate) {
        List<AssetRecord> records = recordRepository.findLatestByAccountIdsBeforeOrEqualDate(accountIds, targetDate);

        // Build a map of accountId -> record for fast lookup
        Map<Long, AssetRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AssetRecord::getAccountId, r -> r, (a, b) -> a));

        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Long accountId : accountIds) {
            Map<String, Object> entry = new HashMap<>();
            AssetRecord record = recordMap.get(accountId);
            if (record != null) {
                entry.put("amount", record.getAmount());
                entry.put("recordDate", record.getRecordDate());
                entry.put("currency", record.getCurrency());
                entry.put("hasExactRecord", record.getRecordDate().equals(targetDate));
            }
            result.put(accountId, entry);
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

            // 设置币种 (默认USD)
            String currency = accountUpdate.getCurrency() != null ? accountUpdate.getCurrency() : account.getCurrency();
            record.setCurrency(currency);

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
        dto.setAssetTypeId(account.getAssetTypeId());
        dto.setAccountName(account.getAccountName());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setInstitution(account.getInstitution());
        dto.setCurrency(account.getCurrency());
        dto.setNotes(account.getNotes());
        dto.setIsActive(account.getIsActive());
        dto.setTaxStatus(account.getTaxStatus());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());

        // 获取用户信息
        userRepository.findById(account.getUserId())
                .ifPresent(user -> dto.setUserName(user.getFullName() != null ? user.getFullName() : user.getUsername()));

        // 获取资产类型信息
        if (account.getAssetType() != null) {
            dto.setAssetTypeName(account.getAssetType().getChineseName());
            dto.setAssetTypeCode(account.getAssetType().getType());
            dto.setAssetTypeIcon(account.getAssetType().getIcon());
        }

        // 获取最新金额和记录日期
        recordRepository.findLatestByAccountId(account.getId())
                .ifPresent(record -> {
                    dto.setLatestAmount(record.getAmount());  // 原币种金额
                    dto.setLatestRecordDate(record.getRecordDate());

                    // 计算基准货币金额（USD）
                    if (record.getAmount() != null && record.getCurrency() != null) {
                        BigDecimal rate = exchangeRateService.getExchangeRate(
                            record.getCurrency(),
                            record.getRecordDate()
                        );
                        BigDecimal amountInUSD = record.getAmount().multiply(rate);
                        dto.setLatestAmountInBaseCurrency(amountInUSD);
                    }
                });

        // 获取关联的负债账户信息
        dto.setLinkedLiabilityAccountId(account.getLinkedLiabilityAccountId());
        if (account.getLinkedLiabilityAccountId() != null) {
            liabilityAccountRepository.findById(account.getLinkedLiabilityAccountId())
                    .ifPresent(linkedAccount -> dto.setLinkedLiabilityAccountName(linkedAccount.getAccountName()));
        }

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
        dto.setNotes(record.getNotes());
        dto.setAttachmentUrl(record.getAttachmentUrl());

        if (record.getAccount() != null) {
            dto.setAccountName(record.getAccount().getAccountName());
        }

        return dto;
    }

    /**
     * 根据userId获取familyId
     * 用于数据保护验证
     */
    private Long getFamilyIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
            .map(com.finance.app.model.User::getFamilyId)
            .orElse(null);
    }
}
