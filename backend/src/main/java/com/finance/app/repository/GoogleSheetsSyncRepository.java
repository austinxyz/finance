package com.finance.app.repository;

import com.finance.app.model.GoogleSheetsSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Google Sheets同步记录Repository
 */
@Repository
public interface GoogleSheetsSyncRepository extends JpaRepository<GoogleSheetsSync, Long> {

    /**
     * 根据家庭ID和年份查找同步记录
     * @param familyId 家庭ID
     * @param year 年份
     * @return 同步记录
     */
    Optional<GoogleSheetsSync> findByFamilyIdAndYear(Long familyId, Integer year);

    /**
     * 检查是否存在记录
     * @param familyId 家庭ID
     * @param year 年份
     * @return 是否存在
     */
    boolean existsByFamilyIdAndYear(Long familyId, Integer year);
}
