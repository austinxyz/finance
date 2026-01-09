package com.finance.app.service.expense;

import com.finance.app.dto.expense.BatchBudgetRequest;
import com.finance.app.dto.expense.ExpenseBudgetDTO;
import com.finance.app.model.ExpenseBudget;
import com.finance.app.model.ExpenseCategoryMajor;
import com.finance.app.model.ExpenseCategoryMinor;
import com.finance.app.repository.ExpenseBudgetRepository;
import com.finance.app.repository.ExpenseCategoryMajorRepository;
import com.finance.app.repository.ExpenseCategoryMinorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 支出预算Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseBudgetService {

    private final ExpenseBudgetRepository budgetRepository;
    private final ExpenseCategoryMajorRepository majorCategoryRepository;
    private final ExpenseCategoryMinorRepository minorCategoryRepository;

    /**
     * 获取指定家庭、年份、货币的预算（带分类信息）
     */
    public List<ExpenseBudgetDTO> getBudgets(Long familyId, Integer budgetYear, String currency) {
        List<ExpenseBudget> budgets = budgetRepository.findByFamilyIdAndBudgetYearAndCurrency(
            familyId, budgetYear, currency);

        // 构建预算ID到预算对象的映射
        Map<Long, ExpenseBudget> budgetMap = budgets.stream()
            .collect(Collectors.toMap(ExpenseBudget::getMinorCategoryId, b -> b));

        // 获取所有分类
        List<ExpenseCategoryMajor> majors = majorCategoryRepository.findAllByOrderBySortOrder();
        List<ExpenseBudgetDTO> result = new ArrayList<>();

        for (ExpenseCategoryMajor major : majors) {
            List<ExpenseCategoryMinor> minors = minorCategoryRepository
                .findByMajorCategoryIdOrderBySortOrder(major.getId());

            for (ExpenseCategoryMinor minor : minors) {
                ExpenseBudgetDTO dto = new ExpenseBudgetDTO();

                // 分类信息
                dto.setMinorCategoryId(minor.getId());
                dto.setMajorCategoryId(major.getId());
                dto.setMajorCategoryName(major.getName());
                dto.setMajorCategoryIcon(major.getIcon());
                dto.setMinorCategoryName(minor.getName());
                dto.setExpenseType(minor.getExpenseType());

                // 预算信息
                ExpenseBudget budget = budgetMap.get(minor.getId());
                if (budget != null) {
                    dto.setId(budget.getId());
                    dto.setFamilyId(budget.getFamilyId());
                    dto.setBudgetYear(budget.getBudgetYear());
                    dto.setBudgetAmount(budget.getBudgetAmount());
                    dto.setCurrency(budget.getCurrency());
                    dto.setNotes(budget.getNotes());
                } else {
                    // 没有预算记录，设置默认值
                    dto.setFamilyId(familyId);
                    dto.setBudgetYear(budgetYear);
                    dto.setBudgetAmount(BigDecimal.ZERO);
                    dto.setCurrency(currency);
                }

                result.add(dto);
            }
        }

        return result;
    }

    /**
     * 批量保存预算
     */
    @Transactional
    public List<ExpenseBudgetDTO> batchSaveBudgets(BatchBudgetRequest request) {
        List<ExpenseBudgetDTO> savedBudgets = new ArrayList<>();
        int saveCount = 0;
        int deleteCount = 0;

        for (BatchBudgetRequest.BudgetItem item : request.getBudgets()) {
            // 如果金额为0或负数，删除预算
            if (item.getBudgetAmount() == null ||
                item.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {

                Optional<ExpenseBudget> existing = budgetRepository
                    .findByFamilyIdAndBudgetYearAndMinorCategoryIdAndCurrency(
                        request.getFamilyId(),
                        request.getBudgetYear(),
                        item.getMinorCategoryId(),
                        request.getCurrency()
                    );

                if (existing.isPresent()) {
                    budgetRepository.delete(existing.get());
                    deleteCount++;
                }
                continue;
            }

            // 查找已存在的预算记录
            Optional<ExpenseBudget> existingOpt = budgetRepository
                .findByFamilyIdAndBudgetYearAndMinorCategoryIdAndCurrency(
                    request.getFamilyId(),
                    request.getBudgetYear(),
                    item.getMinorCategoryId(),
                    request.getCurrency()
                );

            ExpenseBudget budget;
            if (existingOpt.isPresent()) {
                // 更新现有预算
                budget = existingOpt.get();
                budget.setBudgetAmount(item.getBudgetAmount());
                budget.setNotes(item.getNotes());
            } else {
                // 创建新预算
                budget = new ExpenseBudget();
                budget.setFamilyId(request.getFamilyId());
                budget.setBudgetYear(request.getBudgetYear());
                budget.setMinorCategoryId(item.getMinorCategoryId());
                budget.setBudgetAmount(item.getBudgetAmount());
                budget.setCurrency(request.getCurrency());
                budget.setNotes(item.getNotes());
            }

            budget = budgetRepository.save(budget);
            saveCount++;

            // 构建返回DTO
            ExpenseBudgetDTO dto = buildBudgetDTO(budget);
            savedBudgets.add(dto);
        }

        log.info("批量保存预算完成: 保存{}条, 删除{}条", saveCount, deleteCount);
        return savedBudgets;
    }

    /**
     * 构建预算DTO（包含分类信息）
     */
    private ExpenseBudgetDTO buildBudgetDTO(ExpenseBudget budget) {
        ExpenseBudgetDTO dto = new ExpenseBudgetDTO();
        dto.setId(budget.getId());
        dto.setFamilyId(budget.getFamilyId());
        dto.setBudgetYear(budget.getBudgetYear());
        dto.setMinorCategoryId(budget.getMinorCategoryId());
        dto.setBudgetAmount(budget.getBudgetAmount());
        dto.setCurrency(budget.getCurrency());
        dto.setNotes(budget.getNotes());

        // 获取分类信息
        Optional<ExpenseCategoryMinor> minorOpt = minorCategoryRepository
            .findById(budget.getMinorCategoryId());

        if (minorOpt.isPresent()) {
            ExpenseCategoryMinor minor = minorOpt.get();
            dto.setMinorCategoryName(minor.getName());
            dto.setExpenseType(minor.getExpenseType());

            Optional<ExpenseCategoryMajor> majorOpt = majorCategoryRepository
                .findById(minor.getMajorCategoryId());

            if (majorOpt.isPresent()) {
                ExpenseCategoryMajor major = majorOpt.get();
                dto.setMajorCategoryId(major.getId());
                dto.setMajorCategoryName(major.getName());
                dto.setMajorCategoryIcon(major.getIcon());
            }
        }

        return dto;
    }

    /**
     * 根据ID获取预算（用于授权验证）
     */
    public ExpenseBudgetDTO getBudgetById(Long id) {
        ExpenseBudget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("预算不存在"));
        return toDTO(budget);
    }

    /**
     * 删除指定预算
     */
    @Transactional
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
        log.info("删除预算: {}", id);
    }
}
