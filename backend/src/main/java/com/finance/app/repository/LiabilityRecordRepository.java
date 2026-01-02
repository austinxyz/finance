package com.finance.app.repository;

import com.finance.app.model.LiabilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiabilityRecordRepository extends JpaRepository<LiabilityRecord, Long> {

    List<LiabilityRecord> findByAccountIdOrderByRecordDateDesc(Long accountId);

    @Query("SELECT r FROM LiabilityRecord r WHERE r.accountId = :accountId " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM LiabilityRecord r2 WHERE r2.accountId = :accountId)")
    Optional<LiabilityRecord> findLatestByAccountId(@Param("accountId") Long accountId);

    List<LiabilityRecord> findByUserIdOrderByRecordDateDesc(Long userId);

    boolean existsByAccountIdAndRecordDate(Long accountId, LocalDate recordDate);

    boolean existsByAccountId(Long accountId);

    Optional<LiabilityRecord> findByAccountIdAndRecordDate(Long accountId, LocalDate recordDate);

    List<LiabilityRecord> findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long accountId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM LiabilityRecord r WHERE r.accountId = :accountId " +
           "AND r.recordDate <= :asOfDate " +
           "ORDER BY r.recordDate DESC")
    List<LiabilityRecord> findByAccountIdAndRecordDateBeforeOrEqual(
            @Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

    // 查询指定日期之前最近的一条记录
    @Query("SELECT r FROM LiabilityRecord r WHERE r.accountId = :accountId " +
           "AND r.recordDate <= :asOfDate " +
           "ORDER BY r.recordDate DESC " +
           "LIMIT 1")
    Optional<LiabilityRecord> findLatestByAccountIdBeforeOrOnDate(
            @Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

    /**
     * 查询指定家庭、负债类型在指定日期的最新总额（USD）
     */
    @Query("SELECT COALESCE(SUM(r.outstandingBalance), 0) FROM LiabilityRecord r " +
           "JOIN r.account a " +
           "WHERE a.userId IN (SELECT u.id FROM User u WHERE u.familyId = :familyId) " +
           "AND a.liabilityType.id = :liabilityTypeId " +
           "AND a.isActive = true " +
           "AND r.currency = 'USD' " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM LiabilityRecord r2 " +
           "                     WHERE r2.accountId = r.accountId AND r2.recordDate <= :asOfDate)")
    Optional<java.math.BigDecimal> findLatestTotalByTypeAndDate(
            @Param("familyId") Long familyId,
            @Param("liabilityTypeId") Long liabilityTypeId,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * 查询指定家庭在指定日期的所有最新负债记录
     */
    @Query("SELECT r FROM LiabilityRecord r " +
           "JOIN r.account a " +
           "WHERE a.userId IN (SELECT u.id FROM User u WHERE u.familyId = :familyId) " +
           "AND a.isActive = true " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM LiabilityRecord r2 " +
           "                     WHERE r2.accountId = r.accountId AND r2.recordDate <= :asOfDate) " +
           "ORDER BY a.liabilityType.displayOrder, a.accountName")
    List<LiabilityRecord> findLatestRecordsByFamilyAndDate(
            @Param("familyId") Long familyId,
            @Param("asOfDate") LocalDate asOfDate);
}
