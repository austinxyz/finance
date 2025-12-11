package com.finance.app.dto.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批量保存预算请求DTO
 */
public class BatchBudgetRequest {

    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    @NotNull(message = "预算年份不能为空")
    @Min(value = 2000, message = "年份必须大于等于2000")
    private Integer budgetYear;

    @NotNull(message = "货币类型不能为空")
    @Size(min = 3, max = 10, message = "货币代码长度必须在3-10之间")
    private String currency;

    @Valid
    @NotNull(message = "预算项不能为空")
    private List<BudgetItem> budgets;

    /**
     * 预算项
     */
    public static class BudgetItem {
        @NotNull(message = "子分类ID不能为空")
        private Long minorCategoryId;

        @NotNull(message = "预算金额不能为空")
        private BigDecimal budgetAmount;

        @Size(max = 500, message = "备注不能超过500字符")
        private String notes;

        // Getters and Setters
        public Long getMinorCategoryId() {
            return minorCategoryId;
        }

        public void setMinorCategoryId(Long minorCategoryId) {
            this.minorCategoryId = minorCategoryId;
        }

        public BigDecimal getBudgetAmount() {
            return budgetAmount;
        }

        public void setBudgetAmount(BigDecimal budgetAmount) {
            this.budgetAmount = budgetAmount;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    // Getters and Setters
    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public Integer getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(Integer budgetYear) {
        this.budgetYear = budgetYear;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<BudgetItem> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<BudgetItem> budgets) {
        this.budgets = budgets;
    }
}
