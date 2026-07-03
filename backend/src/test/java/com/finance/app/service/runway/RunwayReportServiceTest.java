package com.finance.app.service.runway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.RunwayReportDetailDTO;
import com.finance.app.dto.RunwayReportSummaryDTO;
import com.finance.app.dto.RunwayTrendDTO;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.ExpenseCategoryMajor;
import com.finance.app.model.RunwayReport;
import com.finance.app.repository.ExpenseCategoryMajorRepository;
import com.finance.app.repository.RunwayReportRepository;
import com.finance.app.service.RunwayReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RunwayReportService Tests")
class RunwayReportServiceTest {

    @Mock
    private RunwayReportRepository runwayReportRepository;

    @Mock
    private ExpenseCategoryMajorRepository expenseCategoryMajorRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RunwayReportService runwayReportService;

    /** Build a report whose snapshotJson carries the given metrics. */
    private RunwayReport reportWith(Long id, Long familyId, LocalDateTime savedAt,
                                    long liquidTotal, long monthlyBurn, int runwayMonths,
                                    String depletionDate, String expenseBreakdownJson) {
        RunwayReport r = new RunwayReport();
        r.setId(id);
        r.setFamilyId(familyId);
        r.setReportName("runway-report-" + id);
        r.setSavedAt(savedAt);
        r.setSnapshotJson("{\"version\":\"1\",\"snapshot\":{"
                + "\"liquidTotal\":" + liquidTotal + ","
                + "\"monthlyBurn\":" + monthlyBurn + ","
                + "\"runwayMonths\":" + runwayMonths + ","
                + "\"depletionDate\":\"" + depletionDate + "\","
                + "\"expenseBreakdown\":" + expenseBreakdownJson + "}}");
        return r;
    }

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

