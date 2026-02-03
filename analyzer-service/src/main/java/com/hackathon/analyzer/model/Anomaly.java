package com.hackathon.analyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomalies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType metricType;

    @Column(nullable = false)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalySeverity severity;

    @Column(nullable = false)
    private Double actualValue;

    @Column(nullable = false)
    private Double expectedValue;

    @Column(nullable = false)
    private Double zScore;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
