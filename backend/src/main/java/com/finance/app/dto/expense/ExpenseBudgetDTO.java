package com.finance.app.dto.expense;

import java.math.BigDecimal;

/**
 * 支出预算响应DTO
 */
public class ExpenseBudgetDTO {
    private Long id;
    private Long familyId;
    private Integer budgetYear;
    private Long minorCategoryId;
    private Long majorCategoryId;
    private String majorCategoryName;
    private String majorCategoryIcon;
    private String minorCategoryName;
    private String expenseType;
    private BigDecimal budgetAmount;
    private String currency;
    private String notes;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getMinorCategoryId() {
        return minorCategoryId;
    }

    public void setMinorCategoryId(Long minorCategoryId) {
        this.minorCategoryId = minorCategoryId;
    }

    public Long getMajorCategoryId() {
        return majorCategoryId;
    }

    public void setMajorCategoryId(Long majorCategoryId) {
        this.majorCategoryId = majorCategoryId;
    }

    public String getMajorCategoryName() {
        return majorCategoryName;
    }

    public void setMajorCategoryName(String majorCategoryName) {
        this.majorCategoryName = majorCategoryName;
    }

    public String getMajorCategoryIcon() {
        return majorCategoryIcon;
    }

    public void setMajorCategoryIcon(String majorCategoryIcon) {
        this.majorCategoryIcon = majorCategoryIcon;
    }

    public String getMinorCategoryName() {
        return minorCategoryName;
    }

    public void setMinorCategoryName(String minorCategoryName) {
        this.minorCategoryName = minorCategoryName;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
