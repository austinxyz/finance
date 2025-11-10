package com.finance.app.repository;

import com.finance.app.model.LiabilityAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiabilityAccountRepository extends JpaRepository<LiabilityAccount, Long> {

    List<LiabilityAccount> findByIsActiveTrue();

    List<LiabilityAccount> findByUserIdAndIsActiveTrue(Long userId);

    List<LiabilityAccount> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
