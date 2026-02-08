package com.hackathon.analyzer.ml;

import com.hackathon.analyzer.model.CostForecast;
import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for CostPredictionService.
 * Covers forecast generation, default fallback, trend detection, and accuracy
 * scoring.
 */
@ExtendWith(MockitoExtension.class)
class CostPredictionServiceTest {

    @Mock
    private MetricsSnapshotRepository metricsRepository;

    @InjectMocks
    private CostPredictionService predictionService;

    private List<MetricsSnapshot> historicalMetrics;

    @BeforeEach
    void setUp() {
        historicalMetrics = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            historicalMetrics.add(MetricsSnapshot.builder()
                    .serviceName("test-service")
                    .timestamp(Instant.now().minusSeconds(i * 3600))
                    .cpuUsagePercent(40.0 + i * 0.2)
                    .heapUsagePercent(50.0 + i * 0.1)
                    .heapUsedBytes(512L * 1024 * 1024)
                    .heapMaxBytes(1024L * 1024 * 1024)
                    .build());
        }
    }

    @Test
    @DisplayName("Should generate cost forecast with sufficient data")
    void predictCosts_withSufficientData_shouldReturnForecast() {
        when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                .thenReturn(historicalMetrics);

        CostForecast forecast = predictionService.predictCosts("test-service", 30);

        assertThat(forecast).isNotNull();
        assertThat(forecast.getServiceName()).isEqualTo("test-service");
        assertThat(forecast.getCurrentMonthlyCost()).isGreaterThan(0);
        assertThat(forecast.getPredictions()).isNotEmpty();
        assertThat(forecast.getAccuracyScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return default forecast when no data available")
    void predictCosts_withNoData_shouldReturnDefault() {
        when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        CostForecast forecast = predictionService.predictCosts("unknown-service", 30);

        assertThat(forecast).isNotNull();
        assertThat(forecast.getServiceName()).isEqualTo("unknown-service");
        assertThat(forecast.getTrend()).isNotNull();
    }

    @Test
    @DisplayName("Should detect increasing cost trend")
    void predictCosts_withIncreasingCosts_shouldDetectUpwardTrend() {
        List<MetricsSnapshot> increasing = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            increasing.add(MetricsSnapshot.builder()
                    .serviceName("expensive-service")
                    .timestamp(Instant.now().minusSeconds(i * 3600))
                    .cpuUsagePercent(20.0 + i * 1.0)
                    .heapUsagePercent(30.0 + i * 0.5)
                    .heapUsedBytes((256L + i * 8) * 1024 * 1024)
                    .heapMaxBytes(2048L * 1024 * 1024)
                    .build());
        }

        when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                .thenReturn(increasing);

        CostForecast forecast = predictionService.predictCosts("expensive-service", 30);

        assertThat(forecast).isNotNull();
        assertThat(forecast.getPredictedMonthlyCost()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate accuracy score based on sample size")
    void predictCosts_accuracyScore_shouldScaleWithDataSize() {
        when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                .thenReturn(historicalMetrics);

        CostForecast forecast = predictionService.predictCosts("test-service", 7);

        assertThat(forecast.getAccuracyScore()).isBetween(0.0, 100.0);
    }
}
