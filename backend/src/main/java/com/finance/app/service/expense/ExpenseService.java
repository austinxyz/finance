package com.finance.app.service.expense;

import com.finance.app.dto.expense.*;
import com.finance.app.model.*;
import com.finance.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支出管理Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseCategoryMajorRepository majorCategoryRepository;
    private final ExpenseCategoryMinorRepository minorCategoryRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final com.finance.app.service.DataProtectionService dataProtectionService;

    // ==================== 分类管理 ====================

    /**
     * 获取所有大类及其子分类
     */
    public List<ExpenseCategoryDTO> getAllCategories() {
        List<ExpenseCategoryMajor> majors = majorCategoryRepository.findAllByOrderBySortOrder();

        return majors.stream().map(major -> {
            List<ExpenseCategoryMinor> minors = minorCategoryRepository
                .findByMajorCategoryIdOrderBySortOrder(major.getId());

            List<ExpenseCategoryDTO.MinorCategoryDTO> minorDTOs = minors.stream()
                .map(minor -> {
                    long recordCount = minorCategoryRepository.countExpenseRecords(minor.getId());
                    return ExpenseCategoryDTO.MinorCategoryDTO.builder()
                        .id(minor.getId())
                        .majorCategoryId(minor.getMajorCategoryId())
                        .name(minor.getName())
                        .isActive(minor.getIsActive())
                        .isDefault(minor.getIsDefault())
                        .sortOrder(minor.getSortOrder())
                        .description(minor.getDescription())
                        .expenseType(minor.getExpenseType())
                        .recordCount((int) recordCount)
                        .build();
                })
                .collect(Collectors.toList());

            return ExpenseCategoryDTO.builder()
                .id(major.getId())
                .code(major.getCode())
                .name(major.getName())
                .icon(major.getIcon())
                .color(major.getColor())
                .sortOrder(major.getSortOrder())
                .isActive(major.getIsActive())
                .description(major.getDescription())
                .minorCategories(minorDTOs)
                .build();
        }).collect(Collectors.toList());
    }

    /**
     * 创建子分类
     */
    @Transactional
    public ExpenseCategoryDTO.MinorCategoryDTO createMinorCategory(CreateMinorCategoryRequest request) {
        // 检查大类是否存在
        ExpenseCategoryMajor major = majorCategoryRepository.findById(request.getMajorCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("大类不存在"));

        // 检查名称是否重复
        Optional<ExpenseCategoryMinor> existing = minorCategoryRepository
            .findByMajorCategoryIdAndName(request.getMajorCategoryId(), request.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("该大类下已存在同名子分类");
        }

        ExpenseCategoryMinor minor = new ExpenseCategoryMinor();
        minor.setMajorCategoryId(request.getMajorCategoryId());
        minor.setName(request.getName());
        minor.setSortOrder(request.getSortOrder());
        minor.setDescription(request.getDescription());
        minor.setExpenseType(request.getExpenseType());
        minor.setIsActive(true);
        minor.setIsDefault(false);

        ExpenseCategoryMinor saved = minorCategoryRepository.save(minor);

        return ExpenseCategoryDTO.MinorCategoryDTO.builder()
            .id(saved.getId())
            .majorCategoryId(saved.getMajorCategoryId())
            .name(saved.getName())
            .isActive(saved.getIsActive())
            .isDefault(saved.getIsDefault())
            .sortOrder(saved.getSortOrder())
            .description(saved.getDescription())
            .expenseType(saved.getExpenseType())
            .recordCount(0)
            .build();
    }

    /**
     * 更新子分类
     */
    @Transactional
    public ExpenseCategoryDTO.MinorCategoryDTO updateMinorCategory(Long id, CreateMinorCategoryRequest request) {
        ExpenseCategoryMinor minor = minorCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("子分类不存在"));

        // 如果修改名称，检查是否重复
        if (!minor.getName().equals(request.getName())) {
            Optional<ExpenseCategoryMinor> existing = minorCategoryRepository
                .findByMajorCategoryIdAndName(minor.getMajorCategoryId(), request.getName());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("该大类下已存在同名子分类");
            }
        }

        minor.setName(request.getName());
        minor.setSortOrder(request.getSortOrder());
        minor.setDescription(request.getDescription());
        minor.setExpenseType(request.getExpenseType());

        ExpenseCategoryMinor saved = minorCategoryRepository.save(minor);
        long recordCount = minorCategoryRepository.countExpenseRecords(saved.getId());

        return ExpenseCategoryDTO.MinorCategoryDTO.builder()
            .id(saved.getId())
            .majorCategoryId(saved.getMajorCategoryId())
            .name(saved.getName())
            .isActive(saved.getIsActive())
            .isDefault(saved.getIsDefault())
            .sortOrder(saved.getSortOrder())
            .description(saved.getDescription())
            .expenseType(saved.getExpenseType())
            .recordCount((int) recordCount)
            .build();
    }

    /**
     * 停用子分类（软删除）
     */
    @Transactional
    public void disableMinorCategory(Long id) {
        ExpenseCategoryMinor minor = minorCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("子分类不存在"));

        boolean hasRecords = minorCategoryRepository.hasExpenseRecords(id);
        if (hasRecords) {
            // 有记录，只能停用
            minor.setIsActive(false);
            minorCategoryRepository.save(minor);
            log.info("子分类{}已停用（有关联记录）", id);
        } else {
            // 无记录，可以直接删除
            minorCategoryRepository.deleteById(id);
            log.info("子分类{}已删除（无关联记录）", id);
        }
    }

    // ==================== 支出记录管理 ====================

    /**
     * 创建支出记录
     */
    @Transactional
    public ExpenseRecordDTO createExpenseRecord(CreateExpenseRecordRequest request) {
        // 检查唯一性
        Optional<ExpenseRecord> existing = expenseRecordRepository
            .findFirstByFamilyIdAndExpensePeriodAndMinorCategoryId(
                request.getFamilyId(),
                request.getExpensePeriod(),
                request.getMinorCategoryId()
            );

        if (existing.isPresent()) {
            throw new IllegalArgumentException("该期间该分类已有支出记录，请使用更新接口");
        }

        ExpenseRecord record = buildExpenseRecord(request);
        ExpenseRecord saved = expenseRecordRepository.save(record);

        return toDTO(saved);
    }

    /**
     * 根据ID获取支出记录（用于授权验证）
     */
    public ExpenseRecordDTO getExpenseRecordById(Long id) {
        ExpenseRecord record = expenseRecordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("支出记录不存在"));
        return toDTO(record);
    }

    /**
     * 更新支出记录
     */
    @Transactional
    public ExpenseRecordDTO updateExpenseRecord(Long id, UpdateExpenseRecordRequest request) {
        ExpenseRecord record = expenseRecordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("支出记录不存在"));

        record.setAmount(request.getAmount());
        record.setCurrency(request.getCurrency());
        record.setExpenseType(request.getExpenseType());
        record.setDescription(request.getDescription());

        ExpenseRecord saved = expenseRecordRepository.save(record);
        return toDTO(saved);
    }

    /**
     * 批量保存支出记录
     */
    @Transactional
    public List<ExpenseRecordDTO> batchSaveExpenseRecords(BatchExpenseRecordRequest request) {
        List<ExpenseRecord> savedRecords = new ArrayList<>();

        for (BatchExpenseRecordRequest.ExpenseRecordItem item : request.getRecords()) {
            // 查找子分类所属的大类
            ExpenseCategoryMinor minor = minorCategoryRepository.findById(item.getMinorCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("子分类不存在: " + item.getMinorCategoryId()));

            // 检查是否已存在（包含货币，支持同一分类多货币记录）
            Optional<ExpenseRecord> existing = expenseRecordRepository
                .findByFamilyIdAndExpensePeriodAndMinorCategoryIdAndCurrency(
                    request.getFamilyId(),
                    request.getExpensePeriod(),
                    item.getMinorCategoryId(),
                    item.getCurrency()
                );

            ExpenseRecord record;
            if (existing.isPresent()) {
                // 更新现有记录
                record = existing.get();
                record.setAmount(item.getAmount());
                record.setCurrency(item.getCurrency());
                record.setExpenseType(item.getExpenseType());
                record.setDescription(item.getDescription());
            } else {
                // 创建新记录
                record = new ExpenseRecord();
                record.setFamilyId(request.getFamilyId());
                record.setUserId(request.getUserId());
                record.setExpensePeriod(request.getExpensePeriod());
                record.setMinorCategoryId(item.getMinorCategoryId());
                record.setMajorCategoryId(minor.getMajorCategoryId());
                record.setAmount(item.getAmount());
                record.setCurrency(item.getCurrency());
                record.setExpenseType(item.getExpenseType());
                record.setDescription(item.getDescription());

                // 解析期间
                String[] parts = request.getExpensePeriod().split("-");
                record.setExpenseYear(Integer.parseInt(parts[0]));
                record.setExpenseMonth(Integer.parseInt(parts[1]));
            }

            savedRecords.add(expenseRecordRepository.save(record));
        }

        return savedRecords.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 查询支出记录（按期间）
     */
    public List<ExpenseRecordDTO> getExpenseRecordsByPeriod(Long familyId, String period) {
        List<ExpenseRecord> records = expenseRecordRepository.findByFamilyIdAndExpensePeriod(familyId, period);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询支出记录（按期间范围）
     */
    public List<ExpenseRecordDTO> getExpenseRecordsByPeriodRange(
        Long familyId,
        String startPeriod,
        String endPeriod
    ) {
        List<ExpenseRecord> records = expenseRecordRepository
            .findByFamilyIdAndPeriodRange(familyId, startPeriod, endPeriod);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 删除支出记录
     */
    @Transactional
    public void deleteExpenseRecord(Long id) {
        ExpenseRecord record = expenseRecordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("支出记录不存在"));

        dataProtectionService.validateDeleteOperation(record.getFamilyId(), "删除支出记录");

        expenseRecordRepository.deleteById(id);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建支出记录实体
     */
    private ExpenseRecord buildExpenseRecord(CreateExpenseRecordRequest request) {
        // 查找子分类所属的大类
        ExpenseCategoryMinor minor = minorCategoryRepository.findById(request.getMinorCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("子分类不存在"));

        ExpenseRecord record = new ExpenseRecord();
        record.setFamilyId(request.getFamilyId());
        record.setUserId(request.getUserId());
        record.setExpensePeriod(request.getExpensePeriod());
        record.setMinorCategoryId(request.getMinorCategoryId());
        record.setMajorCategoryId(minor.getMajorCategoryId());
        record.setAmount(request.getAmount());
        record.setCurrency(request.getCurrency());
        record.setExpenseType(request.getExpenseType());
        record.setDescription(request.getDescription());

        // 解析期间
        String[] parts = request.getExpensePeriod().split("-");
        record.setExpenseYear(Integer.parseInt(parts[0]));
        record.setExpenseMonth(Integer.parseInt(parts[1]));

        return record;
    }

    /**
     * 计算基准货币金额
     * 逻辑：查询exchange_rates表，优先使用月末汇率
     * CNY是基准货币，直接返回
     * USD是中间货币，需要转换为CNY (USD * 7.0固定汇率)
     * 其他货币：先转USD，再转CNY
     */
    private BigDecimal calculateBaseAmount(BigDecimal amount, String currency, String period) {
        // CNY是基准货币，直接返回
        if ("CNY".equals(currency)) {
            return amount;
        }

        // USD是中间货币，使用固定汇率转CNY
        if ("USD".equals(currency)) {
            BigDecimal usdToCny = BigDecimal.valueOf(7.0); // 固定汇率：1 USD = 7 CNY
            return amount.multiply(usdToCny);
        }

        // 其他货币：查询汇率转USD，再转CNY
        YearMonth ym = YearMonth.parse(period);
        java.time.LocalDate monthEnd = ym.atEndOfMonth();

        List<ExchangeRate> rates = exchangeRateRepository
            .findLatestRateByCurrencyAndDate(currency, monthEnd);

        if (rates.isEmpty()) {
            throw new IllegalArgumentException("找不到" + currency + "的汇率");
        }

        ExchangeRate rate = rates.get(0);

        // amount * rate_to_usd * usd_to_cny_rate
        BigDecimal usdAmount = amount.multiply(rate.getRateToUsd());
        BigDecimal usdToCny = BigDecimal.valueOf(7.0);

        return usdAmount.multiply(usdToCny);
    }

    /**
     * 转换为DTO
     */
    private ExpenseRecordDTO toDTO(ExpenseRecord record) {
        ExpenseCategoryMajor major = record.getMajorCategory();
        ExpenseCategoryMinor minor = record.getMinorCategory();

        return ExpenseRecordDTO.builder()
            .id(record.getId())
            .familyId(record.getFamilyId())
            .userId(record.getUserId())
            .expensePeriod(record.getExpensePeriod())
            .expenseYear(record.getExpenseYear())
            .expenseMonth(record.getExpenseMonth())
            .majorCategoryId(record.getMajorCategoryId())
            .majorCategoryName(major != null ? major.getName() : null)
            .majorCategoryIcon(major != null ? major.getIcon() : null)
            .minorCategoryId(record.getMinorCategoryId())
            .minorCategoryName(minor != null ? minor.getName() : null)
            .amount(record.getAmount())
            .currency(record.getCurrency())
            .expenseType(record.getExpenseType())
            .description(record.getDescription())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .build();
    }
}
