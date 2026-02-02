package com.hackathon.analyzer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private Instant analysisTimestamp;

    // Current configuration
    private String currentCpuRequest;
    private String currentCpuLimit;
    private String currentMemoryRequest;
    private String currentMemoryLimit;

    // Recommended configuration
    private String recommendedCpuRequest;
    private String recommendedCpuLimit;
    private String recommendedMemoryRequest;
    private String recommendedMemoryLimit;

    // JVM recommendations
    private String recommendedJvmXms;
    private String recommendedJvmXmx;

    // Connection pool recommendations
    private Integer recommendedMaxPoolSize;
    private Integer recommendedMinIdle;

    // Analysis metrics
    private Double p95CpuUsage;
    private Double p99CpuUsage;
    private Double maxCpuUsage;
    private Double p95MemoryUsage;
    private Double p99MemoryUsage;
    private Double maxMemoryUsage;

    // Issues detected
    private Boolean cpuThrottlingDetected;
    private Boolean memoryLeakDetected;
    private Boolean connectionPoolExhaustion;

    // Cost savings
    private Double estimatedMonthlySavings;
    private Double confidenceScore;
}
