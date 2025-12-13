package com.finance.app.controller.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.controller.ExpenseController;
import com.finance.app.dto.expense.*;
import com.finance.app.model.ExpenseRecord;
import com.finance.app.service.expense.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@DisplayName("ExpenseController Tests")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    private ExpenseCategoryDTO categoryDTO;
    private ExpenseCategoryDTO.MinorCategoryDTO minorCategoryDTO;
    private ExpenseRecordDTO recordDTO;

    @BeforeEach
    void setUp() {
        // Setup minor category DTO
        minorCategoryDTO = new ExpenseCategoryDTO.MinorCategoryDTO();
        minorCategoryDTO.setId(1L);
        minorCategoryDTO.setMajorCategoryId(1L);
        minorCategoryDTO.setName("外出就餐");
        minorCategoryDTO.setIsActive(true);
        minorCategoryDTO.setIsDefault(false);
        minorCategoryDTO.setSortOrder(1);
        minorCategoryDTO.setRecordCount(5);

        // Setup category DTO
        categoryDTO = new ExpenseCategoryDTO();
        categoryDTO.setId(1L);
        categoryDTO.setCode("FOOD");
        categoryDTO.setName("食");
        categoryDTO.setIcon("🍜");
        categoryDTO.setColor("#95E1D3");
        categoryDTO.setSortOrder(3);
        categoryDTO.setIsActive(true);
        categoryDTO.setMinorCategories(Arrays.asList(minorCategoryDTO));

        // Setup record DTO
        recordDTO = new ExpenseRecordDTO();
        recordDTO.setId(1L);
        recordDTO.setFamilyId(1L);
        recordDTO.setUserId(1L);
        recordDTO.setExpensePeriod("2024-12");
        recordDTO.setMajorCategoryId(1L);
        recordDTO.setMajorCategoryName("食");
        recordDTO.setMajorCategoryIcon("🍜");
        recordDTO.setMinorCategoryId(1L);
        recordDTO.setMinorCategoryName("外出就餐");
        recordDTO.setAmount(new BigDecimal("1500.00"));
        recordDTO.setCurrency("USD");
        recordDTO.setExpenseType("FIXED_DAILY");
    }

    @Test
    @DisplayName("GET /api/expenses/categories - 应该返回所有分类")
    void shouldGetAllCategories() throws Exception {
        // Given
        List<ExpenseCategoryDTO> categories = Arrays.asList(categoryDTO);
        when(expenseService.getAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/expenses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("FOOD"))
                .andExpect(jsonPath("$.data[0].name").value("食"))
                .andExpect(jsonPath("$.data[0].icon").value("🍜"))
                .andExpect(jsonPath("$.data[0].minorCategories", hasSize(1)))
                .andExpect(jsonPath("$.data[0].minorCategories[0].name").value("外出就餐"))
                .andExpect(jsonPath("$.data[0].minorCategories[0].recordCount").value(5));

        verify(expenseService).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/expenses/categories - 空列表应该返回成功")
    void shouldReturnEmptyListWhenNoCategories() throws Exception {
        // Given
        when(expenseService.getAllCategories()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/expenses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/expenses/categories/minor - 应该成功创建子分类")
    void shouldCreateMinorCategory() throws Exception {
        // Given
        CreateMinorCategoryRequest request = new CreateMinorCategoryRequest();
        request.setMajorCategoryId(1L);
        request.setName("零食");
        request.setSortOrder(2);
        request.setDescription("零食饮料");

        when(expenseService.createMinorCategory(any(CreateMinorCategoryRequest.class)))
                .thenReturn(minorCategoryDTO);

        // When & Then
        mockMvc.perform(post("/expenses/categories/minor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("子分类创建成功"))
                .andExpect(jsonPath("$.data.name").value("外出就餐"))
                .andExpect(jsonPath("$.data.majorCategoryId").value(1));

        verify(expenseService).createMinorCategory(any(CreateMinorCategoryRequest.class));
    }

    @Test
    @DisplayName("POST /api/expenses/categories/minor - 验证失败应该返回400")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given - Invalid request (missing required field)
        CreateMinorCategoryRequest request = new CreateMinorCategoryRequest();
        // Missing majorCategoryId and name

        // When & Then
        mockMvc.perform(post("/expenses/categories/minor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/expenses/categories/minor/{id} - 应该成功停用子分类")
    void shouldDisableMinorCategory() throws Exception {
        // Given
        doNothing().when(expenseService).disableMinorCategory(1L);

        // When & Then
        mockMvc.perform(delete("/expenses/categories/minor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("子分类处理成功"));

        verify(expenseService).disableMinorCategory(1L);
    }

    @Test
    @DisplayName("POST /api/expenses/records - 应该成功创建支出记录")
    void shouldCreateExpenseRecord() throws Exception {
        // Given
        CreateExpenseRecordRequest request = new CreateExpenseRecordRequest();
        request.setFamilyId(1L);
        request.setUserId(1L);
        request.setExpensePeriod("2024-12");
        request.setMinorCategoryId(1L);
        request.setAmount(new BigDecimal("1500.00"));
        request.setCurrency("CNY");
        request.setExpenseType("FIXED_DAILY");

        when(expenseService.createExpenseRecord(any(CreateExpenseRecordRequest.class)))
                .thenReturn(recordDTO);

        // When & Then
        mockMvc.perform(post("/expenses/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("支出记录创建成功"))
                .andExpect(jsonPath("$.data.expensePeriod").value("2024-12"))
                .andExpect(jsonPath("$.data.amount").value(1500.00))
                .andExpect(jsonPath("$.data.currency").value("CNY"))
                .andExpect(jsonPath("$.data.majorCategoryName").value("食"))
                .andExpect(jsonPath("$.data.minorCategoryName").value("外出就餐"));

        verify(expenseService).createExpenseRecord(any(CreateExpenseRecordRequest.class));
    }

    @Test
    @DisplayName("POST /api/expenses/records/batch - 应该成功批量保存记录")
    void shouldBatchSaveExpenseRecords() throws Exception {
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

        when(expenseService.batchSaveExpenseRecords(any(BatchExpenseRecordRequest.class)))
                .thenReturn(Arrays.asList(recordDTO));

        // When & Then
        mockMvc.perform(post("/expenses/records/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("批量保存成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(expenseService).batchSaveExpenseRecords(any(BatchExpenseRecordRequest.class));
    }

    @Test
    @DisplayName("GET /api/expenses/records - 应该查询指定期间的记录")
    void shouldGetExpenseRecordsByPeriod() throws Exception {
        // Given
        when(expenseService.getExpenseRecordsByPeriod(1L, "2024-12"))
                .thenReturn(Arrays.asList(recordDTO));

        // When & Then
        mockMvc.perform(get("/expenses/records")
                        .param("familyId", "1")
                        .param("period", "2024-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].expensePeriod").value("2024-12"))
                .andExpect(jsonPath("$.data[0].amount").value(1500.00));

        verify(expenseService).getExpenseRecordsByPeriod(1L, "2024-12");
    }

    @Test
    @DisplayName("GET /api/expenses/records/range - 应该查询期间范围的记录")
    void shouldGetExpenseRecordsByRange() throws Exception {
        // Given
        when(expenseService.getExpenseRecordsByPeriodRange(1L, "2024-01", "2024-12"))
                .thenReturn(Arrays.asList(recordDTO));

        // When & Then
        mockMvc.perform(get("/expenses/records/range")
                        .param("familyId", "1")
                        .param("startPeriod", "2024-01")
                        .param("endPeriod", "2024-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(expenseService).getExpenseRecordsByPeriodRange(1L, "2024-01", "2024-12");
    }

    @Test
    @DisplayName("PUT /api/expenses/records/{id} - 应该成功更新记录")
    void shouldUpdateExpenseRecord() throws Exception {
        // Given
        UpdateExpenseRecordRequest request = new UpdateExpenseRecordRequest();
        request.setAmount(new BigDecimal("2000.00"));
        request.setExpenseType("LARGE_IRREGULAR");
        request.setDescription("更新后的描述");

        ExpenseRecordDTO updatedRecord = new ExpenseRecordDTO();
        updatedRecord.setId(1L);
        updatedRecord.setAmount(new BigDecimal("2000.00"));
        updatedRecord.setExpenseType("LARGE_IRREGULAR");

        when(expenseService.updateExpenseRecord(eq(1L), any(UpdateExpenseRecordRequest.class)))
                .thenReturn(updatedRecord);

        // When & Then
        mockMvc.perform(put("/expenses/records/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("支出记录更新成功"))
                .andExpect(jsonPath("$.data.amount").value(2000.00))
                .andExpect(jsonPath("$.data.expenseType").value("LARGE_IRREGULAR"));

        verify(expenseService).updateExpenseRecord(eq(1L), any(UpdateExpenseRecordRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/expenses/records/{id} - 应该成功删除记录")
    void shouldDeleteExpenseRecord() throws Exception {
        // Given
        doNothing().when(expenseService).deleteExpenseRecord(1L);

        // When & Then
        mockMvc.perform(delete("/expenses/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("支出记录删除成功"));

        verify(expenseService).deleteExpenseRecord(1L);
    }

    @Test
    @DisplayName("异常情况应该返回错误响应")
    void shouldReturnErrorResponseOnException() throws Exception {
        // Given
        when(expenseService.getAllCategories())
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/expenses/categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("IllegalArgumentException应该返回400")
    void shouldReturn400OnIllegalArgument() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Invalid category ID"))
                .when(expenseService).disableMinorCategory(999L);

        // When & Then
        mockMvc.perform(delete("/expenses/categories/minor/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid category ID"));
    }
}
