package com.finance.app.repository;

import com.finance.app.model.AssetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRecordRepository extends JpaRepository<AssetRecord, Long> {

    List<AssetRecord> findByAccountIdOrderByRecordDateDesc(Long accountId);

    List<AssetRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM AssetRecord r WHERE r.accountId = :accountId " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 WHERE r2.accountId = :accountId)")
    Optional<AssetRecord> findLatestByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT r FROM AssetRecord r WHERE r.userId = :userId " +
           "ORDER BY r.recordDate DESC")
    List<AssetRecord> findByUserIdOrderByDateDesc(@Param("userId") Long userId);

    boolean existsByAccountIdAndRecordDate(Long accountId, LocalDate recordDate);

    boolean existsByAccountId(Long accountId);

    Optional<AssetRecord> findByAccountIdAndRecordDate(Long accountId, LocalDate recordDate);

    List<AssetRecord> findByAccountIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long accountId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM AssetRecord r WHERE r.accountId = :accountId " +
           "AND r.recordDate <= :asOfDate " +
           "ORDER BY r.recordDate DESC")
    List<AssetRecord> findByAccountIdAndRecordDateBeforeOrEqual(
            @Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

    // 查询指定日期之前最近的一条记录
    @Query("SELECT r FROM AssetRecord r WHERE r.accountId = :accountId " +
           "AND r.recordDate <= :asOfDate " +
           "ORDER BY r.recordDate DESC " +
           "LIMIT 1")
    Optional<AssetRecord> findLatestByAccountIdBeforeOrOnDate(
            @Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

    /**
     * 查询指定家庭、资产类型的所有账户的最新资产记录（用于投资回报率计算）
     */
    @Query("SELECT SUM(r.amount) FROM AssetRecord r " +
           "WHERE r.accountId IN :accountIds " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 WHERE r2.accountId = r.accountId)")
    BigDecimal sumLatestAmountByAccountIds(@Param("accountIds") List<Long> accountIds);

    /**
     * 查询指定家庭、资产类型的所有账户在指定日期的资产总额
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM AssetRecord r " +
           "WHERE r.accountId IN :accountIds " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 " +
           "                     WHERE r2.accountId = r.accountId AND r2.recordDate <= :asOfDate)")
    BigDecimal sumAmountByAccountIdsAsOfDate(
            @Param("accountIds") List<Long> accountIds,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * 查询指定家庭、资产类型在指定日期的最新总额（USD）
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM AssetRecord r " +
           "JOIN r.account a " +
           "WHERE a.userId IN (SELECT u.id FROM User u WHERE u.familyId = :familyId) " +
           "AND a.assetType.id = :assetTypeId " +
           "AND a.isActive = true " +
           "AND r.currency = 'USD' " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 " +
           "                     WHERE r2.accountId = r.accountId AND r2.recordDate <= :asOfDate)")
    Optional<BigDecimal> findLatestTotalByTypeAndDate(
            @Param("familyId") Long familyId,
            @Param("assetTypeId") Long assetTypeId,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * 查询指定家庭在指定日期的所有最新资产记录
     */
    @Query("SELECT r FROM AssetRecord r " +
           "JOIN r.account a " +
           "WHERE a.userId IN (SELECT u.id FROM User u WHERE u.familyId = :familyId) " +
           "AND a.isActive = true " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 " +
           "                     WHERE r2.accountId = r.accountId AND r2.recordDate <= :asOfDate) " +
           "ORDER BY a.assetType.displayOrder, a.accountName")
    List<AssetRecord> findLatestRecordsByFamilyAndDate(
            @Param("familyId") Long familyId,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * 查询指定账户在指定日期的最新记录
     */
    @Query("SELECT r FROM AssetRecord r " +
           "WHERE r.accountId = :accountId " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 " +
           "                     WHERE r2.accountId = :accountId AND r2.recordDate <= :asOfDate)")
    Optional<AssetRecord> findLatestByAccountAndDate(
            @Param("accountId") Long accountId,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * 批量查询多个账户的最新记录（用于性能优化）
     */
    @Query("SELECT r FROM AssetRecord r " +
           "WHERE r.accountId IN :accountIds " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 WHERE r2.accountId = r.accountId)")
    List<AssetRecord> findLatestByAccountIds(@Param("accountIds") List<Long> accountIds);

    /**
     * 批量查询多个账户在指定日期之前的最新记录（用于性能优化）
     */
    @Query("SELECT r FROM AssetRecord r " +
           "WHERE r.accountId IN :accountIds " +
           "AND r.recordDate = (SELECT MAX(r2.recordDate) FROM AssetRecord r2 " +
           "                     WHERE r2.accountId = r.accountId AND r2.recordDate <= :asOfDate)")
    List<AssetRecord> findLatestByAccountIdsBeforeOrEqualDate(
            @Param("accountIds") List<Long> accountIds,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * 批量查询多个账户在日期范围内的所有记录（用于趋势分析性能优化）
     */
    List<AssetRecord> findByAccountIdInAndRecordDateBetweenOrderByRecordDateDesc(
            List<Long> accountIds, LocalDate startDate, LocalDate endDate);
}
