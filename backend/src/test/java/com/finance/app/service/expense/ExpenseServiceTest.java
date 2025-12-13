package com.finance.app.service.expense;

import com.finance.app.dto.expense.*;
import com.finance.app.model.*;
import com.finance.app.repository.*;
import com.finance.app.service.expense.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseCategoryMajorRepository majorRepository;

    @Mock
    private ExpenseCategoryMinorRepository minorRepository;

    @Mock
    private ExpenseRecordRepository recordRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private Family testFamily;
    private User testUser;
    private ExpenseCategoryMajor majorCategory;
    private ExpenseCategoryMinor minorCategory;
    private ExpenseRecord expenseRecord;

    @BeforeEach
    void setUp() {
        // Setup test family
        testFamily = new Family();
        testFamily.setId(1L);
        testFamily.setFamilyName("Test Family");

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Setup major category
        majorCategory = new ExpenseCategoryMajor();
        majorCategory.setId(1L);
        majorCategory.setCode("FOOD");
        majorCategory.setName("食");
        majorCategory.setIcon("🍜");
        majorCategory.setColor("#95E1D3");
        majorCategory.setSortOrder(3);
        majorCategory.setIsActive(true);

        // Setup minor category
        minorCategory = new ExpenseCategoryMinor();
        minorCategory.setId(1L);
        minorCategory.setName("外出就餐");
        minorCategory.setMajorCategory(majorCategory);
        minorCategory.setIsActive(true);
        minorCategory.setIsDefault(false);
        minorCategory.setSortOrder(1);

        // Setup expense record
        expenseRecord = new ExpenseRecord();
        expenseRecord.setId(1L);
        expenseRecord.setFamily(testFamily);
        expenseRecord.setUser(testUser);
        expenseRecord.setExpensePeriod("2024-12");
        expenseRecord.setMajorCategory(majorCategory);
        expenseRecord.setMinorCategory(minorCategory);
        expenseRecord.setAmount(new BigDecimal("1500.00"));
        expenseRecord.setCurrency("USD");
        expenseRecord.setExpenseType("FIXED_DAILY");
    }

    @Test
    @DisplayName("应该成功获取所有分类层级结构")
    void shouldGetAllCategoriesWithHierarchy() {
        // Given
        List<ExpenseCategoryMajor> majors = Arrays.asList(majorCategory);
        List<ExpenseCategoryMinor> minors = Arrays.asList(minorCategory);

        when(majorRepository.findAllByOrderBySortOrder()).thenReturn(majors);
        when(minorRepository.findByMajorCategoryIdOrderBySortOrder(1L)).thenReturn(minors);
        when(minorRepository.countExpenseRecords(anyLong())).thenReturn(5L);

        // When
        List<ExpenseCategoryDTO> result = expenseService.getAllCategories();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        ExpenseCategoryDTO categoryDTO = result.get(0);
        assertEquals("FOOD", categoryDTO.getCode());
        assertEquals("食", categoryDTO.getName());
        assertEquals(1, categoryDTO.getMinorCategories().size());

        ExpenseCategoryDTO.MinorCategoryDTO minorDTO = categoryDTO.getMinorCategories().get(0);
        assertEquals("外出就餐", minorDTO.getName());
        assertEquals(5, minorDTO.getRecordCount());

        verify(majorRepository).findAllByOrderBySortOrder();
        verify(minorRepository).findByMajorCategoryIdOrderBySortOrder(1L);
    }

    @Test
    @DisplayName("应该成功创建子分类")
    void shouldCreateMinorCategorySuccessfully() {
        // Given
        CreateMinorCategoryRequest request = new CreateMinorCategoryRequest();
        request.setMajorCategoryId(1L);
        request.setName("零食");
        request.setSortOrder(2);
        request.setDescription("零食饮料");

        ExpenseCategoryMinor newMinor = new ExpenseCategoryMinor();
        newMinor.setId(2L);
        newMinor.setMajorCategoryId(1L); // Fix: add majorCategoryId
        newMinor.setName("零食");
        newMinor.setMajorCategory(majorCategory);

        when(majorRepository.findById(1L)).thenReturn(Optional.of(majorCategory));
        when(minorRepository.save(any(ExpenseCategoryMinor.class))).thenReturn(newMinor);

        // When
        ExpenseCategoryDTO.MinorCategoryDTO result = expenseService.createMinorCategory(request);

        // Then
        assertNotNull(result);
        assertEquals("零食", result.getName());
        assertEquals(1L, result.getMajorCategoryId());

        verify(majorRepository).findById(1L);
        verify(minorRepository).save(any(ExpenseCategoryMinor.class));
    }

    @Test
    @DisplayName("创建子分类时大类不存在应该抛出异常")
    void shouldThrowExceptionWhenMajorCategoryNotFound() {
        // Given
        CreateMinorCategoryRequest request = new CreateMinorCategoryRequest();
        request.setMajorCategoryId(999L);
        request.setName("不存在");

        when(majorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            expenseService.createMinorCategory(request);
        });

        verify(majorRepository).findById(999L);
        verify(minorRepository, never()).save(any());
    }

    @Test
    @DisplayName("应该成功停用没有记录的子分类")
    void shouldDisableMinorCategoryWithoutRecords() {
        // Given
        when(minorRepository.findById(1L)).thenReturn(Optional.of(minorCategory));
        when(minorRepository.hasExpenseRecords(1L)).thenReturn(false);

        // When
        expenseService.disableMinorCategory(1L);

        // Then
        verify(minorRepository).findById(1L);
        verify(minorRepository).hasExpenseRecords(1L);
        verify(minorRepository).deleteById(1L);
    }

    @Test
    @DisplayName("停用有记录的子分类应该只设置为不活跃")
    void shouldSoftDeleteMinorCategoryWithRecords() {
        // Given
        when(minorRepository.findById(1L)).thenReturn(Optional.of(minorCategory));
        when(minorRepository.hasExpenseRecords(1L)).thenReturn(true);
        when(minorRepository.save(any(ExpenseCategoryMinor.class))).thenReturn(minorCategory);

        // When
        expenseService.disableMinorCategory(1L);

        // Then
        assertFalse(minorCategory.getIsActive());
        verify(minorRepository).save(minorCategory);
        verify(minorRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("应该成功创建支出记录（CNY币种）")
    void shouldCreateExpenseRecordWithCNY() {
        // Given
        CreateExpenseRecordRequest request = new CreateExpenseRecordRequest();
        request.setFamilyId(1L);
        request.setUserId(1L);
        request.setExpensePeriod("2024-12");
        request.setMinorCategoryId(1L);
        request.setAmount(new BigDecimal("1500.00"));
        request.setCurrency("CNY");
        request.setExpenseType("FIXED_DAILY");

        when(minorRepository.findById(1L)).thenReturn(Optional.of(minorCategory));
        when(recordRepository.save(any(ExpenseRecord.class))).thenReturn(expenseRecord);

        // When
        ExpenseRecordDTO result = expenseService.createExpenseRecord(request);

        // Then
        assertNotNull(result);
        assertEquals("2024-12", result.getExpensePeriod());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());
        assertEquals("CNY", result.getCurrency());
        assertEquals("食", result.getMajorCategoryName());
        assertEquals("外出就餐", result.getMinorCategoryName());

        verify(recordRepository).save(any(ExpenseRecord.class));
    }

    @Test
    @DisplayName("应该成功创建支出记录（USD币种需转换）")
    void shouldCreateExpenseRecordWithUSD() {
        // Given
        CreateExpenseRecordRequest request = new CreateExpenseRecordRequest();
        request.setFamilyId(1L);
        request.setUserId(1L);
        request.setExpensePeriod("2024-12");
        request.setMinorCategoryId(1L);
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("USD");
        request.setExpenseType("FIXED_DAILY");

        ExchangeRate usdRate = new ExchangeRate();
        usdRate.setCurrency("USD");
        usdRate.setRateToUsd(BigDecimal.ONE);

        ExpenseRecord usdRecord = new ExpenseRecord();
        usdRecord.setId(2L);
        usdRecord.setAmount(new BigDecimal("200.00"));
        usdRecord.setCurrency("USD");
        usdRecord.setMajorCategory(majorCategory);
        usdRecord.setMinorCategory(minorCategory);

        when(minorRepository.findById(1L)).thenReturn(Optional.of(minorCategory));
        when(exchangeRateRepository.findLatestRateByCurrencyAndDate(eq("USD"), any(LocalDate.class)))
            .thenReturn(Arrays.asList(usdRate));
        when(recordRepository.save(any(ExpenseRecord.class))).thenReturn(usdRecord);

        // When
        ExpenseRecordDTO result = expenseService.createExpenseRecord(request);

        // Then
        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        verify(exchangeRateRepository).findLatestRateByCurrencyAndDate(eq("USD"), any(LocalDate.class));
    }

    @Test
    @DisplayName("应该成功批量保存支出记录")
    void shouldBatchSaveExpenseRecords() {
        // Given
        BatchExpenseRecordRequest batchRequest = new BatchExpenseRecordRequest();
        batchRequest.setFamilyId(1L);
        batchRequest.setUserId(1L);
        batchRequest.setExpensePeriod("2024-12");

        BatchExpenseRecordRequest.ExpenseRecordItem record1 = new BatchExpenseRecordRequest.ExpenseRecordItem();
        record1.setMinorCategoryId(1L);
        record1.setAmount(new BigDecimal("1500"));
        record1.setCurrency("CNY");
        record1.setExpenseType("FIXED_DAILY");

        batchRequest.setRecords(Arrays.asList(record1));

        when(minorRepository.findById(1L)).thenReturn(Optional.of(minorCategory));
        when(recordRepository.findByFamilyIdAndExpensePeriodAndMinorCategoryId(anyLong(), anyString(), anyLong()))
            .thenReturn(Collections.emptyList());
        when(recordRepository.save(any(ExpenseRecord.class))).thenReturn(expenseRecord);

        // When
        List<ExpenseRecordDTO> results = expenseService.batchSaveExpenseRecords(batchRequest);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(recordRepository).save(any(ExpenseRecord.class));
    }

    @Test
    @DisplayName("应该成功查询指定期间的支出记录")
    void shouldQueryExpenseRecordsByPeriod() {
        // Given
        List<ExpenseRecord> records = Arrays.asList(expenseRecord);
        when(recordRepository.findByFamilyIdAndExpensePeriod(1L, "2024-12")).thenReturn(records);

        // When
        List<ExpenseRecordDTO> results = expenseService.getExpenseRecordsByPeriod(1L, "2024-12");

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseRecordDTO dto = results.get(0);
        assertEquals("食", dto.getMajorCategoryName());
        assertEquals("🍜", dto.getMajorCategoryIcon());
        assertEquals("外出就餐", dto.getMinorCategoryName());
        assertEquals(new BigDecimal("1500.00"), dto.getAmount());

        verify(recordRepository).findByFamilyIdAndExpensePeriod(1L, "2024-12");
    }

    @Test
    @DisplayName("应该成功删除支出记录")
    void shouldDeleteExpenseRecord() {
        // Given - No mock needed, Service directly calls deleteById

        // When
        expenseService.deleteExpenseRecord(1L);

        // Then
        verify(recordRepository).deleteById(1L);
    }

    // Note: deleteExpenseRecord doesn't check existence - it directly calls deleteById
    // This is acceptable as deleteById is idempotent (deleting non-existent record is no-op)

    @Test
    @DisplayName("汇率不存在时应该抛出异常")
    void shouldThrowExceptionWhenExchangeRateNotFound() {
        // Given
        CreateExpenseRecordRequest request = new CreateExpenseRecordRequest();
        request.setFamilyId(1L);
        request.setUserId(1L);
        request.setExpensePeriod("2024-12");
        request.setMinorCategoryId(1L);
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("EUR"); // 不存在的币种
        request.setExpenseType("FIXED_DAILY");

        when(minorRepository.findById(1L)).thenReturn(Optional.of(minorCategory));
        when(exchangeRateRepository.findLatestRateByCurrencyAndDate(eq("EUR"), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            expenseService.createExpenseRecord(request);
        });
    }
}
