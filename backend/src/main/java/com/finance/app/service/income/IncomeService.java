package com.finance.app.service.income;

import com.finance.app.dto.income.*;
import com.finance.app.model.*;
import com.finance.app.repository.*;
import com.finance.app.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收入管理Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeCategoryMajorRepository majorCategoryRepository;
    private final IncomeCategoryMinorRepository minorCategoryRepository;
    private final IncomeRecordRepository incomeRecordRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final ExchangeRateService exchangeRateService;

    // ==================== 分类管理 ====================

    /**
     * 获取所有大类
     */
    public List<IncomeCategoryMajor> getAllMajorCategories() {
        return majorCategoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * 获取指定大类的所有小类
     */
    public List<IncomeCategoryMinor> getMinorCategoriesByMajor(Long majorCategoryId) {
        return minorCategoryRepository.findByMajorCategoryIdAndIsActiveTrue(majorCategoryId);
    }

    /**
     * 根据家庭ID和大类ID获取小类（包含公共分类和该家庭用户的私有分类）
     */
    public List<IncomeCategoryMinor> getMinorCategoriesByFamilyAndMajor(Long familyId, Long majorCategoryId) {
        return minorCategoryRepository.findByMajorCategoryIdAndFamilyIdAndIsActiveTrue(majorCategoryId, familyId);
    }

    /**
     * 获取所有分类（大类+小类嵌套结构）
     */
    public List<IncomeCategoryDTO> getAllCategories() {
        List<IncomeCategoryMajor> majors = majorCategoryRepository.findAllByOrderByDisplayOrderAsc();

        return majors.stream()
            .map(major -> {
                // 获取该大类下的所有小类
                List<IncomeCategoryMinor> minors = minorCategoryRepository
                    .findByMajorCategoryId(major.getId());

                // 构建小类DTO列表
                List<IncomeCategoryDTO.MinorCategoryDTO> minorDTOs = minors.stream()
                    .<IncomeCategoryDTO.MinorCategoryDTO>map(minor -> IncomeCategoryDTO.MinorCategoryDTO.builder()
                        .id(minor.getId())
                        .majorCategoryId(major.getId())
                        .name(minor.getName())
                        .chineseName(minor.getChineseName())
                        .isActive(minor.getIsActive())
                        .displayOrder(null) // 小类暂无排序字段
                        .recordCount(0) // TODO: 后续可以添加记录数统计
                        .build())
                    .collect(Collectors.toList());

                // 构建大类DTO（包含小类列表）
                return IncomeCategoryDTO.builder()
                    .id(major.getId())
                    .name(major.getName())
                    .chineseName(major.getChineseName())
                    .icon(major.getIcon())
                    .color(major.getColor())
                    .displayOrder(major.getDisplayOrder())
                    .isActive(major.getIsActive())
                    .minorCategories(minorDTOs)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 创建小类
     */
    @Transactional
    public IncomeCategoryMinor createMinorCategory(Long majorCategoryId, String name, String chineseName, Long userId, String description) {
        // 检查大类是否存在
        majorCategoryRepository.findById(majorCategoryId)
            .orElseThrow(() -> new IllegalArgumentException("大类不存在"));

        IncomeCategoryMinor minor = new IncomeCategoryMinor();
        minor.setMajorCategoryId(majorCategoryId);
        minor.setName(name);
        minor.setChineseName(chineseName);
        minor.setUserId(userId);
        minor.setDescription(description);
        minor.setIsActive(true);

        return minorCategoryRepository.save(minor);
    }

    /**
     * 更新小类
     */
    @Transactional
    public IncomeCategoryMinor updateMinorCategory(Long id, String name, String chineseName, Long userId, String description) {
        IncomeCategoryMinor minor = minorCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("小类不存在"));

        minor.setName(name);
        minor.setChineseName(chineseName);
        minor.setUserId(userId);
        minor.setDescription(description);

        return minorCategoryRepository.save(minor);
    }

    /**
     * 停用小类（软删除）
     */
    @Transactional
    public void disableMinorCategory(Long id) {
        IncomeCategoryMinor minor = minorCategoryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("小类不存在"));

        // 检查是否有关联记录
        List<IncomeRecord> records = incomeRecordRepository.findByFamilyIdAndMajorCategoryIdAndPeriod(
            null, id, null);

        if (!records.isEmpty()) {
            // 有记录，只能停用
            minor.setIsActive(false);
            minorCategoryRepository.save(minor);
            log.info("小类{}已停用（有关联记录）", id);
        } else {
            // 无记录，可以直接删除
            minorCategoryRepository.deleteById(id);
            log.info("小类{}已删除（无关联记录）", id);
        }
    }

    // ==================== 收入记录管理 ====================

    /**
     * 创建收入记录
     */
    @Transactional
    public IncomeRecordDTO createIncomeRecord(CreateIncomeRecordRequest request) {
        // 检查唯一性约束
        // 注意：投资收益类别不允许手动创建
        IncomeCategoryMajor major = majorCategoryRepository.findById(request.getMajorCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("大类不存在"));

        if ("Investment".equals(major.getName())) {
            throw new IllegalArgumentException("投资收益由系统自动计算，不能手动录入");
        }

        IncomeRecord record = buildIncomeRecord(request);
        IncomeRecord saved = incomeRecordRepository.save(record);

        return toDTO(saved);
    }

    /**
     * 更新收入记录
     */
    @Transactional
    public IncomeRecordDTO updateIncomeRecord(Long id, UpdateIncomeRecordRequest request) {
        IncomeRecord record = incomeRecordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("收入记录不存在"));

        // 投资收益不允许更新
        IncomeCategoryMajor major = majorCategoryRepository.findById(record.getMajorCategoryId())
            .orElse(null);
        if (major != null && "Investment".equals(major.getName())) {
            throw new IllegalArgumentException("投资收益由系统自动计算，不能手动更新");
        }

        record.setAssetAccountId(request.getAssetAccountId());
        record.setAmount(request.getAmount());
        record.setDescription(request.getDescription());

        // 重新计算USD金额
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
            record.getCurrency(),
            LocalDate.parse(record.getPeriod() + "-01")
        );
        record.setAmountUsd(request.getAmount().multiply(exchangeRate));

        IncomeRecord saved = incomeRecordRepository.save(record);
        return toDTO(saved);
    }

    /**
     * 批量保存收入记录
     */
    @Transactional
    public List<IncomeRecordDTO> batchSaveIncomeRecords(BatchIncomeRecordRequest request) {
        List<IncomeRecord> savedRecords = new ArrayList<>();

        for (BatchIncomeRecordRequest.IncomeRecordItem item : request.getRecords()) {
            // 检查大类
            IncomeCategoryMajor major = majorCategoryRepository.findById(item.getMajorCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("大类不存在: " + item.getMajorCategoryId()));

            // 投资收益不允许手动录入
            if ("Investment".equals(major.getName())) {
                log.warn("跳过投资收益记录，该类别由系统自动计算");
                continue;
            }

            // 检查是否已存在（家庭+用户+期间+大类+小类+币种唯一）
            IncomeRecord record;
            List<IncomeRecord> existing = incomeRecordRepository.findByFamilyIdAndPeriod(
                request.getFamilyId(),
                request.getPeriod()
            );

            Optional<IncomeRecord> matchingRecord = existing.stream()
                .filter(r -> Objects.equals(r.getMajorCategoryId(), item.getMajorCategoryId())
                    && Objects.equals(r.getMinorCategoryId(), item.getMinorCategoryId())
                    && Objects.equals(r.getCurrency(), item.getCurrency()))
                .findFirst();

            if (matchingRecord.isPresent()) {
                // 更新现有记录
                record = matchingRecord.get();
                record.setAssetAccountId(item.getAssetAccountId());
                record.setAmount(item.getAmount());
                record.setDescription(item.getDescription());

                BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                    item.getCurrency(),
                    LocalDate.parse(request.getPeriod() + "-01")
                );
                record.setAmountUsd(item.getAmount().multiply(exchangeRate));
            } else {
                // 创建新记录
                record = new IncomeRecord();
                record.setFamilyId(request.getFamilyId());
                record.setAssetAccountId(item.getAssetAccountId());
                record.setMajorCategoryId(item.getMajorCategoryId());
                record.setMinorCategoryId(item.getMinorCategoryId());
                record.setPeriod(request.getPeriod());
                record.setAmount(item.getAmount());
                record.setCurrency(item.getCurrency());
                record.setDescription(item.getDescription());

                // 计算USD金额
                BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                    item.getCurrency(),
                    LocalDate.parse(request.getPeriod() + "-01")
                );
                record.setAmountUsd(item.getAmount().multiply(exchangeRate));
            }

            savedRecords.add(incomeRecordRepository.save(record));
        }

        return savedRecords.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * 查询收入记录（按期间）
     */
    public List<IncomeRecordDTO> getIncomeRecordsByPeriod(Long familyId, String period) {
        List<IncomeRecord> records = incomeRecordRepository.findByFamilyIdAndPeriod(familyId, period);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 查询收入记录（按期间范围）
     */
    public List<IncomeRecordDTO> getIncomeRecordsByPeriodRange(
        Long familyId,
        String startPeriod,
        String endPeriod
    ) {
        List<IncomeRecord> records = incomeRecordRepository
            .findByFamilyIdAndPeriodBetween(familyId, startPeriod, endPeriod);
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 删除收入记录
     */
    @Transactional
    public void deleteIncomeRecord(Long id) {
        IncomeRecord record = incomeRecordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("收入记录不存在"));

        // 投资收益不允许删除
        IncomeCategoryMajor major = majorCategoryRepository.findById(record.getMajorCategoryId())
            .orElse(null);
        if (major != null && "Investment".equals(major.getName())) {
            throw new IllegalArgumentException("投资收益由系统自动计算，不能手动删除");
        }

        incomeRecordRepository.deleteById(id);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建收入记录实体
     */
    private IncomeRecord buildIncomeRecord(CreateIncomeRecordRequest request) {
        IncomeRecord record = new IncomeRecord();
        record.setFamilyId(request.getFamilyId());
        record.setAssetAccountId(request.getAssetAccountId());
        record.setMajorCategoryId(request.getMajorCategoryId());
        record.setMinorCategoryId(request.getMinorCategoryId());
        record.setPeriod(request.getPeriod());
        record.setAmount(request.getAmount());
        record.setCurrency(request.getCurrency());
        record.setDescription(request.getDescription());

        // 计算USD金额
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
            request.getCurrency(),
            LocalDate.parse(request.getPeriod() + "-01")
        );
        record.setAmountUsd(request.getAmount().multiply(exchangeRate));

        return record;
    }

    /**
     * 转换为DTO
     */
    private IncomeRecordDTO toDTO(IncomeRecord record) {
        IncomeCategoryMajor major = majorCategoryRepository.findById(record.getMajorCategoryId())
            .orElse(null);
        IncomeCategoryMinor minor = record.getMinorCategoryId() != null
            ? minorCategoryRepository.findById(record.getMinorCategoryId()).orElse(null)
            : null;
        AssetAccount assetAccount = record.getAssetAccountId() != null
            ? assetAccountRepository.findById(record.getAssetAccountId()).orElse(null)
            : null;

        String[] periodParts = record.getPeriod().split("-");
        Integer year = Integer.parseInt(periodParts[0]);
        Integer month = Integer.parseInt(periodParts[1]);

        return IncomeRecordDTO.builder()
            .id(record.getId())
            .familyId(record.getFamilyId())
            .assetAccountId(record.getAssetAccountId())
            .assetAccountName(assetAccount != null ? assetAccount.getAccountName() : null)
            .period(record.getPeriod())
            .year(year)
            .month(month)
            .majorCategoryId(record.getMajorCategoryId())
            .majorCategoryName(major != null ? major.getName() : null)
            .majorCategoryChineseName(major != null ? major.getChineseName() : null)
            .majorCategoryIcon(major != null ? major.getIcon() : null)
            .minorCategoryId(record.getMinorCategoryId())
            .minorCategoryName(minor != null ? minor.getName() : null)
            .minorCategoryChineseName(minor != null ? minor.getChineseName() : null)
            .amount(record.getAmount())
            .currency(record.getCurrency())
            .amountUsd(record.getAmountUsd())
            .description(record.getDescription())
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .build();
    }
}
