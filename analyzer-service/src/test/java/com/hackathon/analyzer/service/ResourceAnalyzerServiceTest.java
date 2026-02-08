package com.hackathon.analyzer.service;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.ResourceRecommendation;
import com.hackathon.analyzer.repository.AnalysisResultRepository;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import com.hackathon.analyzer.service.strategy.ConnectionPoolAnalysisStrategy;
import com.hackathon.analyzer.service.strategy.CpuAnalysisStrategy;
import com.hackathon.analyzer.service.strategy.MemoryAnalysisStrategy;
import com.hackathon.analyzer.service.strategy.ResourceAnalysisStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the refactored ResourceAnalyzerService (orchestrator).
 * Verifies that strategies are invoked and results are assembled correctly.
 */
@ExtendWith(MockitoExtension.class)
class ResourceAnalyzerServiceTest {

        @Mock
        private MetricsSnapshotRepository metricsRepository;

        @Mock
        private AnalysisResultRepository analysisRepository;

        private CostCalculationService costService;
        private ResourceAnalyzerService analyzerService;
        private List<MetricsSnapshot> mockSnapshots;

        @BeforeEach
        void setUp() {
                costService = new CostCalculationService();
                List<ResourceAnalysisStrategy> strategies = List.of(
                                new CpuAnalysisStrategy(),
                                new MemoryAnalysisStrategy(),
                                new ConnectionPoolAnalysisStrategy());
                analyzerService = new ResourceAnalyzerService(
                                metricsRepository, analysisRepository, costService, strategies);

                mockSnapshots = new ArrayList<>();
                for (int i = 0; i < 50; i++) {
                        MetricsSnapshot snapshot = MetricsSnapshot.builder()
                                        .serviceName("test-service")
                                        .timestamp(Instant.now().minusSeconds(i * 10))
                                        .cpuUsagePercent(50.0 + i)
                                        .heapUsagePercent(60.0 + i)
                                        .heapUsedBytes(512L * 1024 * 1024)
                                        .heapMaxBytes(1024L * 1024 * 1024)
                                        .hikariActiveConnections(5 + i)
                                        .hikariMaxConnections(20)
                                        .build();
                        mockSnapshots.add(snapshot);
                }
        }

        @Test
        @DisplayName("Should return recommendation with all dimensions populated")
        void analyzeService_withMetrics_shouldReturnRecommendation() {
                when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                                .thenReturn(mockSnapshots);
                when(analysisRepository.save(any(AnalysisResult.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResourceRecommendation result = analyzerService.analyzeService("test-service");

                assertThat(result).isNotNull();
                assertThat(result.getServiceName()).isEqualTo("test-service");
                assertThat(result.getConfidenceScore()).isGreaterThan(0.0);
                assertThat(result.getKubernetes()).isNotNull();
                assertThat(result.getJvm()).isNotNull();
                assertThat(result.getCostAnalysis()).isNotNull();

                verify(metricsRepository, times(1)).findByServiceNameAndTimestampAfter(anyString(), any(Instant.class));
                verify(analysisRepository, times(1)).save(any(AnalysisResult.class));
        }

        @Test
        @DisplayName("Should return default recommendation when no metrics available")
        void analyzeService_withoutMetrics_shouldReturnDefaultRecommendation() {
                when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                                .thenReturn(new ArrayList<>());

                ResourceRecommendation result = analyzerService.analyzeService("test-service");

                assertThat(result).isNotNull();
                assertThat(result.getServiceName()).isEqualTo("test-service");
                assertThat(result.getConfidenceScore()).isEqualTo(0.0);
                assertThat(result.getRationale()).isEqualTo("No metrics available for analysis");

                verify(metricsRepository, times(1)).findByServiceNameAndTimestampAfter(anyString(), any(Instant.class));
                verify(analysisRepository, never()).save(any(AnalysisResult.class));
        }

        @Test
        @DisplayName("Should detect CPU throttling with high CPU metrics")
        void analyzeService_withHighCpuUsage_shouldDetectThrottling() {
                List<MetricsSnapshot> highCpuSnapshots = new ArrayList<>();
                for (int i = 0; i < 50; i++) {
                        MetricsSnapshot snapshot = MetricsSnapshot.builder()
                                        .serviceName("test-service")
                                        .timestamp(Instant.now().minusSeconds(i * 10))
                                        .cpuUsagePercent(85.0 + i % 10)
                                        .heapUsagePercent(60.0)
                                        .heapUsedBytes(512L * 1024 * 1024)
                                        .heapMaxBytes(1024L * 1024 * 1024)
                                        .build();
                        highCpuSnapshots.add(snapshot);
                }

                when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                                .thenReturn(highCpuSnapshots);
                when(analysisRepository.save(any(AnalysisResult.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResourceRecommendation result = analyzerService.analyzeService("test-service");

                assertThat(result.getDetectedIssues()).containsKey("CPU Throttling");

                verify(analysisRepository, times(1)).save(any(AnalysisResult.class));
        }

        @Test
        @DisplayName("Should return analysis history from repository")
        void getAnalysisHistory_shouldReturnHistory() {
                List<AnalysisResult> mockHistory = List.of(
                                AnalysisResult.builder().serviceName("test-service").build());

                when(analysisRepository.findByServiceNameOrderByAnalysisTimestampDesc("test-service"))
                                .thenReturn(mockHistory);

                List<AnalysisResult> result = analyzerService.getAnalysisHistory("test-service");

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getServiceName()).isEqualTo("test-service");

                verify(analysisRepository, times(1))
                                .findByServiceNameOrderByAnalysisTimestampDesc("test-service");
        }

        @Test
        @DisplayName("Should return latest analysis when present")
        void getLatestAnalysis_shouldReturnLatest() {
                AnalysisResult mockResult = AnalysisResult.builder()
                                .serviceName("test-service")
                                .build();

                when(analysisRepository.findFirstByServiceNameOrderByAnalysisTimestampDesc("test-service"))
                                .thenReturn(Optional.of(mockResult));

                Optional<AnalysisResult> result = analyzerService.getLatestAnalysis("test-service");

                assertThat(result).isPresent();
                assertThat(result.get().getServiceName()).isEqualTo("test-service");

                verify(analysisRepository, times(1))
                                .findFirstByServiceNameOrderByAnalysisTimestampDesc("test-service");
        }

        @Test
        @DisplayName("Should return empty when no analysis exists")
        void getLatestAnalysis_whenNotFound_shouldReturnEmpty() {
                when(analysisRepository.findFirstByServiceNameOrderByAnalysisTimestampDesc("test-service"))
                                .thenReturn(Optional.empty());

                Optional<AnalysisResult> result = analyzerService.getLatestAnalysis("test-service");

                assertThat(result).isEmpty();

                verify(analysisRepository, times(1))
                                .findFirstByServiceNameOrderByAnalysisTimestampDesc("test-service");
        }
}
