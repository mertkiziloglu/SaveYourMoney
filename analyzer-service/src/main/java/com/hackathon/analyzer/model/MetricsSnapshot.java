package com.hackathon.analyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "metrics_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private Instant timestamp;

    // CPU metrics
    private Double cpuUsagePercent;
    private Double systemCpuUsagePercent;

    // Memory metrics
    private Long heapUsedBytes;
    private Long heapMaxBytes;
    private Double heapUsagePercent;
    private Long nonHeapUsedBytes;

    // GC metrics
    private Long gcPauseTimeMs;
    private Long gcCount;

    // Thread metrics
    private Integer threadCount;
    private Integer daemonThreadCount;

    // HTTP metrics
    private Long httpRequestCount;
    private Double httpRequestDurationAvg;
    private Double httpRequestDurationMax;
    private Double httpRequestDurationP95;
    private Double httpRequestDurationP99;

    // Connection pool metrics (for db-connection-service)
    private Integer hikariActiveConnections;
    private Integer hikariIdleConnections;
    private Integer hikariMaxConnections;
    private Integer hikariMinConnections;
    private Long hikariConnectionTimeout;
}
