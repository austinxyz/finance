package com.finance.app.repository;

import com.finance.app.model.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {

    List<AssetCategory> findByUserIdOrderByDisplayOrderAsc(Long userId);

    List<AssetCategory> findByUserIdAndType(Long userId, String type);

    boolean existsByUserIdAndName(Long userId, String name);

    // Find system categories (shared across all users)
    List<AssetCategory> findByIsSystemTrueOrderByDisplayOrderAsc();

    // Find user's custom categories
    List<AssetCategory> findByUserIdAndIsSystemFalseOrderByDisplayOrderAsc(Long userId);

    // Find investment categories (is_investment = TRUE)
    List<AssetCategory> findByIsInvestmentTrueOrderByDisplayOrderAsc();
}
