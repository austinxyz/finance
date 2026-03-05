package com.finance.app.controller.runway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.controller.RunwayReportController;
import com.finance.app.dto.RunwayReportSummaryDTO;
import com.finance.app.dto.SaveRunwayReportRequest;
import com.finance.app.security.AuthHelper;
import com.finance.app.service.RunwayReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = RunwayReportController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("RunwayReportController Tests")
class RunwayReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RunwayReportService runwayReportService;

    @MockBean
    private AuthHelper authHelper;

    @Test
    @DisplayName("应该成功保存报告并返回 HTTP 200 和 success:true")
    void shouldSaveReportAndReturn200() throws Exception {
        // Given
        SaveRunwayReportRequest request = new SaveRunwayReportRequest();
        request.setFamilyId(1L);
        request.setSnapshotJson("{\"version\":\"1\"}");

        RunwayReportSummaryDTO dto = new RunwayReportSummaryDTO(1L, "runway-2026-03-04-report", LocalDateTime.now());
        doNothing().when(authHelper).requireFamilyAccess(anyString(), eq(1L));
        when(runwayReportService.saveReport(eq(1L), anyString())).thenReturn(dto);

        // When / Then
        mockMvc.perform(post("/runway/reports")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportName").value("runway-2026-03-04-report"));
    }

    @Test
    @DisplayName("应该返回家庭的报告列表并包含 HTTP 200 和 success:true")
    void shouldListReportsAndReturn200() throws Exception {
        // Given
        List<RunwayReportSummaryDTO> reports = Arrays.asList(
                new RunwayReportSummaryDTO(1L, "runway-2026-03-04-report", LocalDateTime.now()),
                new RunwayReportSummaryDTO(2L, "runway-2026-03-03-report", LocalDateTime.now().minusDays(1))
        );
        doNothing().when(authHelper).requireFamilyAccess(anyString(), eq(1L));
        when(runwayReportService.listReports(1L)).thenReturn(reports);

        // When / Then
        mockMvc.perform(get("/runway/reports")
                        .param("familyId", "1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
