package com.finance.app.service;

import com.finance.app.dto.AnnualFinancialSummaryDTO;
import com.finance.app.model.AnnualFinancialSummary;
import com.finance.app.repository.AnnualFinancialSummaryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 年度财务摘要Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnualFinancialSummaryService {

    private final AnnualFinancialSummaryRepository summaryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 获取家庭所有年度摘要
     */
    public List<AnnualFinancialSummaryDTO> getAllSummaries(Long familyId) {
        log.info("获取家庭 {} 的所有年度财务摘要", familyId);
        List<AnnualFinancialSummary> summaries = summaryRepository.findByFamilyIdOrderByYearDesc(familyId);
        return summaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取家庭指定年份的摘要
     */
    public Optional<AnnualFinancialSummaryDTO> getSummaryByYear(Long familyId, Integer year) {
        log.info("获取家庭 {} 的 {} 年度财务摘要", familyId, year);
        return summaryRepository.findByFamilyIdAndYear(familyId, year)
                .map(this::convertToDTO);
    }

    /**
     * 获取家庭指定年份范围的摘要
     */
    public List<AnnualFinancialSummaryDTO> getSummariesByYearRange(Long familyId, Integer startYear, Integer endYear) {
        log.info("获取家庭 {} 从 {} 到 {} 年的财务摘要", familyId, startYear, endYear);
        List<AnnualFinancialSummary> summaries = summaryRepository.findByFamilyIdAndYearRange(familyId, startYear, endYear);
        return summaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取最近N年的摘要
     */
    public List<AnnualFinancialSummaryDTO> getRecentYearsSummaries(Long familyId, int limit) {
        log.info("获取家庭 {} 最近 {} 年的财务摘要", familyId, limit);
        List<AnnualFinancialSummary> summaries = summaryRepository.findRecentYears(familyId, limit);
        return summaries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 计算或刷新指定年份的财务摘要
     * 调用数据库存储过程
     */
    @Transactional
    public AnnualFinancialSummaryDTO calculateAndRefreshSummary(Long familyId, Integer year) {
        log.info("计算家庭 {} 的 {} 年度财务摘要", familyId, year);

        try {
            // 调用存储过程
            entityManager.createNativeQuery("CALL sp_calculate_annual_summary(:familyId, :year)")
                    .setParameter("familyId", familyId)
                    .setParameter("year", year)
                    .executeUpdate();

            // 刷新实体管理器
            entityManager.flush();
            entityManager.clear();

            // 重新查询计算后的结果
            Optional<AnnualFinancialSummary> summary = summaryRepository.findByFamilyIdAndYear(familyId, year);

            if (summary.isPresent()) {
                log.info("成功计算家庭 {} 的 {} 年度财务摘要", familyId, year);
                return convertToDTO(summary.get());
            } else {
                log.warn("计算家庭 {} 的 {} 年度财务摘要后未找到结果", familyId, year);
                throw new RuntimeException("无法生成年度财务摘要，可能该年度没有数据");
            }
        } catch (Exception e) {
            log.error("计算家庭 {} 的 {} 年度财务摘要时发生错误", familyId, year, e);
            throw new RuntimeException("计算年度财务摘要失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量刷新多个年份的摘要
     */
    @Transactional
    public List<AnnualFinancialSummaryDTO> batchRefreshSummaries(Long familyId, List<Integer> years) {
        log.info("批量刷新家庭 {} 的年度财务摘要，年份: {}", familyId, years);

        return years.stream()
                .map(year -> calculateAndRefreshSummary(familyId, year))
                .collect(Collectors.toList());
    }

    /**
     * 删除指定年份的摘要
     */
    @Transactional
    public void deleteSummary(Long familyId, Integer year) {
        log.info("删除家庭 {} 的 {} 年度财务摘要", familyId, year);
        summaryRepository.deleteByFamilyIdAndYear(familyId, year);
    }

    /**
     * 手动创建或更新摘要（不使用存储过程）
     */
    @Transactional
    public AnnualFinancialSummaryDTO saveOrUpdateSummary(AnnualFinancialSummaryDTO dto) {
        log.info("手动保存/更新家庭 {} 的 {} 年度财务摘要", dto.getFamilyId(), dto.getYear());

        AnnualFinancialSummary summary;
        Optional<AnnualFinancialSummary> existing = summaryRepository.findByFamilyIdAndYear(dto.getFamilyId(), dto.getYear());

        if (existing.isPresent()) {
            summary = existing.get();
            updateEntityFromDTO(summary, dto);
        } else {
            summary = convertToEntity(dto);
        }

        AnnualFinancialSummary saved = summaryRepository.save(summary);
        return convertToDTO(saved);
    }

    /**
     * 转换Entity到DTO
     */
    private AnnualFinancialSummaryDTO convertToDTO(AnnualFinancialSummary entity) {
        AnnualFinancialSummaryDTO dto = new AnnualFinancialSummaryDTO();
        dto.setId(entity.getId());
        dto.setFamilyId(entity.getFamilyId());
        dto.setYear(entity.getYear());
        dto.setSummaryDate(entity.getSummaryDate());
        dto.setTotalAssets(entity.getTotalAssets());
        dto.setTotalLiabilities(entity.getTotalLiabilities());
        dto.setNetWorth(entity.getNetWorth());
        dto.setAssetBreakdown(entity.getAssetBreakdown());
        dto.setLiabilityBreakdown(entity.getLiabilityBreakdown());
        dto.setNetAssetBreakdown(entity.getNetAssetBreakdown());
        dto.setCurrency(entity.getCurrency());
        dto.setYoyAssetChange(entity.getYoyAssetChange());
        dto.setYoyLiabilityChange(entity.getYoyLiabilityChange());
        dto.setYoyNetWorthChange(entity.getYoyNetWorthChange());
        dto.setYoyAssetChangePct(entity.getYoyAssetChangePct());
        dto.setYoyLiabilityChangePct(entity.getYoyLiabilityChangePct());
        dto.setYoyNetWorthChangePct(entity.getYoyNetWorthChangePct());
        dto.setRealEstateAssets(entity.getRealEstateAssets());
        dto.setRealEstateNetWorth(entity.getRealEstateNetWorth());
        dto.setNonRealEstateNetWorth(entity.getNonRealEstateNetWorth());
        dto.setYoyRealEstateNetWorthChange(entity.getYoyRealEstateNetWorthChange());
        dto.setYoyRealEstateNetWorthChangePct(entity.getYoyRealEstateNetWorthChangePct());
        dto.setYoyNonRealEstateNetWorthChange(entity.getYoyNonRealEstateNetWorthChange());
        dto.setYoyNonRealEstateNetWorthChangePct(entity.getYoyNonRealEstateNetWorthChangePct());
        dto.setRealEstateAssetRatio(entity.getRealEstateAssetRatio());
        dto.setRealEstateNetWorthRatio(entity.getRealEstateNetWorthRatio());
        dto.setRealEstateToNetWorthRatio(entity.getRealEstateToNetWorthRatio());
        dto.setNotes(entity.getNotes());
        return dto;
    }

    /**
     * 转换DTO到Entity
     */
    private AnnualFinancialSummary convertToEntity(AnnualFinancialSummaryDTO dto) {
        AnnualFinancialSummary entity = new AnnualFinancialSummary();
        updateEntityFromDTO(entity, dto);
        return entity;
    }

    /**
     * 用DTO更新Entity
     */
    private void updateEntityFromDTO(AnnualFinancialSummary entity, AnnualFinancialSummaryDTO dto) {
        entity.setFamilyId(dto.getFamilyId());
        entity.setYear(dto.getYear());
        entity.setSummaryDate(dto.getSummaryDate());
        entity.setTotalAssets(dto.getTotalAssets());
        entity.setTotalLiabilities(dto.getTotalLiabilities());
        entity.setNetWorth(dto.getNetWorth());
        entity.setAssetBreakdown(dto.getAssetBreakdown());
        entity.setLiabilityBreakdown(dto.getLiabilityBreakdown());
        entity.setNetAssetBreakdown(dto.getNetAssetBreakdown());
        entity.setCurrency(dto.getCurrency());
        entity.setYoyAssetChange(dto.getYoyAssetChange());
        entity.setYoyLiabilityChange(dto.getYoyLiabilityChange());
        entity.setYoyNetWorthChange(dto.getYoyNetWorthChange());
        entity.setYoyAssetChangePct(dto.getYoyAssetChangePct());
        entity.setYoyLiabilityChangePct(dto.getYoyLiabilityChangePct());
        entity.setYoyNetWorthChangePct(dto.getYoyNetWorthChangePct());
        entity.setRealEstateAssets(dto.getRealEstateAssets());
        entity.setRealEstateNetWorth(dto.getRealEstateNetWorth());
        entity.setNonRealEstateNetWorth(dto.getNonRealEstateNetWorth());
        entity.setYoyRealEstateNetWorthChange(dto.getYoyRealEstateNetWorthChange());
        entity.setYoyRealEstateNetWorthChangePct(dto.getYoyRealEstateNetWorthChangePct());
        entity.setYoyNonRealEstateNetWorthChange(dto.getYoyNonRealEstateNetWorthChange());
        entity.setYoyNonRealEstateNetWorthChangePct(dto.getYoyNonRealEstateNetWorthChangePct());
        entity.setRealEstateAssetRatio(dto.getRealEstateAssetRatio());
        entity.setRealEstateNetWorthRatio(dto.getRealEstateNetWorthRatio());
        entity.setRealEstateToNetWorthRatio(dto.getRealEstateToNetWorthRatio());
        entity.setNotes(dto.getNotes());
    }
}
