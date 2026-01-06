package com.finance.app.service.income;

import com.finance.app.model.AssetAccount;
import com.finance.app.model.IncomeCategoryMajor;
import com.finance.app.model.IncomeRecord;
import com.finance.app.model.InvestmentTransaction;
import com.finance.app.model.LiabilityAccount;
import com.finance.app.model.LiabilityRecord;
import com.finance.app.repository.AssetAccountRepository;
import com.finance.app.repository.AssetRecordRepository;
import com.finance.app.repository.IncomeCategoryMajorRepository;
import com.finance.app.repository.IncomeCategoryMinorRepository;
import com.finance.app.repository.IncomeRecordRepository;
import com.finance.app.repository.InvestmentTransactionRepository;
import com.finance.app.repository.LiabilityAccountRepository;
import com.finance.app.repository.LiabilityRecordRepository;
import com.finance.app.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * 投资收益自动计算Service
 *
 * 投资收益计算逻辑（参考投资分析部分的实现）：
 * 投资收益 = (期末投资账户余额 - 期初投资账户余额) - (投入 - 取出)
 * 其中：
 * - 投入(Deposit): 流入投资账户的资金（如退休金贡献、RSU转入等）
 * - 取出(Withdrawal): 从投资账户取出的资金
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvestmentIncomeCalculationService {

    private final AssetAccountRepository assetAccountRepository;
    private final AssetRecordRepository assetRecordRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final IncomeCategoryMajorRepository majorCategoryRepository;
    private final IncomeCategoryMinorRepository minorCategoryRepository;
    private final LiabilityAccountRepository liabilityAccountRepository;
    private final LiabilityRecordRepository liabilityRecordRepository;
    private final ExchangeRateService exchangeRateService;

    /**
     * 计算指定期间的投资收益
     *
     * @param familyId 家庭ID
     * @param period 期间 (格式: YYYY-MM)
     * @return 投资收益金额(USD)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateInvestmentIncome(Long familyId, String period) {
        try {
            // 1. 解析期间
            YearMonth yearMonth = YearMonth.parse(period);
            LocalDate periodEnd = yearMonth.atEndOfMonth();

            // 获取前一个月的最后一天作为期初日期
            YearMonth previousMonth = yearMonth.minusMonths(1);
            LocalDate previousMonthEnd = previousMonth.atEndOfMonth();

            // 2. 获取投资类账户（修改：使用AssetType.isInvestment筛选，与投资分析页面保持一致）
            List<AssetAccount> allAccounts = assetAccountRepository.findByFamilyIdAndIsActiveTrue(familyId);

            // 过滤出投资账户（所有is_investment=true的账户）
            List<AssetAccount> investmentAccounts = allAccounts.stream()
                    .filter(account -> account.getAssetType() != null && account.getAssetType().getIsInvestment())
                    .collect(java.util.stream.Collectors.toList());

            if (investmentAccounts.isEmpty()) {
                log.warn("未找到活跃的投资账户，familyId={}, period={}", familyId, period);
                return BigDecimal.ZERO;
            }

            // 3. 计算期初和期末余额（考虑房地产净资产）
            BigDecimal beginningBalance = BigDecimal.ZERO;
            BigDecimal endingBalance = BigDecimal.ZERO;

            for (AssetAccount account : investmentAccounts) {
                // 检查是否为房地产账户且有关联房贷
                boolean isRealEstateWithMortgage = "REAL_ESTATE".equals(account.getAssetType().getType())
                        && account.getLinkedLiabilityAccountId() != null;

                if (isRealEstateWithMortgage) {
                    // 房地产账户：计算净资产（房产 - 房贷）
                    LiabilityAccount mortgageAccount = liabilityAccountRepository
                            .findById(account.getLinkedLiabilityAccountId()).orElse(null);

                    if (mortgageAccount != null) {
                        // 期初净资产
                        BigDecimal beginProperty = assetRecordRepository
                                .sumAmountByAccountIdsAsOfDate(List.of(account.getId()), previousMonthEnd);
                        if (beginProperty == null) beginProperty = BigDecimal.ZERO;

                        BigDecimal beginMortgage = liabilityRecordRepository
                                .findLatestByAccountIdBeforeOrOnDate(mortgageAccount.getId(), previousMonthEnd)
                                .map(LiabilityRecord::getOutstandingBalance)
                                .orElse(BigDecimal.ZERO);

                        BigDecimal beginNetWorth = beginProperty.subtract(beginMortgage);
                        beginningBalance = beginningBalance.add(
                                convertToUSD(beginNetWorth, account.getCurrency(), previousMonthEnd));

                        // 期末净资产
                        BigDecimal endProperty = assetRecordRepository
                                .sumAmountByAccountIdsAsOfDate(List.of(account.getId()), periodEnd);
                        if (endProperty == null) endProperty = BigDecimal.ZERO;

                        BigDecimal endMortgage = liabilityRecordRepository
                                .findLatestByAccountIdBeforeOrOnDate(mortgageAccount.getId(), periodEnd)
                                .map(LiabilityRecord::getOutstandingBalance)
                                .orElse(BigDecimal.ZERO);

                        BigDecimal endNetWorth = endProperty.subtract(endMortgage);
                        endingBalance = endingBalance.add(
                                convertToUSD(endNetWorth, account.getCurrency(), periodEnd));
                    }
                } else {
                    // 普通投资账户：直接使用资产记录
                    // 期初余额: 取上个月最后一天的记录
                    BigDecimal beginAmount = assetRecordRepository
                            .sumAmountByAccountIdsAsOfDate(List.of(account.getId()), previousMonthEnd);
                    if (beginAmount != null) {
                        beginningBalance = beginningBalance.add(
                                convertToUSD(beginAmount, account.getCurrency(), previousMonthEnd));
                    }

                    // 期末余额: 取本月最后一天或之前的最近记录
                    BigDecimal endAmount = assetRecordRepository
                            .sumAmountByAccountIdsAsOfDate(List.of(account.getId()), periodEnd);
                    if (endAmount != null) {
                        endingBalance = endingBalance.add(
                                convertToUSD(endAmount, account.getCurrency(), periodEnd));
                    }
                }
            }

            log.debug("Investment balance - period: {}, beginning: {}, ending: {}",
                    period, beginningBalance, endingBalance);

            // 4. 计算市值变化
            BigDecimal marketValueChange = endingBalance.subtract(beginningBalance);

            // 5. 查询该期间的投资交易（投入和取出）
            List<InvestmentTransaction> transactions = investmentTransactionRepository
                    .findByFamilyIdAndYearPattern(familyId, period);

            BigDecimal totalDeposits = BigDecimal.ZERO;
            BigDecimal totalWithdrawals = BigDecimal.ZERO;
            LocalDate periodDate = LocalDate.parse(period + "-01");

            for (InvestmentTransaction tx : transactions) {
                BigDecimal amount = tx.getAmount();
                String currency = tx.getAccount().getCurrency();
                BigDecimal amountUsd = convertToUSD(amount, currency, periodDate);

                if (InvestmentTransaction.TransactionType.DEPOSIT.equals(tx.getTransactionType())) {
                    totalDeposits = totalDeposits.add(amountUsd);
                } else if (InvestmentTransaction.TransactionType.WITHDRAWAL.equals(tx.getTransactionType())) {
                    totalWithdrawals = totalWithdrawals.add(amountUsd);
                }
            }

            BigDecimal netDeposits = totalDeposits.subtract(totalWithdrawals);

            // 6. 计算投资收益 = 市值变化 - 净投入
            BigDecimal investmentIncome = marketValueChange.subtract(netDeposits);

            log.info("Investment income calculated - period: {}, marketChange: {}, deposits: {}, withdrawals: {}, netDeposits: {}, income: {}",
                    period, marketValueChange, totalDeposits, totalWithdrawals, netDeposits, investmentIncome);

            return investmentIncome;

        } catch (Exception e) {
            log.error("计算投资收益失败, familyId={}, period={}", familyId, period, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 转换为USD
     */
    private BigDecimal convertToUSD(BigDecimal amount, String currency, LocalDate date) {
        if ("USD".equals(currency)) {
            return amount;
        }

        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(currency, date);
        return amount.multiply(exchangeRate);
    }

    /**
     * 创建或更新投资收益记录
     *
     * @param familyId 家庭ID
     * @param period 期间
     * @param amount 投资收益金额(USD)
     */
    @Transactional
    public void createOrUpdateInvestmentIncomeRecord(Long familyId, String period, BigDecimal amount) {
        // 1. 查找Investment大类
        IncomeCategoryMajor investmentCategory = majorCategoryRepository.findByName("Investment");
        if (investmentCategory == null) {
            log.error("Investment大类不存在，无法创建投资收益记录");
            return;
        }

        // 2. 查找是否已存在该期间的投资收益记录
        List<IncomeRecord> existingRecords = incomeRecordRepository
                .findByFamilyIdAndMajorCategoryIdAndPeriod(familyId, investmentCategory.getId(), period);

        IncomeRecord record;
        if (!existingRecords.isEmpty()) {
            // 更新现有记录
            record = existingRecords.get(0);
            record.setAmount(amount);
            record.setAmountUsd(amount); // 投资收益直接以USD计算
        } else {
            // 创建新记录
            record = new IncomeRecord();
            record.setFamilyId(familyId);
            record.setMajorCategoryId(investmentCategory.getId());
            record.setMinorCategoryId(null); // 投资收益无小类
            record.setPeriod(period);
            record.setAmount(amount);
            record.setCurrency("USD");
            record.setAmountUsd(amount);
            record.setDescription("系统自动计算的投资收益");
        }

        incomeRecordRepository.save(record);
        log.info("投资收益记录已{}，familyId={}, period={}, amount={}",
                existingRecords.isEmpty() ? "创建" : "更新", familyId, period, amount);
    }
}
