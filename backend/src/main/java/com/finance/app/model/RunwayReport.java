package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "runway_reports", indexes = {
        @Index(name = "idx_runway_reports_family_id", columnList = "family_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunwayReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;
}
