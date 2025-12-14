package com.finance.app.repository;

import com.finance.app.model.InvestmentTransaction;
import com.finance.app.model.InvestmentTransaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 投资交易记录Repository
 */
@Repository
public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, Long> {

    /**
     * 根据账户ID和交易期间查询投资交易记录
     */
    List<InvestmentTransaction> findByAccountIdAndTransactionPeriod(Long accountId, String transactionPeriod);

    /**
     * 根据账户ID和交易期间和类型查询唯一记录
     */
    Optional<InvestmentTransaction> findByAccountIdAndTransactionPeriodAndTransactionType(
        Long accountId,
        String transactionPeriod,
        TransactionType transactionType
    );

    /**
     * 根据账户ID查询所有交易记录，按期间倒序
     */
    List<InvestmentTransaction> findByAccountIdOrderByTransactionPeriodDesc(Long accountId);

    /**
     * 根据账户ID和期间范围查询交易记录
     */
    @Query("SELECT t FROM InvestmentTransaction t " +
           "WHERE t.accountId = :accountId " +
           "AND t.transactionPeriod >= :startPeriod " +
           "AND t.transactionPeriod <= :endPeriod " +
           "ORDER BY t.transactionPeriod ASC")
    List<InvestmentTransaction> findByAccountIdAndPeriodRange(
        @Param("accountId") Long accountId,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    /**
     * 根据账户ID列表和交易期间查询所有交易记录
     * 用于批量录入页面加载历史数据
     */
    @Query("SELECT t FROM InvestmentTransaction t " +
           "WHERE t.accountId IN :accountIds " +
           "AND t.transactionPeriod = :transactionPeriod")
    List<InvestmentTransaction> findByAccountIdsAndTransactionPeriod(
        @Param("accountIds") List<Long> accountIds,
        @Param("transactionPeriod") String transactionPeriod
    );

    /**
     * 根据账户ID列表和期间范围查询所有交易记录
     * 用于批量录入页面加载前3个月历史数据
     */
    @Query("SELECT t FROM InvestmentTransaction t " +
           "WHERE t.accountId IN :accountIds " +
           "AND t.transactionPeriod >= :startPeriod " +
           "AND t.transactionPeriod <= :endPeriod " +
           "ORDER BY t.transactionPeriod DESC, t.accountId")
    List<InvestmentTransaction> findByAccountIdsAndPeriodRange(
        @Param("accountIds") List<Long> accountIds,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    /**
     * 根据交易期间查询所有交易记录
     * 用于验证和统计
     */
    List<InvestmentTransaction> findByTransactionPeriod(String transactionPeriod);

    /**
     * 根据账户ID和交易类型统计总额
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM InvestmentTransaction t " +
           "WHERE t.accountId = :accountId " +
           "AND t.transactionType = :transactionType " +
           "AND t.transactionPeriod >= :startPeriod " +
           "AND t.transactionPeriod <= :endPeriod")
    Double sumAmountByAccountAndType(
        @Param("accountId") Long accountId,
        @Param("transactionType") TransactionType transactionType,
        @Param("startPeriod") String startPeriod,
        @Param("endPeriod") String endPeriod
    );

    /**
     * 删除指定账户、期间、类型的交易记录
     * 用于批量更新时清理旧数据
     */
    void deleteByAccountIdAndTransactionPeriodAndTransactionType(
        Long accountId,
        String transactionPeriod,
        TransactionType transactionType
    );

    /**
     * 批量删除指定ID列表的记录
     */
    void deleteByIdIn(List<Long> ids);

    /**
     * 根据家庭ID和年份模式查询所有投资交易记录（用于年度分析）
     */
    @Query("SELECT t FROM InvestmentTransaction t " +
           "JOIN t.account a " +
           "WHERE a.userId IN (SELECT u.id FROM User u WHERE u.familyId = :familyId) " +
           "AND t.transactionPeriod LIKE :yearPattern")
    List<InvestmentTransaction> findByFamilyIdAndYearPattern(
        @Param("familyId") Long familyId,
        @Param("yearPattern") String yearPattern
    );

    /**
     * 根据家庭ID、大类ID和年份模式查询投资交易记录（用于账户分析）
     */
    @Query("SELECT t FROM InvestmentTransaction t " +
           "JOIN t.account a " +
           "WHERE a.userId IN (SELECT u.id FROM User u WHERE u.familyId = :familyId) " +
           "AND a.assetTypeId = :assetTypeId " +
           "AND t.transactionPeriod LIKE :yearPattern")
    List<InvestmentTransaction> findByFamilyIdAndAssetTypeIdAndYearPattern(
        @Param("familyId") Long familyId,
        @Param("assetTypeId") Long assetTypeId,
        @Param("yearPattern") String yearPattern
    );

    /**
     * 根据账户ID和年份模式查询交易记录（用于月度趋势）
     */
    @Query("SELECT t FROM InvestmentTransaction t " +
           "WHERE t.accountId = :accountId " +
           "AND t.transactionPeriod LIKE :yearPattern " +
           "ORDER BY t.transactionPeriod ASC")
    List<InvestmentTransaction> findByAccountIdAndYearPattern(
        @Param("accountId") Long accountId,
        @Param("yearPattern") String yearPattern
    );
}
