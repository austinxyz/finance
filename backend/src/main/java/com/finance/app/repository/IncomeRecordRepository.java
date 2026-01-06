package com.finance.app.repository;

import com.finance.app.model.IncomeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, Long> {

    /**
     * 查找指定家庭、周期的所有收入记录
     */
    List<IncomeRecord> findByFamilyIdAndPeriod(Long familyId, String period);

    /**
     * 查找指定家庭、周期范围的所有收入记录
     */
    @Query("SELECT ir FROM IncomeRecord ir WHERE ir.familyId = :familyId " +
           "AND ir.period >= :startPeriod AND ir.period <= :endPeriod " +
           "ORDER BY ir.period DESC, ir.majorCategoryId, ir.minorCategoryId")
    List<IncomeRecord> findByFamilyIdAndPeriodBetween(
        @Param("familyId") Long familyId,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    /**
     * 查找指定家庭、大类、周期的收入记录
     */
    List<IncomeRecord> findByFamilyIdAndMajorCategoryIdAndPeriod(
        Long familyId,
        Long majorCategoryId,
        String period
    );

    /**
     * 查找指定家庭、年份的所有收入记录
     */
    @Query("SELECT ir FROM IncomeRecord ir WHERE ir.familyId = :familyId " +
           "AND ir.period LIKE CONCAT(:year, '%') " +
           "ORDER BY ir.period")
    List<IncomeRecord> findByFamilyIdAndYear(
        @Param("familyId") Long familyId,
        @Param("year") String year
    );

    /**
     * 删除指定家庭、周期的所有收入记录
     */
    void deleteByFamilyIdAndPeriod(Long familyId, String period);
}
