package com.finance.app.service.investment;

import com.finance.app.dto.investment.*;
import com.finance.app.model.*;
import com.finance.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 投资交易Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvestmentTransactionService {

    private final InvestmentTransactionRepository transactionRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final UserRepository userRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final com.finance.app.service.ExchangeRateService exchangeRateService;

    // ==================== 投资账户查询 ====================

    /**
     * 获取家庭所有投资账户列表
     * 优化：使用 assetTypeId 直接查询投资类账户
     */
    public List<InvestmentAccountDTO> getInvestmentAccounts(Long familyId) {
        // 1. 获取该家庭的所有用户
        List<User> familyUsers = userRepository.findByFamilyId(familyId);
        if (familyUsers.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> familyUserIds = familyUsers.stream()
            .map(User::getId)
            .collect(Collectors.toSet());

        // 2. 查询这些用户的投资类账户
        // 通过 assetType 的 isInvestment 字段直接过滤
        List<AssetAccount> accounts = assetAccountRepository.findAll().stream()
            .filter(account -> {
                // 过滤：只要该家庭成员的账户
                if (!familyUserIds.contains(account.getUserId())) {
                    return false;
                }
                // 过滤：只要投资类账户（通过 assetType.isInvestment 判断）
                return account.getAssetType() != null && Boolean.TRUE.equals(account.getAssetType().getIsInvestment());
            })
            .collect(Collectors.toList());

        return accounts.stream().map(account -> {
            User user = userRepository.findById(account.getUserId()).orElse(null);

            // 查询交易记录数
            int recordCount = transactionRepository.findByAccountIdOrderByTransactionPeriodDesc(account.getId()).size();

            // 查询最新资产记录
            List<AssetRecord> records = assetRecordRepository.findByAccountIdOrderByRecordDateDesc(account.getId());
            Double latestValue = null;
            Double latestValueInUSD = null;
            String latestRecordDate = null;
            if (!records.isEmpty()) {
                AssetRecord latest = records.get(0);
                latestValue = latest.getAmount().doubleValue();
                latestRecordDate = latest.getRecordDate().toString();

                // 转换为USD基准货币
                BigDecimal amountInUSD = exchangeRateService.convertToUSD(
                    latest.getAmount(),
                    latest.getCurrency(),
                    latest.getRecordDate()
                );
                latestValueInUSD = amountInUSD != null ? amountInUSD.doubleValue() : null;
            }

            return InvestmentAccountDTO.builder()
                .accountId(account.getId())
                .accountName(account.getAccountName())
                .categoryId(account.getAssetType() != null ? account.getAssetType().getId() : null)
                .categoryName(account.getAssetType() != null ? account.getAssetType().getChineseName() : null)
                .categoryType(account.getAssetType() != null ? account.getAssetType().getType() : null)
                .categoryIcon(account.getAssetType() != null ? account.getAssetType().getIcon() : null)
                .userId(account.getUserId())
                .userName(user != null ? user.getFullName() : null)
                .currency(account.getCurrency())
                .institution(account.getInstitution())
                .recordCount(recordCount)
                .latestValue(latestValue)
                .latestValueInUSD(latestValueInUSD)
                .latestRecordDate(latestRecordDate)
                .build();
        }).collect(Collectors.toList());
    }

    /**
     * 根据大类获取投资账户
     */
    public List<InvestmentAccountDTO> getInvestmentAccountsByCategory(Long familyId, Long categoryId) {
        List<InvestmentAccountDTO> allAccounts = getInvestmentAccounts(familyId);
        return allAccounts.stream()
            .filter(account -> account.getCategoryId().equals(categoryId))
            .collect(Collectors.toList());
    }

    // ==================== 交易记录管理 ====================

    /**
     * 获取账户的交易记录
     */
    public List<InvestmentTransactionDTO> getTransactionsByAccount(Long accountId, String startPeriod, String endPeriod) {
        List<InvestmentTransaction> transactions;

        if (startPeriod != null && endPeriod != null) {
            transactions = transactionRepository.findByAccountIdAndPeriodRange(accountId, startPeriod, endPeriod);
        } else {
            transactions = transactionRepository.findByAccountIdOrderByTransactionPeriodDesc(accountId);
        }

        return transactions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 创建投资交易记录
     */
    @Transactional
    public InvestmentTransactionDTO createTransaction(CreateInvestmentTransactionRequest request) {
        // 验证账户是否为投资类
        AssetAccount account = assetAccountRepository.findById(request.getAccountId())
            .orElseThrow(() -> new IllegalArgumentException("账户不存在"));

        if (account.getAssetType() == null || !Boolean.TRUE.equals(account.getAssetType().getIsInvestment())) {
            throw new IllegalArgumentException("只有投资类账户才能创建投资交易记录");
        }

        // 检查是否已存在相同类型的交易记录
        InvestmentTransaction.TransactionType type = InvestmentTransaction.TransactionType.valueOf(request.getTransactionType());
        Optional<InvestmentTransaction> existing = transactionRepository
            .findByAccountIdAndTransactionPeriodAndTransactionType(
                request.getAccountId(),
                request.getTransactionPeriod(),
                type
            );

        if (existing.isPresent()) {
            throw new IllegalArgumentException("该账户在此期间已存在相同类型的交易记录，请使用更新功能");
        }

        // 创建新记录
        InvestmentTransaction transaction = new InvestmentTransaction();
        transaction.setAccountId(request.getAccountId());
        transaction.setTransactionPeriod(request.getTransactionPeriod());
        transaction.setTransactionType(type);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        InvestmentTransaction saved = transactionRepository.save(transaction);
        log.info("创建投资交易记录成功: accountId={}, period={}, type={}, amount={}",
            request.getAccountId(), request.getTransactionPeriod(), request.getTransactionType(), request.getAmount());

        return convertToDTO(saved);
    }

    /**
     * 更新投资交易记录
     */
    @Transactional
    public InvestmentTransactionDTO updateTransaction(Long id, CreateInvestmentTransactionRequest request) {
        InvestmentTransaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("交易记录不存在"));

        // 验证账户
        AssetAccount account = assetAccountRepository.findById(request.getAccountId())
            .orElseThrow(() -> new IllegalArgumentException("账户不存在"));

        if (account.getAssetType() == null || !Boolean.TRUE.equals(account.getAssetType().getIsInvestment())) {
            throw new IllegalArgumentException("只有投资类账户才能创建投资交易记录");
        }

        transaction.setAccountId(request.getAccountId());
        transaction.setTransactionPeriod(request.getTransactionPeriod());
        transaction.setTransactionType(InvestmentTransaction.TransactionType.valueOf(request.getTransactionType()));
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        InvestmentTransaction saved = transactionRepository.save(transaction);
        log.info("更新投资交易记录成功: id={}", id);

        return convertToDTO(saved);
    }

    /**
     * 删除投资交易记录
     */
    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new IllegalArgumentException("交易记录不存在");
        }
        transactionRepository.deleteById(id);
        log.info("删除投资交易记录成功: id={}", id);
    }

    /**
     * 批量保存投资交易记录
     */
    @Transactional
    public Map<String, Object> batchSaveTransactions(BatchInvestmentTransactionRequest request) {
        int created = 0;
        int updated = 0;
        int deleted = 0;

        for (BatchInvestmentTransactionRequest.TransactionItem item : request.getTransactions()) {
            // 验证账户
            AssetAccount account = assetAccountRepository.findById(item.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("账户不存在: " + item.getAccountId()));

            if (account.getAssetType() == null || !Boolean.TRUE.equals(account.getAssetType().getIsInvestment())) {
                throw new IllegalArgumentException("只有投资类账户才能创建投资交易记录");
            }

            // 处理投入记录
            if (item.getDeposits() != null && item.getDeposits().compareTo(BigDecimal.ZERO) > 0) {
                created += saveOrUpdateTransaction(
                    item.getAccountId(),
                    request.getTransactionPeriod(),
                    InvestmentTransaction.TransactionType.DEPOSIT,
                    item.getDeposits(),
                    item.getDescription()
                );
            } else {
                // 删除原有的投入记录
                deleted += deleteTransactionIfExists(
                    item.getAccountId(),
                    request.getTransactionPeriod(),
                    InvestmentTransaction.TransactionType.DEPOSIT
                );
            }

            // 处理取出记录
            if (item.getWithdrawals() != null && item.getWithdrawals().compareTo(BigDecimal.ZERO) > 0) {
                created += saveOrUpdateTransaction(
                    item.getAccountId(),
                    request.getTransactionPeriod(),
                    InvestmentTransaction.TransactionType.WITHDRAWAL,
                    item.getWithdrawals(),
                    item.getDescription()
                );
            } else {
                // 删除原有的取出记录
                deleted += deleteTransactionIfExists(
                    item.getAccountId(),
                    request.getTransactionPeriod(),
                    InvestmentTransaction.TransactionType.WITHDRAWAL
                );
            }
        }

        log.info("批量保存投资交易记录完成: period={}, created={}, updated={}, deleted={}",
            request.getTransactionPeriod(), created, updated - created, deleted);

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("updated", updated - created);
        result.put("deleted", deleted);
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 保存或更新交易记录
     * 返回1表示创建，0表示更新
     */
    private int saveOrUpdateTransaction(Long accountId, String period,
                                       InvestmentTransaction.TransactionType type,
                                       BigDecimal amount, String description) {
        Optional<InvestmentTransaction> existing = transactionRepository
            .findByAccountIdAndTransactionPeriodAndTransactionType(accountId, period, type);

        if (existing.isPresent()) {
            // 更新现有记录
            InvestmentTransaction transaction = existing.get();
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transactionRepository.save(transaction);
            return 0; // 更新
        } else {
            // 创建新记录
            InvestmentTransaction transaction = new InvestmentTransaction();
            transaction.setAccountId(accountId);
            transaction.setTransactionPeriod(period);
            transaction.setTransactionType(type);
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transactionRepository.save(transaction);
            return 1; // 创建
        }
    }

    /**
     * 如果存在则删除交易记录
     * 返回删除数量
     */
    private int deleteTransactionIfExists(Long accountId, String period,
                                         InvestmentTransaction.TransactionType type) {
        Optional<InvestmentTransaction> existing = transactionRepository
            .findByAccountIdAndTransactionPeriodAndTransactionType(accountId, period, type);

        if (existing.isPresent()) {
            transactionRepository.delete(existing.get());
            return 1;
        }
        return 0;
    }

    /**
     * 转换为DTO
     */
    private InvestmentTransactionDTO convertToDTO(InvestmentTransaction transaction) {
        AssetAccount account = assetAccountRepository.findById(transaction.getAccountId()).orElse(null);
        User user = account != null ?
            userRepository.findById(account.getUserId()).orElse(null) : null;

        return InvestmentTransactionDTO.builder()
            .id(transaction.getId())
            .accountId(transaction.getAccountId())
            .accountName(account != null ? account.getAccountName() : null)
            .categoryId(account != null && account.getAssetType() != null ? account.getAssetType().getId() : null)
            .categoryName(account != null && account.getAssetType() != null ? account.getAssetType().getChineseName() : null)
            .categoryType(account != null && account.getAssetType() != null ? account.getAssetType().getType() : null)
            .userId(user != null ? user.getId() : null)
            .userName(user != null ? user.getFullName() : null)
            .transactionPeriod(transaction.getTransactionPeriod())
            .transactionType(transaction.getTransactionType().name())
            .amount(transaction.getAmount())
            .currency(account != null ? account.getCurrency() : null)
            .description(transaction.getDescription())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .build();
    }
}
