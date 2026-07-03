package com.finance.app.controller.runway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.controller.RunwayReportController;
import com.finance.app.dto.RunwayTrendDTO;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.RunwayReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RunwayReportController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("RunwayReportController Trend Tests")
class RunwayReportTrendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RunwayReportService runwayReportService;

    @MockBean
    private AuthHelper authHelper;

    @Test
    @DisplayName("GET /runway/reports/trend 返回 200，success:true，含 points/categories，且不含 snapshotJson")
    void shouldReturnTrendWith200AndNoRawSnapshot() throws Exception {
        // Given
        RunwayTrendDTO trend = new RunwayTrendDTO(
                List.of(new RunwayTrendDTO.TrendPoint(
                        LocalDateTime.of(2026, 6, 20, 10, 0), "runway-report-2",
                        new BigDecimal("168500"), new BigDecimal("12600"), 13, "2027-08")),
                List.of(new RunwayTrendDTO.CategoryItem("RENT", "房租", "hsl(142 76% 36%)", new BigDecimal("6800")))
        );
        doNothing().when(authHelper).requireFamilyAccess(anyString(), eq(1L));
        when(runwayReportService.getTrend(1L)).thenReturn(trend);

        // When / Then
        mockMvc.perform(get("/runway/reports/trend")
                        .param("familyId", "1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.points").isArray())
                .andExpect(jsonPath("$.data.points.length()").value(1))
                .andExpect(jsonPath("$.data.points[0].runwayMonths").value(13))
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.categories[0].name").value("房租"))
                .andExpect(content().string(not(containsString("snapshotJson"))));
    }

    @Test
    @DisplayName("GET /runway/reports/trend 家庭访问被拒时返回 success:false")
    void shouldRejectWhenFamilyAccessDenied() throws Exception {
        // Given
        doThrow(new RuntimeException("无权访问")).when(authHelper).requireFamilyAccess(anyString(), eq(2L));

        // When / Then
        mockMvc.perform(get("/runway/reports/trend")
                        .param("familyId", "2")
                        .header("Authorization", "Bearer token"))
                .andExpect(jsonPath("$.success").value(false));
        verify(runwayReportService, never()).getTrend(anyLong());
    }
}
