package com.finance.app.service;

import com.finance.app.dto.AccountMonthlyTrendResponseDTO;
import com.finance.app.dto.InvestmentAccountAnalysisDTO;
import com.finance.app.dto.InvestmentCategoryAnalysisDTO;
import com.finance.app.dto.InvestmentMonthlyTrendDTO;
import com.finance.app.model.AssetAccount;
import com.finance.app.model.AssetType;
import com.finance.app.model.ExpenseRecord;
import com.finance.app.model.InvestmentTransaction;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.model.User;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.AssetTypeRepository;
import com.finance.app.repository.ExpenseCategoryMajorRepository;
import com.finance.app.repository.ExpenseCategoryMinorRepository;
import com.finance.app.repository.ExpenseRecordRepository;
import com.finance.app.repository.InvestmentTransactionRepository;
import com.finance.app.repository.LiabilityAccountRepository;
import com.finance.app.repository.LiabilityRecordRepository;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentAnalysisService {

    private final InvestmentTransactionRepository transactionRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final UserRepository userRepository;
    private final ExchangeRateService exchangeRateService;
    private final LiabilityAccountRepository liabilityAccountRepository;
    private final LiabilityRecordRepository liabilityRecordRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final ExpenseCategoryMajorRepository expenseCategoryMajorRepository;
    private final ExpenseCategoryMinorRepository expenseCategoryMinorRepository;

    /**
     * 获取年度大类投资分析
     * @param currency 货币筛选条件：
     *                 - "USD": 只显示美元账户
     *                 - "CNY": 只显示人民币账户
     *                 - "All": 显示所有账户，并折算为美元
     */
    public List<InvestmentCategoryAnalysisDTO> getAnnualByCategory(Long familyId, Integer year, String currency) {
        // 获取所有投资类别
        List<AssetType> investmentCategories = assetTypeRepository.findByIsInvestmentTrueOrderByDisplayOrderAsc();

        // 获取该年度所有投资交易记录
        String yearPattern = year + "-%";
        List<InvestmentTransaction> transactions = transactionRepository.findByFamilyIdAndYearPattern(familyId, yearPattern);

        // 按大类分组统计
        Map<Long, InvestmentCategoryAnalysisDTO> categoryMap = new HashMap<>();

        for (AssetType assetType : investmentCategories) {
            InvestmentCategoryAnalysisDTO dto = new InvestmentCategoryAnalysisDTO();
            dto.setCategoryId(assetType.getId());
            dto.setCategoryName(assetType.getChineseName());
            dto.setCategoryIcon(assetType.getIcon());
            dto.setTotalDeposits(BigDecimal.ZERO);
            dto.setTotalWithdrawals(BigDecimal.ZERO);
            dto.setNetDeposits(BigDecimal.ZERO);
            categoryMap.put(assetType.getId(), dto);
        }

        // 统计每个大类的投入和取出
        for (InvestmentTransaction tx : transactions) {
            String accountCurrency = tx.getAccount().getCurrency();

            // 货币筛选：如果不是"All"，则只统计指定货币的账户
            if (!"All".equalsIgnoreCase(currency) && !accountCurrency.equalsIgnoreCase(currency)) {
                continue; // 跳过不匹配的货币账户
            }

            Long assetTypeId = tx.getAccount().getAssetTypeId();
            InvestmentCategoryAnalysisDTO dto = categoryMap.get(assetTypeId);

            if (dto != null) {
                BigDecimal amount = tx.getAmount();

                // 只有选择"All"时才进行货币转换（折算为USD）
                if ("All".equalsIgnoreCase(currency)) {
                    LocalDate transactionDate = LocalDate.parse(tx.getTransactionPeriod() + "-01");
                    amount = convertCurrency(amount, accountCurrency, "USD", transactionDate);
                }
                // 选择单一货币时，使用原始金额（不转换）

                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    dto.setTotalDeposits(dto.getTotalDeposits().add(amount));
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    dto.setTotalWithdrawals(dto.getTotalWithdrawals().add(amount));
                }
            }
        }

        // 计算净投入
        categoryMap.values().forEach(dto ->
            dto.setNetDeposits(dto.getTotalDeposits().subtract(dto.getTotalWithdrawals()))
        );

        // 计算投资回报率
        calculateReturnRates(familyId, year, currency, categoryMap);

        // 过滤掉没有任何资产的大类并排序（按当前资产排序）
        List<InvestmentCategoryAnalysisDTO> result = categoryMap.values().stream()
            .filter(dto -> dto.getCurrentAssets() != null && dto.getCurrentAssets().compareTo(BigDecimal.ZERO) > 0)
            .sorted((a, b) -> b.getCurrentAssets().compareTo(a.getCurrentAssets()))
            .collect(Collectors.toList());

        return result;
    }

    /**
     * 计算投资回报率
     * 回报率 = (当前资产 - 去年年底资产 - 净投入) / (去年年底资产 + 净投入)
     * @param targetCurrency 货币筛选条件："USD", "CNY", 或 "All"
     */
    private void calculateReturnRates(Long familyId, Integer year, String targetCurrency, Map<Long, InvestmentCategoryAnalysisDTO> categoryMap) {
        // 获取选中年份年底日期和去年年底日期
        LocalDate currentDate = LocalDate.of(year, 12, 31);  // 修复：使用选中年份的年末，而不是今天
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        // 获取该家庭的所有用户ID
        List<Long> userIds = getUserIdsByFamilyId(familyId);
        if (userIds.isEmpty()) {
            return;
        }

        // 获取所有投资账户
        List<AssetAccount> accounts = assetAccountRepository.findByUserIdInAndIsActiveTrue(userIds);

        // 按资产类型分组账户（保留完整账户对象以获取货币信息）
        // 并根据货币筛选条件过滤账户
        Map<Long, List<AssetAccount>> accountsByAssetType = accounts.stream()
            .filter(acc -> acc.getAssetType() != null && acc.getAssetType().getIsInvestment())
            // 货币筛选：如果不是"All"，则只包含指定货币的账户
            .filter(acc -> "All".equalsIgnoreCase(targetCurrency) || acc.getCurrency().equalsIgnoreCase(targetCurrency))
            .collect(Collectors.groupingBy(AssetAccount::getAssetTypeId));

        // 为每个大类计算资产数据和回报率
        for (Map.Entry<Long, InvestmentCategoryAnalysisDTO> entry : categoryMap.entrySet()) {
            Long assetTypeId = entry.getKey();
            InvestmentCategoryAnalysisDTO dto = entry.getValue();

            List<AssetAccount> typeAccounts = accountsByAssetType.get(assetTypeId);
            if (typeAccounts == null || typeAccounts.isEmpty()) {
                dto.setCurrentAssets(BigDecimal.ZERO);
                dto.setLastYearEndAssets(BigDecimal.ZERO);
                dto.setReturnRate(BigDecimal.ZERO);
                continue;
            }

            // 计算当前资产和去年年底资产（需要按账户货币转换）
            BigDecimal currentAssets = BigDecimal.ZERO;
            BigDecimal lastYearEndAssets = BigDecimal.ZERO;
            BigDecimal totalPrincipalPayment = BigDecimal.ZERO; // 房地产的本金还款总额

            for (AssetAccount account : typeAccounts) {
                // 检查是否为房地产账户且有关联房贷
                boolean isRealEstateWithMortgage = "REAL_ESTATE".equals(account.getAssetType().getType())
                    && account.getLinkedLiabilityAccountId() != null;

                if (isRealEstateWithMortgage) {
                    // 房地产账户：计算净资产（房产 - 房贷）
                    LiabilityAccount mortgageAccount = liabilityAccountRepository.findById(account.getLinkedLiabilityAccountId()).orElse(null);
                    if (mortgageAccount != null) {
                        // 查询当前房产价值（修复：使用指定日期）
                        BigDecimal currentRealEstateValue = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(account.getId()), currentDate);
                        if (currentRealEstateValue == null) currentRealEstateValue = BigDecimal.ZERO;

                        // 查询当前房贷余额（修复：使用指定日期）
                        BigDecimal currentMortgage = liabilityRecordRepository.findLatestByAccountIdBeforeOrOnDate(mortgageAccount.getId(), currentDate)
                            .map(LiabilityRecord::getOutstandingBalance)
                            .orElse(BigDecimal.ZERO);

                        // 计算当前净资产
                        BigDecimal currentNetWorth = currentRealEstateValue.subtract(currentMortgage);
                        // 只有选择"All"时才转换为USD
                        if ("All".equalsIgnoreCase(targetCurrency)) {
                            currentNetWorth = convertCurrency(currentNetWorth, account.getCurrency(), "USD", currentDate);
                        }
                        currentAssets = currentAssets.add(currentNetWorth);

                        // 查询去年年底房产价值
                        BigDecimal lastYearRealEstateValue = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(account.getId()), lastYearEndDate);
                        if (lastYearRealEstateValue == null) lastYearRealEstateValue = BigDecimal.ZERO;

                        // 查询去年年底房贷余额
                        BigDecimal lastYearMortgage = liabilityRecordRepository.findLatestByAccountIdBeforeOrOnDate(mortgageAccount.getId(), lastYearEndDate)
                            .map(LiabilityRecord::getOutstandingBalance)
                            .orElse(BigDecimal.ZERO);

                        // 计算去年净资产
                        BigDecimal lastYearNetWorth = lastYearRealEstateValue.subtract(lastYearMortgage);
                        // 只有选择"All"时才转换为USD
                        if ("All".equalsIgnoreCase(targetCurrency)) {
                            lastYearNetWorth = convertCurrency(lastYearNetWorth, account.getCurrency(), "USD", lastYearEndDate);
                        }
                        lastYearEndAssets = lastYearEndAssets.add(lastYearNetWorth);

                        // 计算本金还款（负债减少部分）= 去年房贷 - 今年房贷
                        BigDecimal principalPayment = lastYearMortgage.subtract(currentMortgage);
                        // 只有选择"All"时才转换为USD
                        if ("All".equalsIgnoreCase(targetCurrency)) {
                            principalPayment = convertCurrency(principalPayment, mortgageAccount.getCurrency(), "USD", currentDate);
                        }
                        totalPrincipalPayment = totalPrincipalPayment.add(principalPayment);
                    }
                } else {
                    // 普通投资账户：直接使用资产记录
                    // 修复：使用指定日期的资产记录，而不是最新记录
                    BigDecimal accountCurrentAsset = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(account.getId()), currentDate);
                    if (accountCurrentAsset != null && accountCurrentAsset.compareTo(BigDecimal.ZERO) > 0) {
                        // 只有选择"All"时才转换为USD
                        BigDecimal convertedAmount = "All".equalsIgnoreCase(targetCurrency)
                            ? convertCurrency(accountCurrentAsset, account.getCurrency(), "USD", currentDate)
                            : accountCurrentAsset;
                        currentAssets = currentAssets.add(convertedAmount);
                    }

                    // 查询去年年底资产
                    BigDecimal accountLastYearAsset = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(account.getId()), lastYearEndDate);
                    if (accountLastYearAsset != null && accountLastYearAsset.compareTo(BigDecimal.ZERO) > 0) {
                        // 只有选择"All"时才转换为USD
                        BigDecimal convertedAmount = "All".equalsIgnoreCase(targetCurrency)
                            ? convertCurrency(accountLastYearAsset, account.getCurrency(), "USD", lastYearEndDate)
                            : accountLastYearAsset;
                        lastYearEndAssets = lastYearEndAssets.add(convertedAmount);
                    }
                }
            }

            dto.setCurrentAssets(currentAssets);
            dto.setLastYearEndAssets(lastYearEndAssets);

            // 对于房地产大类，将本金还款加到净投入中
            // 本金还款 = 去年房贷余额 - 今年房贷余额（这部分算作投资）
            if (totalPrincipalPayment.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal updatedNetDeposits = dto.getNetDeposits().add(totalPrincipalPayment);
                dto.setNetDeposits(updatedNetDeposits);
            }

            // 计算投资回报
            BigDecimal netDeposits = dto.getNetDeposits();
            BigDecimal returns = currentAssets.subtract(lastYearEndAssets).subtract(netDeposits);
            dto.setReturns(returns);

            // 计算投资回报率
            // 规则：净投入为正时，分母=期初资产+净投入；净投入为负或0时，分母=期初资产
            BigDecimal denominator = netDeposits.compareTo(BigDecimal.ZERO) > 0
                ? lastYearEndAssets.add(netDeposits)  // 增加了本金
                : lastYearEndAssets;                   // 取出了钱或无变化
            if (denominator.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal returnRate = returns.divide(denominator, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)); // 转换为百分比
                dto.setReturnRate(returnRate);
            } else {
                dto.setReturnRate(BigDecimal.ZERO);
            }
        }
    }

    /**
     * 货币转换辅助方法
     * @param amount 原始金额
     * @param fromCurrency 源货币
     * @param toCurrency 目标货币
     * @param date 汇率日期
     * @return 转换后的金额
     */
    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency, LocalDate date) {
        // 如果源货币和目标货币相同，直接返回原金额
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        // 获取源货币到USD的汇率
        BigDecimal fromRate = exchangeRateService.getExchangeRate(fromCurrency, date);
        // 获取目标货币到USD的汇率
        BigDecimal toRate = exchangeRateService.getExchangeRate(toCurrency, date);

        // 转换逻辑：
        // 1. 先将源货币转换为USD：amount * fromRate
        // 2. 再将USD转换为目标货币：(amount * fromRate) / toRate
        BigDecimal amountInUsd = amount.multiply(fromRate);
        if (toRate.compareTo(BigDecimal.ZERO) == 0) {
            return amountInUsd; // 如果目标货币汇率为0，返回USD金额
        }
        return amountInUsd.divide(toRate, 2, RoundingMode.HALF_UP);
    }

    /**
     * 根据familyId获取所有用户ID
     */
    private List<Long> getUserIdsByFamilyId(Long familyId) {
        // 直接从users表查询该家庭的所有用户ID
        return userRepository.findAll().stream()
            .filter(user -> familyId.equals(user.getFamilyId()))
            .map(User::getId)
            .collect(Collectors.toList());
    }

    /**
     * 获取年度账户投资分析（按大类筛选）
     * @param currency 货币筛选条件："USD", "CNY", 或 "All"
     */
    public List<InvestmentAccountAnalysisDTO> getAnnualByAccount(Long familyId, Integer year, Long assetTypeId, String currency) {
        // 获取该家庭的所有用户ID
        List<Long> userIds = getUserIdsByFamilyId(familyId);
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有投资账户（筛选条件：家庭用户 + 可选的资产类型 + 货币筛选）
        List<AssetAccount> accounts;
        if (assetTypeId != null) {
            accounts = assetAccountRepository.findByUserIdInAndIsActiveTrue(userIds).stream()
                .filter(acc -> acc.getAssetType() != null && acc.getAssetType().getIsInvestment())
                .filter(acc -> assetTypeId.equals(acc.getAssetTypeId()))
                // 货币筛选：如果不是"All"，则只包含指定货币的账户
                .filter(acc -> "All".equalsIgnoreCase(currency) || acc.getCurrency().equalsIgnoreCase(currency))
                .collect(Collectors.toList());
        } else {
            accounts = assetAccountRepository.findByUserIdInAndIsActiveTrue(userIds).stream()
                .filter(acc -> acc.getAssetType() != null && acc.getAssetType().getIsInvestment())
                // 货币筛选：如果不是"All"，则只包含指定货币的账户
                .filter(acc -> "All".equalsIgnoreCase(currency) || acc.getCurrency().equalsIgnoreCase(currency))
                .collect(Collectors.toList());
        }

        // 查询所有用户信息
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> userNameMap = users.stream()
            .collect(Collectors.toMap(User::getId, User::getFullName));

        // 初始化所有账户的DTO
        Map<Long, InvestmentAccountAnalysisDTO> accountMap = new HashMap<>();
        for (AssetAccount account : accounts) {
            InvestmentAccountAnalysisDTO dto = new InvestmentAccountAnalysisDTO();
            dto.setAccountId(account.getId());
            dto.setAccountName(account.getAccountName());
            dto.setCategoryName(account.getAssetType().getChineseName());
            dto.setUserName(userNameMap.getOrDefault(account.getUserId(), "Unknown"));
            dto.setCurrency(account.getCurrency());
            dto.setTotalDeposits(BigDecimal.ZERO);
            dto.setTotalWithdrawals(BigDecimal.ZERO);
            dto.setNetDeposits(BigDecimal.ZERO);
            accountMap.put(account.getId(), dto);
        }

        // 获取交易记录并统计
        String yearPattern = year + "-%";
        List<InvestmentTransaction> transactions;
        if (assetTypeId != null) {
            transactions = transactionRepository.findByFamilyIdAndAssetTypeIdAndYearPattern(familyId, assetTypeId, yearPattern);
        } else {
            transactions = transactionRepository.findByFamilyIdAndYearPattern(familyId, yearPattern);
        }

        for (InvestmentTransaction tx : transactions) {
            Long accountId = tx.getAccountId();
            InvestmentAccountAnalysisDTO dto = accountMap.get(accountId);

            if (dto != null) {
                BigDecimal amount = tx.getAmount();

                // 只有选择"All"时才进行货币转换（折算为USD）
                String accountCurrency = tx.getAccount().getCurrency();
                if ("All".equalsIgnoreCase(currency)) {
                    LocalDate transactionDate = LocalDate.parse(tx.getTransactionPeriod() + "-01");
                    amount = convertCurrency(amount, accountCurrency, "USD", transactionDate);
                }
                // 选择单一货币时，使用原始金额（不转换）

                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    dto.setTotalDeposits(dto.getTotalDeposits().add(amount));
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    dto.setTotalWithdrawals(dto.getTotalWithdrawals().add(amount));
                }
            }
        }

        // 计算净投入
        accountMap.values().forEach(dto ->
            dto.setNetDeposits(dto.getTotalDeposits().subtract(dto.getTotalWithdrawals()))
        );

        // 计算投资回报率
        calculateAccountReturnRates(familyId, year, currency, accountMap);

        // 过滤逻辑：显示有意义的账户
        // 1. 当前资产 > 0，或
        // 2. 上一年有值（lastYearEndAssets > 0），或
        // 3. 本年有交易（netDeposits != 0）
        List<InvestmentAccountAnalysisDTO> result = accountMap.values().stream()
            .filter(dto -> {
                boolean hasCurrentAssets = dto.getCurrentAssets() != null && dto.getCurrentAssets().compareTo(BigDecimal.ZERO) > 0;
                boolean hadLastYearAssets = dto.getLastYearEndAssets() != null && dto.getLastYearEndAssets().compareTo(BigDecimal.ZERO) > 0;
                boolean hasTransactions = dto.getNetDeposits() != null && dto.getNetDeposits().compareTo(BigDecimal.ZERO) != 0;

                return hasCurrentAssets || hadLastYearAssets || hasTransactions;
            })
            .sorted((a, b) -> b.getCurrentAssets().compareTo(a.getCurrentAssets()))
            .collect(Collectors.toList());

        return result;
    }

    /**
     * 计算账户投资回报率
     */
    private void calculateAccountReturnRates(Long familyId, Integer year, String targetCurrency, Map<Long, InvestmentAccountAnalysisDTO> accountMap) {
        LocalDate currentDate = LocalDate.of(year, 12, 31);  // 修复：使用选中年份的年末
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        // 获取所有账户信息以获取货币类型
        List<Long> accountIds = new ArrayList<>(accountMap.keySet());
        List<AssetAccount> accounts = assetAccountRepository.findAllById(accountIds);
        Map<Long, AssetAccount> accountById = accounts.stream()
            .collect(Collectors.toMap(AssetAccount::getId, acc -> acc));

        for (Map.Entry<Long, InvestmentAccountAnalysisDTO> entry : accountMap.entrySet()) {
            Long accountId = entry.getKey();
            InvestmentAccountAnalysisDTO dto = entry.getValue();

            AssetAccount account = accountById.get(accountId);
            if (account == null) {
                continue;
            }

            // 检查是否为房产账户（通过资产类型判断）
            boolean isRealEstate = account.getAssetType() != null
                && "REAL_ESTATE".equals(account.getAssetType().getType());

            if (isRealEstate) {
                // 房产账户：使用特殊计算逻辑（净资产 = 房产 - 房贷）
                calculateRealEstateReturns(familyId, year, targetCurrency, dto, account);
            } else {
                // 普通投资账户：使用标准计算逻辑
                // 查询当前资产（原始货币金额）修复：使用指定日期
                BigDecimal currentAssetsOriginal = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(accountId), currentDate);
                if (currentAssetsOriginal == null) {
                    currentAssetsOriginal = BigDecimal.ZERO;
                }

                // 查询去年年底资产（原始货币金额）
                BigDecimal lastYearEndAssetsOriginal = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(accountId), lastYearEndDate);
                if (lastYearEndAssetsOriginal == null) {
                    lastYearEndAssetsOriginal = BigDecimal.ZERO;
                }

                // 只有选择"All"时才转换为USD
                BigDecimal currentAssets = "All".equalsIgnoreCase(targetCurrency)
                    ? convertCurrency(currentAssetsOriginal, account.getCurrency(), "USD", currentDate)
                    : currentAssetsOriginal;
                BigDecimal lastYearEndAssets = "All".equalsIgnoreCase(targetCurrency)
                    ? convertCurrency(lastYearEndAssetsOriginal, account.getCurrency(), "USD", lastYearEndDate)
                    : lastYearEndAssetsOriginal;

                dto.setCurrentAssets(currentAssets);
                dto.setLastYearEndAssets(lastYearEndAssets);

                // 计算投资回报（使用选择的货币或原始货币）
                BigDecimal netDeposits = dto.getNetDeposits();
                BigDecimal returns = currentAssets.subtract(lastYearEndAssets).subtract(netDeposits);
                dto.setReturns(returns);

                // 为了兼容性，将当前货币的值也存储到USD字段（用于总计）
                dto.setCurrentAssetsUsd(currentAssets);
                dto.setLastYearEndAssetsUsd(lastYearEndAssets);
                dto.setNetDepositsUsd(netDeposits);
                dto.setReturnsUsd(returns);

                // 计算投资回报率
                // 规则：净投入为正时，分母=期初资产+净投入；净投入为负或0时，分母=期初资产
                BigDecimal denominator = netDeposits.compareTo(BigDecimal.ZERO) > 0
                    ? lastYearEndAssets.add(netDeposits)  // 增加了本金
                    : lastYearEndAssets;                   // 取出了钱或无变化
                if (denominator.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal returnRate = returns.divide(denominator, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    dto.setReturnRate(returnRate);
                } else {
                    dto.setReturnRate(BigDecimal.ZERO);
                }
            }
        }
    }

    /**
     * 获取账户月度趋势
     */
    public AccountMonthlyTrendResponseDTO getAccountMonthlyTrend(Long accountId, Integer year) {
        // 获取账户信息
        AssetAccount account = assetAccountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return null; // 账户不存在，返回null
        }

        // 检查是否为房地产账户
        // 房地产账户的净资产是计算值（房产 - 房贷），没有月度交易记录，不支持月度趋势
        boolean isRealEstate = account.getAssetType() != null
            && "REAL_ESTATE".equals(account.getAssetType().getType());

        if (isRealEstate) {
            return null; // 房地产账户不支持月度趋势，返回null
        }

        String yearPattern = year + "-%";
        List<InvestmentTransaction> transactions = transactionRepository.findByAccountIdAndYearPattern(accountId, yearPattern);

        // 初始化12个月的数据
        Map<Integer, InvestmentMonthlyTrendDTO> monthMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            InvestmentMonthlyTrendDTO dto = new InvestmentMonthlyTrendDTO();
            dto.setMonth(i);
            dto.setPeriod(String.format("%d-%02d", year, i));
            dto.setDeposits(BigDecimal.ZERO);
            dto.setWithdrawals(BigDecimal.ZERO);
            monthMap.put(i, dto);
        }

        // 统计每个月的投入和取出，并获取账户信息
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        String accountName = "";

        for (InvestmentTransaction tx : transactions) {
            if (accountName.isEmpty()) {
                accountName = tx.getAccount().getAccountName();
            }

            String period = tx.getTransactionPeriod();
            int month = Integer.parseInt(period.substring(5, 7));

            InvestmentMonthlyTrendDTO dto = monthMap.get(month);
            if (dto != null) {
                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    dto.setDeposits(dto.getDeposits().add(tx.getAmount()));
                    totalDeposits = totalDeposits.add(tx.getAmount());
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    dto.setWithdrawals(dto.getWithdrawals().add(tx.getAmount()));
                    totalWithdrawals = totalWithdrawals.add(tx.getAmount());
                }
            }
        }

        // 计算净投入
        BigDecimal netDeposits = totalDeposits.subtract(totalWithdrawals);

        // 查询资产数据（原始货币金额）
        LocalDate currentDate = LocalDate.of(year, 12, 31);  // 修复：使用选中年份的年末
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        // 修复：使用指定日期的资产记录，而不是最新记录
        BigDecimal currentAssets = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(accountId), currentDate);
        if (currentAssets == null) {
            currentAssets = BigDecimal.ZERO;
        }

        BigDecimal lastYearEndAssets = assetRecordRepository.sumAmountByAccountIdsAsOfDate(List.of(accountId), lastYearEndDate);
        if (lastYearEndAssets == null) {
            lastYearEndAssets = BigDecimal.ZERO;
        }

        // 计算投资回报和回报率（原始货币）
        BigDecimal returns = currentAssets.subtract(lastYearEndAssets).subtract(netDeposits);
        BigDecimal returnRate = BigDecimal.ZERO;
        BigDecimal denominator = lastYearEndAssets.add(netDeposits);
        if (denominator.compareTo(BigDecimal.ZERO) != 0) {
            returnRate = returns.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // 构建响应对象
        AccountMonthlyTrendResponseDTO response = new AccountMonthlyTrendResponseDTO();
        response.setAccountName(accountName);
        response.setCurrency(account.getCurrency());
        response.setCurrentAssets(currentAssets);
        response.setLastYearEndAssets(lastYearEndAssets);
        response.setNetDeposits(netDeposits);
        response.setReturns(returns);
        response.setReturnRate(returnRate);

        // 按月份排序
        List<InvestmentMonthlyTrendDTO> monthlyData = monthMap.values().stream()
            .sorted(Comparator.comparing(InvestmentMonthlyTrendDTO::getMonth))
            .collect(Collectors.toList());
        response.setMonthlyData(monthlyData);

        return response;
    }

    /**
     * 查找房产对应的房贷账户
     * 优先使用数据库关联字段，如果没有设置关联则返回null
     */
    private LiabilityAccount findMortgageByRealEstateAccount(AssetAccount realEstateAccount) {
        // 如果资产账户设置了关联的负债账户ID，直接查询
        if (realEstateAccount.getLinkedLiabilityAccountId() != null) {
            return liabilityAccountRepository.findById(realEstateAccount.getLinkedLiabilityAccountId())
                .orElse(null);
        }

        // 如果没有设置关联，返回null（需要用户在页面上手动建立关联）
        return null;
    }

    /**
     * 获取指定期间内的房贷还款总额（从支出记录中获取）
     * 支出分类：大类"住" -> 子分类"租房还贷"
     */
    private BigDecimal getMortgagePaymentFromExpense(Long familyId, Integer year, String targetCurrency) {
        // 查找"住"大类
        var housingCategory = expenseCategoryMajorRepository.findByCode("HOUSING")
            .orElse(null);

        if (housingCategory == null) {
            return BigDecimal.ZERO;
        }

        // 查找"租房还贷"子分类
        var mortgageCategory = expenseCategoryMinorRepository.findByMajorCategoryIdAndName(
            housingCategory.getId(), "租房还贷"
        ).orElse(null);

        if (mortgageCategory == null) {
            return BigDecimal.ZERO;
        }

        // 查询该年度的所有房贷支出记录
        String startPeriod = year + "-01";
        String endPeriod = year + "-12";
        List<ExpenseRecord> expenseRecords = expenseRecordRepository.findByFamilyIdAndPeriodRange(
            familyId, startPeriod, endPeriod
        );

        // 过滤出房贷支出并汇总
        BigDecimal totalPayment = BigDecimal.ZERO;
        for (ExpenseRecord record : expenseRecords) {
            if (mortgageCategory.getId().equals(record.getMinorCategoryId())) {
                // 货币转换
                BigDecimal amount = record.getAmount();
                LocalDate expenseDate = LocalDate.parse(record.getExpensePeriod() + "-01");
                amount = convertCurrency(amount, record.getCurrency(), targetCurrency, expenseDate);
                totalPayment = totalPayment.add(amount);
            }
        }

        return totalPayment;
    }

    /**
     * 计算房地产投资的净投入和回报
     * 逻辑：
     * 1. 净资产 = 房产资产 - 房贷负债
     * 2. 本金还款 = 去年房贷 - 今年房贷（负债减少部分）
     * 3. 利息支出 = 总还款额 - 本金还款
     * 4. 净投入 = 本金还款（利息不算投入）
     * 5. 投资回报 = 当前净资产 - 去年净资产 - 净投入
     */
    private void calculateRealEstateReturns(Long familyId, Integer year, String targetCurrency,
                                           InvestmentAccountAnalysisDTO dto, AssetAccount realEstateAccount) {
        // 查找对应的房贷账户（使用数据库关联字段）
        LiabilityAccount mortgageAccount = findMortgageByRealEstateAccount(realEstateAccount);
        if (mortgageAccount == null) {
            // 如果没有对应的房贷账户，按普通资产处理（不做特殊处理）
            return;
        }

        LocalDate currentDate = LocalDate.of(year, 12, 31);  // 修复：使用选中年份的年末
        LocalDate lastYearEndDate = LocalDate.of(year - 1, 12, 31);

        // 1. 查询房产资产价值（原始货币）修复：使用指定日期
        BigDecimal currentRealEstateValue = assetRecordRepository.sumAmountByAccountIdsAsOfDate(
            List.of(realEstateAccount.getId()), currentDate
        );
        if (currentRealEstateValue == null) {
            currentRealEstateValue = BigDecimal.ZERO;
        }

        BigDecimal lastYearRealEstateValue = assetRecordRepository.sumAmountByAccountIdsAsOfDate(
            List.of(realEstateAccount.getId()), lastYearEndDate
        );
        if (lastYearRealEstateValue == null) {
            lastYearRealEstateValue = BigDecimal.ZERO;
        }

        // 2. 查询房贷负债（原始货币）修复：使用指定日期
        var currentMortgageRecord = liabilityRecordRepository.findLatestByAccountIdBeforeOrOnDate(
            mortgageAccount.getId(), currentDate
        );
        BigDecimal currentMortgage = currentMortgageRecord.map(LiabilityRecord::getOutstandingBalance)
            .orElse(BigDecimal.ZERO);

        var lastYearMortgageRecord = liabilityRecordRepository.findLatestByAccountIdBeforeOrOnDate(
            mortgageAccount.getId(), lastYearEndDate
        );
        BigDecimal lastYearMortgage = lastYearMortgageRecord.map(LiabilityRecord::getOutstandingBalance)
            .orElse(BigDecimal.ZERO);

        // 3. 计算净资产（原始货币）
        BigDecimal currentNetWorth = currentRealEstateValue.subtract(currentMortgage);
        BigDecimal lastYearNetWorth = lastYearRealEstateValue.subtract(lastYearMortgage);

        // 4. 计算本金还款（负债减少部分，原始货币）
        BigDecimal principalPayment = lastYearMortgage.subtract(currentMortgage);

        // 5. 净投入 = 本金还款（利息不算投入，算支出）
        BigDecimal netDeposits = principalPayment;

        // 6. 投资回报 = 当前净资产 - 去年净资产 - 净投入
        BigDecimal returns = currentNetWorth.subtract(lastYearNetWorth).subtract(netDeposits);

        // 7. 设置DTO字段（所有金额都使用原始货币）
        dto.setCurrentAssets(currentNetWorth);  // 使用净资产而非房产总值
        dto.setLastYearEndAssets(lastYearNetWorth);
        dto.setNetDeposits(netDeposits);  // 只算本金
        dto.setReturns(returns);

        // 8. 计算投资回报率
        BigDecimal denominator = lastYearNetWorth.add(netDeposits);
        if (denominator.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal returnRate = returns.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            dto.setReturnRate(returnRate);
        } else {
            dto.setReturnRate(BigDecimal.ZERO);
        }

        // 9. 为了兼容性，USD字段需要进行货币转换
        // 注意：不同时间点的金额需要用对应时间点的汇率转换
        BigDecimal currentNetWorthUsd = convertCurrency(currentNetWorth, realEstateAccount.getCurrency(),
                                                        "USD", currentDate);
        BigDecimal lastYearNetWorthUsd = convertCurrency(lastYearNetWorth, realEstateAccount.getCurrency(),
                                                         "USD", lastYearEndDate);
        BigDecimal netDepositsUsd = convertCurrency(netDeposits, realEstateAccount.getCurrency(),
                                                    "USD", currentDate);

        // 投资回报的USD值应该重新计算，而不是直接转换原始货币的returns
        // 因为汇率变化会影响不同时间点的金额
        BigDecimal returnsUsd = currentNetWorthUsd.subtract(lastYearNetWorthUsd).subtract(netDepositsUsd);

        dto.setCurrentAssetsUsd(currentNetWorthUsd);
        dto.setLastYearEndAssetsUsd(lastYearNetWorthUsd);
        dto.setNetDepositsUsd(netDepositsUsd);
        dto.setReturnsUsd(returnsUsd);
    }
}
