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
}
