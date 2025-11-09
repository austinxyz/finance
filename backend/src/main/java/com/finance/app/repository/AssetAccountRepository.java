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

    List<AssetAccount> findByUserIdAndCategoryId(Long userId, Long categoryId);

    @Query("SELECT a FROM AssetAccount a WHERE a.userId = :userId AND a.isActive = true ORDER BY a.createdAt DESC")
    List<AssetAccount> findActiveAccountsByUserId(@Param("userId") Long userId);
}
