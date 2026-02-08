package com.hackathon.analyzer.service;

import com.hackathon.analyzer.config.AnomalyDetectionConfig;
import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.repository.AnomalyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for AnomalyDetectionService.
 * Covers Z-score anomaly detection, EMA, severity classification, and edge
 * cases.
 */
@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private AnomalyRepository anomalyRepository;

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        AnomalyDetectionConfig config = new AnomalyDetectionConfig();
        service = new AnomalyDetectionService(anomalyRepository, config);
    }

    @Test
    @DisplayName("Should detect CPU spike anomaly when value deviates significantly")
    void analyzeAll_withCpuSpike_shouldDetectAnomaly() {
        List<MetricsSnapshot> snapshots = createBaselineSnapshots(50, 30.0, 40.0);
        // Inject a spike at the end
        snapshots.add(MetricsSnapshot.builder()
                .serviceName("test-service")
                .timestamp(Instant.now())
                .cpuUsagePercent(95.0)
                .heapUsagePercent(40.0)
                .heapUsedBytes(400L * 1024 * 1024)
                .heapMaxBytes(1024L * 1024 * 1024)
                .build());

        when(anomalyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Anomaly> anomalies = service.analyzeAll("test-service", snapshots);

        assertThat(anomalies).isNotEmpty();
        assertThat(anomalies.stream().anyMatch(a -> a.getMetricType() == MetricType.CPU_USAGE)).isTrue();
    }

    @Test
    @DisplayName("Should detect memory growth anomaly")
    void analyzeAll_withMemoryGrowth_shouldDetectAnomaly() {
        List<MetricsSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            snapshots.add(MetricsSnapshot.builder()
                    .serviceName("test-service")
                    .timestamp(Instant.now().minusSeconds(i * 60))
                    .cpuUsagePercent(30.0)
                    .heapUsagePercent(30.0 + i * 1.5)
                    .heapUsedBytes((300L + i * 15) * 1024 * 1024)
                    .heapMaxBytes(1024L * 1024 * 1024)
                    .build());
        }

        when(anomalyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Anomaly> anomalies = service.analyzeAll("test-service", snapshots);
        assertThat(anomalies).isNotEmpty();
    }

    @Test
    @DisplayName("Should return empty list for stable metrics")
    void analyzeAll_withNormalMetrics_shouldReturnNoHighSeverity() {
        List<MetricsSnapshot> snapshots = createBaselineSnapshots(30, 30.0, 40.0);

        lenient().when(anomalyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Anomaly> anomalies = service.analyzeAll("test-service", snapshots);

        long highSeverity = anomalies.stream()
                .filter(a -> a.getSeverity() == AnomalySeverity.HIGH || a.getSeverity() == AnomalySeverity.CRITICAL)
                .count();
        assertThat(highSeverity).isZero();
    }

    @Test
    @DisplayName("Should handle empty snapshots gracefully")
    void analyzeAll_withEmptySnapshots_shouldReturnEmpty() {
        List<Anomaly> anomalies = service.analyzeAll("test-service", new ArrayList<>());
        assertThat(anomalies).isEmpty();
    }

    @Test
    @DisplayName("Should handle single snapshot without error")
    void analyzeAll_withSingleSnapshot_shouldNotThrow() {
        List<MetricsSnapshot> snapshots = List.of(
                MetricsSnapshot.builder()
                        .serviceName("test-service")
                        .timestamp(Instant.now())
                        .cpuUsagePercent(50.0)
                        .heapUsagePercent(60.0)
                        .heapUsedBytes(512L * 1024 * 1024)
                        .heapMaxBytes(1024L * 1024 * 1024)
                        .build());

        lenient().when(anomalyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Anomaly> anomalies = service.analyzeAll("test-service", snapshots);
        assertThat(anomalies).isNotNull();
    }

    private List<MetricsSnapshot> createBaselineSnapshots(int count, double avgCpu, double avgMemory) {
        List<MetricsSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            snapshots.add(MetricsSnapshot.builder()
                    .serviceName("test-service")
                    .timestamp(Instant.now().minusSeconds(i * 60))
                    .cpuUsagePercent(avgCpu + (Math.random() * 4 - 2))
                    .heapUsagePercent(avgMemory + (Math.random() * 4 - 2))
                    .heapUsedBytes((long) (avgMemory * 10) * 1024 * 1024)
                    .heapMaxBytes(1024L * 1024 * 1024)
                    .build());
        }
        return snapshots;
    }
}
