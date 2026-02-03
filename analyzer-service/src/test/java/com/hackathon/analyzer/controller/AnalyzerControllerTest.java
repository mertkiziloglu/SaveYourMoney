package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.collector.MetricsCollectorService;
import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.repository.AnomalyRepository;
import com.hackathon.analyzer.service.ResourceAnalyzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyzerController.class)
class AnalyzerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceAnalyzerService analyzerService;

    @MockBean
    private MetricsCollectorService metricsCollector;

    @MockBean
    private AnomalyRepository anomalyRepository;

    private ResourceRecommendation mockRecommendation;
    private AnalysisResult mockAnalysisResult;
    private Anomaly mockAnomaly;

    @BeforeEach
    void setUp() {
        mockRecommendation = ResourceRecommendation.builder()
                .serviceName("test-service")
                .confidenceScore(0.85)
                .rationale("Test rationale")
                .detectedIssues(new HashMap<>())
                .costAnalysis(ResourceRecommendation.CostAnalysis.builder()
                        .currentMonthlyCost(100.0)
                        .recommendedMonthlyCost(60.0)
                        .monthlySavings(40.0)
                        .annualSavings(480.0)
                        .savingsPercentage(40)
                        .build())
                .build();

        mockAnalysisResult = AnalysisResult.builder()
                .serviceName("test-service")
                .analysisTimestamp(Instant.now())
                .currentCpuRequest("100m")
                .currentMemoryRequest("256Mi")
                .recommendedCpuRequest("200m")
                .recommendedMemoryRequest("512Mi")
                .estimatedMonthlySavings(40.0)
                .confidenceScore(0.85)
                .build();

        mockAnomaly = Anomaly.builder()
                .id(1L)
                .serviceName("test-service")
                .metricType(MetricType.CPU_USAGE)
                .metricName("cpu_usage_percent")
                .anomalyType(AnomalyType.SPIKE)
                .severity(AnomalySeverity.HIGH)
                .actualValue(95.0)
                .expectedValue(20.0)
                .zScore(3.5)
                .threshold(2.0)
                .detectedAt(LocalDateTime.now())
                .description("CPU spike detected")
                .resolved(false)
                .build();
    }

    @Test
    void health_shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("analyzer-service"));
    }

    @Test
    void analyzeService_shouldReturnRecommendation() throws Exception {
        when(analyzerService.analyzeService("test-service")).thenReturn(mockRecommendation);

        mockMvc.perform(post("/api/analyze/test-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.confidenceScore").value(0.85));

        verify(analyzerService, times(1)).analyzeService("test-service");
    }

    @Test
    void analyzeAllServices_shouldReturnAllRecommendations() throws Exception {
        when(analyzerService.analyzeService(anyString())).thenReturn(mockRecommendation);

        mockMvc.perform(post("/api/analyze-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['cpu-hungry-service']").exists())
                .andExpect(jsonPath("$.['memory-leaker-service']").exists())
                .andExpect(jsonPath("$.['db-connection-service']").exists());

        verify(analyzerService, times(3)).analyzeService(anyString());
    }

    @Test
    void getAnalysisHistory_shouldReturnHistoryList() throws Exception {
        List<AnalysisResult> history = Arrays.asList(mockAnalysisResult);
        when(analyzerService.getAnalysisHistory("test-service")).thenReturn(history);

        mockMvc.perform(get("/api/analysis-history/test-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("test-service"));

        verify(analyzerService, times(1)).getAnalysisHistory("test-service");
    }

    @Test
    void getLatestAnalysis_shouldReturnLatestResult() throws Exception {
        when(analyzerService.getLatestAnalysis("test-service"))
                .thenReturn(Optional.of(mockAnalysisResult));

        mockMvc.perform(get("/api/latest-analysis/test-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"));

        verify(analyzerService, times(1)).getLatestAnalysis("test-service");
    }

    @Test
    void getLatestAnalysis_shouldReturn404WhenNotFound() throws Exception {
        when(analyzerService.getLatestAnalysis("test-service"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/latest-analysis/test-service"))
                .andExpect(status().isNotFound());

        verify(analyzerService, times(1)).getLatestAnalysis("test-service");
    }

    @Test
    void getMetrics_shouldReturnMetrics() throws Exception {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("serviceName", "test-service");
        metrics.put("snapshotCount", 10);

        when(metricsCollector.getRecentMetrics(anyString(), anyInt())).thenReturn(metrics);

        mockMvc.perform(get("/api/metrics/test-service?limit=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.snapshotCount").value(10));

        verify(metricsCollector, times(1)).getRecentMetrics("test-service", 50);
    }

    @Test
    void collectMetrics_shouldTriggerCollection() throws Exception {
        doNothing().when(metricsCollector).collectMetrics();

        mockMvc.perform(post("/api/collect-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Metrics collection completed"));

        verify(metricsCollector, times(1)).collectMetrics();
    }

    @Test
    void getDashboard_shouldReturnDashboardData() throws Exception {
        when(analyzerService.getLatestAnalysis(anyString()))
                .thenReturn(Optional.of(mockAnalysisResult));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicesAnalyzed").value(3))
                .andExpect(jsonPath("$.totalMonthlySavings").exists())
                .andExpect(jsonPath("$.totalAnnualSavings").exists());

        verify(analyzerService, times(3)).getLatestAnalysis(anyString());
    }

    // ========== Anomaly Detection Endpoint Tests ==========

    @Test
    void getActiveAnomalies_shouldReturnActiveAnomaliesList() throws Exception {
        List<Anomaly> activeAnomalies = Arrays.asList(mockAnomaly);
        when(anomalyRepository.findByResolvedFalse()).thenReturn(activeAnomalies);

        mockMvc.perform(get("/api/anomalies/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].serviceName").value("test-service"))
                .andExpect(jsonPath("$[0].metricType").value("CPU_USAGE"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].resolved").value(false));

        verify(anomalyRepository, times(1)).findByResolvedFalse();
    }

    @Test
    void getServiceAnomalies_shouldReturnAnomaliesForService() throws Exception {
        List<Anomaly> serviceAnomalies = Arrays.asList(mockAnomaly);
        when(anomalyRepository.findByServiceNameOrderByDetectedAtDesc(
                eq("test-service"), any(PageRequest.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(serviceAnomalies));

        mockMvc.perform(get("/api/anomalies/test-service?limit=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("test-service"))
                .andExpect(jsonPath("$[0].anomalyType").value("SPIKE"));

        verify(anomalyRepository, times(1)).findByServiceNameOrderByDetectedAtDesc(
                eq("test-service"), any(PageRequest.class));
    }

    @Test
    void getAnomalyStats_shouldReturnStatistics() throws Exception {
        when(anomalyRepository.count()).thenReturn(10L);
        when(anomalyRepository.countActiveAnomalies()).thenReturn(5L);
        when(anomalyRepository.countByServiceNameAndResolvedFalse(anyString())).thenReturn(2L);
        when(anomalyRepository.findByDetectedAtAfter(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockAnomaly));

        mockMvc.perform(get("/api/anomalies/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnomalies").value(10))
                .andExpect(jsonPath("$.activeAnomalies").value(5))
                .andExpect(jsonPath("$.recentAnomalies").value(1))
                .andExpect(jsonPath("$.bySeverity").exists());

        verify(anomalyRepository, times(1)).count();
        verify(anomalyRepository, times(1)).countActiveAnomalies();
    }

    @Test
    void resolveAnomaly_shouldMarkAnomalyAsResolved() throws Exception {
        when(anomalyRepository.findById(1L)).thenReturn(Optional.of(mockAnomaly));
        when(anomalyRepository.save(any(Anomaly.class))).thenReturn(mockAnomaly);

        mockMvc.perform(post("/api/anomalies/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Anomaly resolved successfully"));

        verify(anomalyRepository, times(1)).findById(1L);
        verify(anomalyRepository, times(1)).save(any(Anomaly.class));
    }

    @Test
    void resolveAnomaly_shouldReturn404WhenAnomalyNotFound() throws Exception {
        when(anomalyRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/anomalies/999/resolve"))
                .andExpect(status().isNotFound());

        verify(anomalyRepository, times(1)).findById(999L);
        verify(anomalyRepository, never()).save(any(Anomaly.class));
    }

    @Test
    void getAnomalyTimeline_shouldReturnAnomaliesInTimeRange() throws Exception {
        List<Anomaly> timelineAnomalies = Arrays.asList(mockAnomaly);
        when(anomalyRepository.findByServiceNameAndDateRange(
                eq("test-service"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(timelineAnomalies);

        mockMvc.perform(get("/api/anomalies/timeline/test-service?hoursAgo=24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("test-service"))
                .andExpect(jsonPath("$[0].detectedAt").exists());

        verify(anomalyRepository, times(1)).findByServiceNameAndDateRange(
                eq("test-service"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getAnomalyTimeline_shouldUseDefaultTimeRangeWhenNotProvided() throws Exception {
        when(anomalyRepository.findByServiceNameAndDateRange(
                eq("test-service"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(mockAnomaly));

        mockMvc.perform(get("/api/anomalies/timeline/test-service"))
                .andExpect(status().isOk());

        verify(anomalyRepository, times(1)).findByServiceNameAndDateRange(
                eq("test-service"), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
