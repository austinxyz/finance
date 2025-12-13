package com.finance.app.repository;

import com.finance.app.model.LiabilityAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiabilityAccountRepository extends JpaRepository<LiabilityAccount, Long> {

    List<LiabilityAccount> findByIsActiveTrue();

    List<LiabilityAccount> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT l FROM LiabilityAccount l JOIN User u ON l.userId = u.id WHERE u.familyId = :familyId AND l.isActive = true")
    List<LiabilityAccount> findByFamilyIdAndIsActiveTrue(@Param("familyId") Long familyId);

    List<LiabilityAccount> findByUserIdInAndIsActiveTrue(List<Long> userIds);

    List<LiabilityAccount> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
