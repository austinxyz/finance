package com.finance.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.app.dto.RunwayReportDetailDTO;
import com.finance.app.dto.RunwayReportSummaryDTO;
import com.finance.app.dto.RunwayTrendDTO;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.ExpenseCategoryMajor;
import com.finance.app.model.RunwayReport;
import com.finance.app.repository.ExpenseCategoryMajorRepository;
import com.finance.app.repository.RunwayReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunwayReportService {

    /** 未知分类的兜底颜色（中性灰）。 */
    private static final String DEFAULT_CATEGORY_COLOR = "hsl(215 20% 65%)";

    private final RunwayReportRepository runwayReportRepository;
    private final ExpenseCategoryMajorRepository expenseCategoryMajorRepository;
    private final ObjectMapper objectMapper;

    public RunwayReportSummaryDTO saveReport(Long familyId, String snapshotJson) {
        String baseName = "runway-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "-report";
        String reportName = baseName;

        long existing = runwayReportRepository.countByFamilyIdAndReportNameStartingWith(familyId, baseName);
        if (existing > 0) {
            reportName = baseName + "-" + (existing + 1);
        }

        RunwayReport report = new RunwayReport();
        report.setFamilyId(familyId);
        report.setReportName(reportName);
        report.setSavedAt(LocalDateTime.now());
        report.setSnapshotJson(snapshotJson);

        RunwayReport saved = runwayReportRepository.save(report);
        log.info("保存资金跑道报告: familyId={}, reportName={}", familyId, reportName);
        return toSummaryDTO(saved);
    }

    public List<RunwayReportSummaryDTO> listReports(Long familyId) {
        return runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId)
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    public RunwayReportDetailDTO getReport(Long id, Long familyId) {
        RunwayReport report = runwayReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报告不存在: " + id));
        if (!report.getFamilyId().equals(familyId)) {
            throw new UnauthorizedException("无权访问其他家庭的报告");
        }
        return toDetailDTO(report);
    }

    public void deleteReport(Long id, Long familyId) {
        RunwayReport report = runwayReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("报告不存在: " + id));
        if (!report.getFamilyId().equals(familyId)) {
            throw new UnauthorizedException("无权删除其他家庭的报告");
        }
        runwayReportRepository.delete(report);
        log.info("删除资金跑道报告: id={}, familyId={}", id, familyId);
    }

    /**
     * 把该家庭已保存的全部跑道报告快照汇总成趋势：按 savedAt 升序的指标点，
     * 以及最新报告的分类支出明细。坏 JSON 的报告被跳过，不影响整体。
     */
    public RunwayTrendDTO getTrend(Long familyId) {
        // repository 返回最新在前
        List<RunwayReport> reports = runwayReportRepository.findByFamilyIdOrderBySavedAtDesc(familyId);

        List<RunwayTrendDTO.TrendPoint> points = new ArrayList<>();
        JsonNode latestSnapshot = null;
        JsonNode previousSnapshot = null;
        int parsableCount = 0;

        for (RunwayReport report : reports) {
            JsonNode snapshot = parseSnapshot(report);
            if (snapshot == null) continue;
            points.add(new RunwayTrendDTO.TrendPoint(
                    report.getSavedAt(),
                    report.getReportName(),
                    decimalOrNull(snapshot.path("liquidTotal")),
                    decimalOrNull(snapshot.path("monthlyBurn")),
                    snapshot.path("runwayMonths").isNumber() ? snapshot.path("runwayMonths").intValue() : null,
                    snapshot.path("depletionDate").isTextual() ? snapshot.path("depletionDate").asText() : null
            ));
            // list is newest-first: first parsable = latest, second = previous
            if (parsableCount == 0) latestSnapshot = snapshot;
            else if (parsableCount == 1) previousSnapshot = snapshot;
            parsableCount++;
        }

        points.sort(Comparator.comparing(RunwayTrendDTO.TrendPoint::savedAt));

        // Load the category lookup once (shared by latest + previous enrichment).
        Map<String, ExpenseCategoryMajor> byCode = (latestSnapshot == null && previousSnapshot == null)
                ? Map.of()
                : expenseCategoryMajorRepository.findAll().stream()
                        .collect(Collectors.toMap(ExpenseCategoryMajor::getCode, c -> c, (a, b) -> a));

        List<RunwayTrendDTO.CategoryItem> categories = buildCategories(latestSnapshot, byCode);
        List<RunwayTrendDTO.CategoryItem> previousCategories = buildCategories(previousSnapshot, byCode);
        return new RunwayTrendDTO(points, categories, previousCategories);
    }

    /** 解析一份报告的 snapshot 节点；坏 JSON 返回 null（跳过该报告）。 */
    private JsonNode parseSnapshot(RunwayReport report) {
        try {
            return objectMapper.readTree(report.getSnapshotJson()).path("snapshot");
        } catch (Exception e) {
            log.warn("跳过无法解析的跑道快照: reportId={}", report.getId());
            return null;
        }
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node.isNumber()) return node.decimalValue();
        if (node.isTextual()) {
            try {
                return new BigDecimal(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 某份报告的 expenseBreakdown（code→amount）按 code join 大类，富化 name/color，按金额降序。 */
    private List<RunwayTrendDTO.CategoryItem> buildCategories(
            JsonNode snapshot, Map<String, ExpenseCategoryMajor> byCode) {
        if (snapshot == null) return List.of();
        JsonNode breakdown = snapshot.path("expenseBreakdown");
        if (!breakdown.isObject() || breakdown.isEmpty()) return List.of();

        List<RunwayTrendDTO.CategoryItem> items = new ArrayList<>();
        breakdown.fields().forEachRemaining(entry -> {
            String code = entry.getKey();
            BigDecimal amount = decimalOrNull(entry.getValue());
            if (amount == null) return;
            ExpenseCategoryMajor major = byCode.get(code);
            String name = major != null ? major.getName() : code;
            String color = major != null && major.getColor() != null ? major.getColor() : DEFAULT_CATEGORY_COLOR;
            items.add(new RunwayTrendDTO.CategoryItem(code, name, color, amount));
        });
        items.sort(Comparator.comparing(RunwayTrendDTO.CategoryItem::amount).reversed());
        return items;
    }

    private RunwayReportSummaryDTO toSummaryDTO(RunwayReport report) {
        return new RunwayReportSummaryDTO(report.getId(), report.getReportName(), report.getSavedAt());
    }

    private RunwayReportDetailDTO toDetailDTO(RunwayReport report) {
        return new RunwayReportDetailDTO(report.getId(), report.getReportName(), report.getSavedAt(), report.getSnapshotJson());
    }
}