    @Test
    @DisplayName("getTrend: 多份报告返回按 savedAt 升序的点，并抽取快照指标")
    void getTrend_multipleReports_returnsAscendingPointsWithMetrics() {
        // Given
        Long familyId = 1L;
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 3, 2, 10, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 6, 20, 10, 0);
        // repository returns newest-first (as findByFamilyIdOrderBySavedAtDesc does)
        when(runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)).thenReturn(List.of(
                reportWith(3L, familyId, t3, 168500, 12600, 13, "2027-08", "{\"RENT\":6800}"),
                reportWith(2L, familyId, t2, 175000, 11800, 15, "2027-06", "{\"RENT\":6800}"),
                reportWith(1L, familyId, t1, 180000, 11000, 16, "2027-05", "{\"RENT\":6800}")
        ));

        // When
        RunwayTrendDTO trend = runwayReportService.getTrend(familyId);

        // Then
        assertNotNull(trend);
        assertEquals(3, trend.points().size());
        // ascending by savedAt
        assertEquals(t1, trend.points().get(0).savedAt());
        assertEquals(t2, trend.points().get(1).savedAt());
        assertEquals(t3, trend.points().get(2).savedAt());
        // metrics from the latest point
        RunwayTrendDTO.TrendPoint latest = trend.points().get(2);
        assertEquals(0, new BigDecimal("168500").compareTo(latest.liquidTotal()));
        assertEquals(0, new BigDecimal("12600").compareTo(latest.monthlyBurn()));
        assertEquals(13, latest.runwayMonths());
        assertEquals("2027-08", latest.depletionDate());
    }

    @Test
    @DisplayName("getTrend: 无报告返回空点列表，不报错")
    void getTrend_noReports_returnsEmptyPoints() {
        // Given
        Long familyId = 1L;
        when(runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)).thenReturn(List.of());

        // When
        RunwayTrendDTO trend = runwayReportService.getTrend(familyId);

        // Then
        assertNotNull(trend);
        assertTrue(trend.points().isEmpty());
        assertTrue(trend.categories().isEmpty());
    }

    @Test
    @DisplayName("getTrend: 单份坏 JSON 被跳过，有效点仍返回")
    void getTrend_corruptSnapshotSkipped_validPointsReturned() {
        // Given
        Long familyId = 1L;
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 3, 2, 10, 0);
        RunwayReport corrupt = new RunwayReport();
        corrupt.setId(9L);
        corrupt.setFamilyId(familyId);
        corrupt.setReportName("runway-report-corrupt");
        corrupt.setSavedAt(t2);
        corrupt.setSnapshotJson("{not valid json");
        when(runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)).thenReturn(List.of(
                corrupt,
                reportWith(1L, familyId, t1, 180000, 11000, 16, "2027-05", "{\"RENT\":6800}")
        ));

        // When
        RunwayTrendDTO trend = runwayReportService.getTrend(familyId);

        // Then — corrupt dropped, valid remains
        assertEquals(1, trend.points().size());
        assertEquals(t1, trend.points().get(0).savedAt());
    }

    @Test
    @DisplayName("getTrend: 最新报告分类富化 name/color，按金额降序，未知 code 兜底")
    void getTrend_latestCategories_enrichedSortedWithFallback() {
        // Given
        Long familyId = 1L;
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 20, 10, 0); // latest
        when(runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)).thenReturn(List.of(
                reportWith(2L, familyId, t2, 168500, 12600, 13, "2027-08",
                        "{\"RENT\":6800,\"FOOD\":3200,\"MYSTERY\":500}"),
                reportWith(1L, familyId, t1, 180000, 11000, 16, "2027-05", "{\"RENT\":6800}")
        ));
        ExpenseCategoryMajor rent = new ExpenseCategoryMajor();
        rent.setCode("RENT"); rent.setName("房租"); rent.setColor("hsl(142 76% 36%)");
        ExpenseCategoryMajor food = new ExpenseCategoryMajor();
        food.setCode("FOOD"); food.setName("餐饮"); food.setColor("hsl(217 91% 60%)");
        when(expenseCategoryMajorRepository.findAll()).thenReturn(List.of(rent, food));

        // When
        RunwayTrendDTO trend = runwayReportService.getTrend(familyId);

        // Then — from latest report only, sorted amount desc
        List<RunwayTrendDTO.CategoryItem> cats = trend.categories();
        assertEquals(3, cats.size());
        assertEquals("RENT", cats.get(0).code());
        assertEquals("房租", cats.get(0).name());
        assertEquals("hsl(142 76% 36%)", cats.get(0).color());
        assertEquals(0, new BigDecimal("6800").compareTo(cats.get(0).amount()));
        assertEquals("FOOD", cats.get(1).code());
        assertEquals("餐饮", cats.get(1).name());
        // unknown code kept with fallback name + non-null color
        assertEquals("MYSTERY", cats.get(2).code());
        assertEquals("MYSTERY", cats.get(2).name());
        assertNotNull(cats.get(2).color());
    }

    @Test
    @DisplayName("getTrend: previousCategories 取次新报告，用于环比；仅一份报告时为空")
    void getTrend_previousCategories_fromSecondLatest() {
        // Given two reports; previous should come from the OLDER one
        Long familyId = 1L;
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 15, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 20, 10, 0);
        when(runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)).thenReturn(List.of(
                reportWith(2L, familyId, t2, 168500, 12600, 13, "2027-08", "{\"RENT\":6800,\"FOOD\":3200}"),
                reportWith(1L, familyId, t1, 180000, 11000, 16, "2027-05", "{\"RENT\":6500}")
        ));
        ExpenseCategoryMajor rent = new ExpenseCategoryMajor();
        rent.setCode("RENT"); rent.setName("房租"); rent.setColor("hsl(142 76% 36%)");
        when(expenseCategoryMajorRepository.findAll()).thenReturn(List.of(rent));

        // When
        RunwayTrendDTO trend = runwayReportService.getTrend(familyId);

        // Then — previous = older report's single RENT category
        assertEquals(1, trend.previousCategories().size());
        assertEquals("RENT", trend.previousCategories().get(0).code());
        assertEquals(0, new BigDecimal("6500").compareTo(trend.previousCategories().get(0).amount()));
    }

    @Test
    @DisplayName("getTrend: 仅一份报告时 previousCategories 为空")
    void getTrend_singleReport_previousCategoriesEmpty() {
        Long familyId = 1L;
        when(runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)).thenReturn(List.of(
                reportWith(1L, familyId, LocalDateTime.of(2026, 6, 20, 10, 0),
                        168500, 12600, 13, "2027-08", "{\"RENT\":6800}")
        ));
        when(expenseCategoryMajorRepository.findAll()).thenReturn(List.of());

        RunwayTrendDTO trend = runwayReportService.getTrend(familyId);

        assertTrue(trend.previousCategories().isEmpty());
    }
}
