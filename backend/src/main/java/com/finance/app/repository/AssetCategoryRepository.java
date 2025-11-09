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
}
