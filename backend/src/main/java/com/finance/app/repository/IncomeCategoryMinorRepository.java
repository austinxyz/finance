package com.finance.app.repository;

import com.finance.app.model.IncomeCategoryMinor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeCategoryMinorRepository extends JpaRepository<IncomeCategoryMinor, Long> {

    /**
     * 查找指定大类下的所有小类
     */
    List<IncomeCategoryMinor> findByMajorCategoryId(Long majorCategoryId);

    /**
     * 查找指定大类下的启用小类
     */
    List<IncomeCategoryMinor> findByMajorCategoryIdAndIsActiveTrue(Long majorCategoryId);

    /**
     * 查找所有启用的小类
     */
    List<IncomeCategoryMinor> findByIsActiveTrue();

    /**
     * 根据家庭ID和大类ID查询小分类（包含公共分类和该家庭用户的私有分类）
     * 逻辑：user_id IS NULL（公共分类）OR user_id IN（该家庭的所有用户ID）
     */
    @Query("SELECT icm FROM IncomeCategoryMinor icm " +
           "WHERE icm.majorCategoryId = :majorCategoryId " +
           "AND (icm.userId IS NULL OR icm.userId IN " +
           "(SELECT u.id FROM User u WHERE u.familyId = :familyId))")
    List<IncomeCategoryMinor> findByMajorCategoryIdAndFamilyId(
        @Param("majorCategoryId") Long majorCategoryId,
        @Param("familyId") Long familyId
    );

    /**
     * 根据家庭ID和大类ID查询启用的小分类（包含公共分类和该家庭用户的私有分类）
     */
    @Query("SELECT icm FROM IncomeCategoryMinor icm " +
           "WHERE icm.majorCategoryId = :majorCategoryId " +
           "AND icm.isActive = true " +
           "AND (icm.userId IS NULL OR icm.userId IN " +
           "(SELECT u.id FROM User u WHERE u.familyId = :familyId))")
    List<IncomeCategoryMinor> findByMajorCategoryIdAndFamilyIdAndIsActiveTrue(
        @Param("majorCategoryId") Long majorCategoryId,
        @Param("familyId") Long familyId
    );

    /**
     * 根据家庭ID查询所有启用的小分类（包含公共分类和该家庭用户的私有分类）
     */
    @Query("SELECT icm FROM IncomeCategoryMinor icm " +
           "WHERE icm.isActive = true " +
           "AND (icm.userId IS NULL OR icm.userId IN " +
           "(SELECT u.id FROM User u WHERE u.familyId = :familyId))")
    List<IncomeCategoryMinor> findByFamilyIdAndIsActiveTrue(
        @Param("familyId") Long familyId
    );
}
