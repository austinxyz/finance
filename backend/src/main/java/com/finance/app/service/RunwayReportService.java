package com.finance.app.service;

import com.finance.app.dto.RunwayReportDetailDTO;
import com.finance.app.dto.RunwayReportSummaryDTO;
import com.finance.app.exception.UnauthorizedException;
import com.finance.app.model.RunwayReport;
import com.finance.app.repository.RunwayReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunwayReportService {

    private final RunwayReportRepository runwayReportRepository;

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

    private RunwayReportSummaryDTO toSummaryDTO(RunwayReport report) {
        return new RunwayReportSummaryDTO(report.getId(), report.getReportName(), report.getSavedAt());
    }

    private RunwayReportDetailDTO toDetailDTO(RunwayReport report) {
        return new RunwayReportDetailDTO(report.getId(), report.getReportName(), report.getSavedAt(), report.getSnapshotJson());
    }
}
