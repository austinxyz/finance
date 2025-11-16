package com.finance.app.repository;

import com.finance.app.model.LiabilityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiabilityCategoryRepository extends JpaRepository<LiabilityCategory, Long> {

    List<LiabilityCategory> findByUserIdOrderByDisplayOrderAsc(Long userId);

    List<LiabilityCategory> findByUserIdAndType(Long userId, String type);

    boolean existsByUserIdAndName(Long userId, String name);

    // Find system categories (shared across all users)
    List<LiabilityCategory> findByIsSystemTrueOrderByDisplayOrderAsc();

    // Find user's custom categories
    List<LiabilityCategory> findByUserIdAndIsSystemFalseOrderByDisplayOrderAsc(Long userId);
}
