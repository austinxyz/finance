package com.finance.app.repository;

import com.finance.app.model.AssetAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAccountRepository extends JpaRepository<AssetAccount, Long> {

    List<AssetAccount> findByIsActiveTrue();

    List<AssetAccount> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT a FROM AssetAccount a JOIN User u ON a.userId = u.id WHERE u.familyId = :familyId AND a.isActive = true")
    List<AssetAccount> findByFamilyIdAndIsActiveTrue(@Param("familyId") Long familyId);

    List<AssetAccount> findByUserIdInAndIsActiveTrue(List<Long> userIds);

    @Query("SELECT a FROM AssetAccount a WHERE a.userId = :userId AND a.isActive = true ORDER BY a.createdAt DESC")
    List<AssetAccount> findActiveAccountsByUserId(@Param("userId") Long userId);

    /**
     * 查询指定家庭的非退休投资账户
     */
    @Query("SELECT a FROM AssetAccount a JOIN User u ON a.userId = u.id " +
           "WHERE u.familyId = :familyId " +
           "AND a.isActive = true " +
           "AND a.taxStatus != :taxStatus " +
           "ORDER BY a.accountName")
    List<AssetAccount> findByFamilyIdAndIsActiveTrueAndTaxStatusNot(
        @Param("familyId") Long familyId,
        @Param("taxStatus") com.finance.app.model.TaxStatus taxStatus
    );

    /**
     * 查询指定家庭的退休账户
     */
    @Query("SELECT a FROM AssetAccount a JOIN User u ON a.userId = u.id " +
           "WHERE u.familyId = :familyId " +
           "AND a.isActive = true " +
           "AND a.taxStatus = :taxStatus " +
           "ORDER BY a.accountName")
    List<AssetAccount> findByFamilyIdAndIsActiveTrueAndTaxStatus(
        @Param("familyId") Long familyId,
        @Param("taxStatus") com.finance.app.model.TaxStatus taxStatus
    );
}
