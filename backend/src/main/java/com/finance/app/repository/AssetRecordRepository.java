package com.finance.app.repository;

import com.finance.app.model.AssetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
