package com.finance.app.service.runway;

import com.finance.app.dto.RunwayReportDetailDTO;
import com.finance.app.dto.RunwayReportSummaryDTO;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.RunwayReport;
import com.finance.app.repository.RunwayReportRepository;
import com.finance.app.service.RunwayReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RunwayReportService Tests")
class RunwayReportServiceTest {

    @Mock
    private RunwayReportRepository runwayReportRepository;

    @InjectMocks
    private RunwayReportService runwayReportService;

    @Test
    @DisplayName("应该生成正确的报告名称并保存")
    void shouldGenerateCorrectReportNameWhenNoExistingReports() {
        // Given
        Long familyId = 1L;
        String snapshotJson = "{\"version\":\"1\"}";
        when(runwayReportRepository.countByFamilyIdAndReportNameStartingWith(eq(familyId), anyString())).thenReturn(0L);
        RunwayReport saved = new RunwayReport();
        saved.setId(1L);
        saved.setFamilyId(familyId);
        saved.setReportName("runway-2026-03-04-report");
        saved.setSavedAt(LocalDateTime.now());
        saved.setSnapshotJson(snapshotJson);
        when(runwayReportRepository.save(any(RunwayReport.class))).thenReturn(saved);

        // When
        RunwayReportSummaryDTO result = runwayReportService.saveReport(familyId, snapshotJson);

        // Then
        assertNotNull(result);
        assertTrue(result.getReportName().startsWith("runway-"));
        assertTrue(result.getReportName().endsWith("-report"));
        ArgumentCaptor<RunwayReport> captor = ArgumentCaptor.forClass(RunwayReport.class);
        verify(runwayReportRepository).save(captor.capture());
        assertEquals(familyId, captor.getValue().getFamilyId());
        assertEquals(snapshotJson, captor.getValue().getSnapshotJson());
        assertNotNull(captor.getValue().getSavedAt());
    }

    @Test
    @DisplayName("应该在已存在同名报告时追加序号后缀")
    void shouldAppendSuffixWhenReportNameAlreadyExists() {
        // Given
        Long familyId = 1L;
        when(runwayReportRepository.countByFamilyIdAndReportNameStartingWith(eq(familyId), anyString())).thenReturn(2L);
        RunwayReport saved = new RunwayReport();
        saved.setId(2L);
        saved.setFamilyId(familyId);
        saved.setReportName("runway-2026-03-04-report-3");
        saved.setSavedAt(LocalDateTime.now());
        saved.setSnapshotJson("{}");
        when(runwayReportRepository.save(any(RunwayReport.class))).thenReturn(saved);

        // When
        RunwayReportSummaryDTO result = runwayReportService.saveReport(familyId, "{}");

        // Then
        ArgumentCaptor<RunwayReport> captor = ArgumentCaptor.forClass(RunwayReport.class);
        verify(runwayReportRepository).save(captor.capture());
        assertTrue(captor.getValue().getReportName().endsWith("-3"));
    }

    @Test
    @DisplayName("应该在家庭ID不匹配时拒绝访问报告")
    void shouldThrowUnauthorizedWhenFamilyIdMismatch() {
        // Given
        Long reportFamilyId = 1L;
        Long requestingFamilyId = 2L;
        RunwayReport report = new RunwayReport();
        report.setId(10L);
        report.setFamilyId(reportFamilyId);
        report.setReportName("runway-2026-03-04-report");
        report.setSavedAt(LocalDateTime.now());
        report.setSnapshotJson("{}");
        when(runwayReportRepository.findById(10L)).thenReturn(Optional.of(report));

        // When / Then
        assertThrows(UnauthorizedException.class, () ->
                runwayReportService.getReport(10L, requestingFamilyId));
    }

    @Test
    @DisplayName("应该在家庭ID匹配时成功返回报告详情")
    void shouldReturnReportDetailWhenFamilyIdMatches() {
        // Given
        Long familyId = 1L;
        RunwayReport report = new RunwayReport();
        report.setId(10L);
        report.setFamilyId(familyId);
        report.setReportName("runway-2026-03-04-report");
        report.setSavedAt(LocalDateTime.now());
        report.setSnapshotJson("{\"version\":\"1\"}");
        when(runwayReportRepository.findById(10L)).thenReturn(Optional.of(report));

        // When
        RunwayReportDetailDTO result = runwayReportService.getReport(10L, familyId);

        // Then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("{\"version\":\"1\"}", result.getSnapshotJson());
    }
}
