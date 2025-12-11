package com.finance.app.model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpenseRecord Entity Tests")
class ExpenseRecordTest {

    private ExpenseRecord expenseRecord;
    private Family family;
    private User user;
    private ExpenseCategoryMajor majorCategory;
    private ExpenseCategoryMinor minorCategory;

    @BeforeEach
    void setUp() {
        // Setup test data
        family = new Family();
        family.setId(1L);
        family.setFamilyName("Test Family");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        majorCategory = new ExpenseCategoryMajor();
        majorCategory.setId(1L);
        majorCategory.setCode("FOOD");
        majorCategory.setName("食");

        minorCategory = new ExpenseCategoryMinor();
        minorCategory.setId(1L);
        minorCategory.setName("外出就餐");
        minorCategory.setMajorCategory(majorCategory);

        expenseRecord = new ExpenseRecord();
    }

    @Test
    @DisplayName("应该正确创建ExpenseRecord实例")
    void shouldCreateExpenseRecordSuccessfully() {
        // Given
        expenseRecord.setFamily(family);
        expenseRecord.setUser(user);
        expenseRecord.setExpensePeriod("2024-12");
        expenseRecord.setMajorCategory(majorCategory);
        expenseRecord.setMinorCategory(minorCategory);
        expenseRecord.setAmount(new BigDecimal("1500.00"));
        expenseRecord.setCurrency("CNY");
        expenseRecord.setAmountInBaseCurrency(new BigDecimal("1500.00"));
        expenseRecord.setExpenseType("FIXED_DAILY");

        // Then
        assertNotNull(expenseRecord);
        assertEquals(family, expenseRecord.getFamily());
        assertEquals(user, expenseRecord.getUser());
        assertEquals("2024-12", expenseRecord.getExpensePeriod());
        assertEquals(majorCategory, expenseRecord.getMajorCategory());
        assertEquals(minorCategory, expenseRecord.getMinorCategory());
        assertEquals(new BigDecimal("1500.00"), expenseRecord.getAmount());
        assertEquals("CNY", expenseRecord.getCurrency());
        assertEquals("FIXED_DAILY", expenseRecord.getExpenseType());
    }

    @Test
    @DisplayName("应该正确设置和获取ID")
    void shouldSetAndGetIdCorrectly() {
        // When
        expenseRecord.setId(100L);

        // Then
        assertEquals(100L, expenseRecord.getId());
    }

    @Test
    @DisplayName("应该正确处理描述字段")
    void shouldHandleDescriptionCorrectly() {
        // Given
        String description = "公司聚餐";

        // When
        expenseRecord.setDescription(description);

        // Then
        assertEquals(description, expenseRecord.getDescription());
    }

    @Test
    @DisplayName("应该正确处理两种支出类型")
    void shouldHandleExpenseTypes() {
        // Test FIXED_DAILY
        expenseRecord.setExpenseType("FIXED_DAILY");
        assertEquals("FIXED_DAILY", expenseRecord.getExpenseType());

        // Test LARGE_IRREGULAR
        expenseRecord.setExpenseType("LARGE_IRREGULAR");
        assertEquals("LARGE_IRREGULAR", expenseRecord.getExpenseType());
    }

    @Test
    @DisplayName("应该正确设置创建和更新时间")
    void shouldSetCreatedAndUpdatedTimestamps() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        expenseRecord.setCreatedAt(now);
        expenseRecord.setUpdatedAt(now);

        // Then
        assertEquals(now, expenseRecord.getCreatedAt());
        assertEquals(now, expenseRecord.getUpdatedAt());
    }

    @Test
    @DisplayName("BigDecimal金额应该保持精度")
    void shouldMaintainDecimalPrecision() {
        // Given
        BigDecimal preciseAmount = new BigDecimal("1234.56");

        // When
        expenseRecord.setAmount(preciseAmount);
        expenseRecord.setAmountInBaseCurrency(preciseAmount);

        // Then
        assertEquals(0, preciseAmount.compareTo(expenseRecord.getAmount()));
        assertEquals(0, preciseAmount.compareTo(expenseRecord.getAmountInBaseCurrency()));
    }

    @Test
    @DisplayName("应该正确处理期间格式")
    void shouldHandlePeriodFormat() {
        // Valid periods
        expenseRecord.setExpensePeriod("2024-01");
        assertEquals("2024-01", expenseRecord.getExpensePeriod());

        expenseRecord.setExpensePeriod("2024-12");
        assertEquals("2024-12", expenseRecord.getExpensePeriod());
    }

    @Test
    @DisplayName("应该允许空描述")
    void shouldAllowNullDescription() {
        // When
        expenseRecord.setDescription(null);

        // Then
        assertNull(expenseRecord.getDescription());
    }

    @Test
    @DisplayName("应该正确关联大类和小类")
    void shouldCorrectlyAssociateMajorAndMinorCategories() {
        // When
        expenseRecord.setMajorCategory(majorCategory);
        expenseRecord.setMinorCategory(minorCategory);

        // Then
        assertEquals(majorCategory, expenseRecord.getMajorCategory());
        assertEquals(minorCategory, expenseRecord.getMinorCategory());
        assertEquals(majorCategory, minorCategory.getMajorCategory());
    }
}
