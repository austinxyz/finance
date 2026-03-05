package com.finance.app.repository;

import com.finance.app.model.RunwayReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunwayReportRepository extends JpaRepository<RunwayReport, Long> {

    List<RunwayReport> findByFamilyIdOrderBySavedAtDesc(Long familyId);

    long countByFamilyIdAndReportNameStartingWith(Long familyId, String prefix);
}
