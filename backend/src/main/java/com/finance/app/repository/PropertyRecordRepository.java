package com.finance.app.repository;

import com.finance.app.model.PropertyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRecordRepository extends JpaRepository<PropertyRecord, Long> {

    /**
     * 根据资产账户ID查找房产记录
     */
    Optional<PropertyRecord> findByAssetAccountId(Long assetAccountId);

    /**
     * 查找指定家庭的所有房产记录
     */
    @Query("SELECT pr FROM PropertyRecord pr " +
           "JOIN pr.assetAccount aa " +
           "WHERE aa.userId IN " +
           "(SELECT u.id FROM User u WHERE u.familyId = :familyId)")
    List<PropertyRecord> findByFamilyId(@Param("familyId") Long familyId);

    /**
     * 查找指定年份购买的房产记录（用于年度汇总计算）
     */
    @Query("SELECT pr FROM PropertyRecord pr " +
           "WHERE YEAR(pr.purchaseDate) = :year " +
           "AND pr.assetAccountId IN " +
           "(SELECT aa.id FROM AssetAccount aa WHERE aa.userId IN " +
           "(SELECT u.id FROM User u WHERE u.familyId = :familyId))")
    List<PropertyRecord> findByFamilyIdAndPurchaseYear(
            @Param("familyId") Long familyId,
            @Param("year") Integer year);
}
