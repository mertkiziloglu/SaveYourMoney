package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.collector.MetricsCollectorService;
import com.hackathon.analyzer.ml.CostPredictionService;
import com.hackathon.analyzer.ml.WorkloadClassificationService;
import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.repository.AnomalyRepository;
import com.hackathon.analyzer.service.ResourceAnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Analyzer", description = "AI-powered resource analysis and optimization recommendations")
public class AnalyzerController {

    private final ResourceAnalyzerService analyzerService;
    private final MetricsCollectorService metricsCollector;
    private final AnomalyRepository anomalyRepository;
    private final CostPredictionService costPredictionService;
    private final WorkloadClassificationService workloadClassificationService;

    @Operation(summary = "Health Check", description = "Check if the analyzer service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "analyzer-service");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Analyze Service", description = "Analyze a specific microservice and generate optimization recommendations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PostMapping("/analyze/{serviceName}")
    public ResponseEntity<ResourceRecommendation> analyzeService(
            @Parameter(description = "Service name (e.g., cpu-hungry-service)")
            @PathVariable String serviceName) {
        log.info("Received analysis request for service: {}", serviceName);

        ResourceRecommendation recommendation = analyzerService.analyzeService(serviceName);

        log.info("Analysis completed for {} with confidence: {}%",
                serviceName, recommendation.getConfidenceScore());

        return ResponseEntity.ok(recommendation);
    }

    /**
     * Analyze all services
     */
    @PostMapping("/analyze-all")
    public ResponseEntity<Map<String, ResourceRecommendation>> analyzeAllServices() {
        log.info("Received request to analyze all services");

        Map<String, ResourceRecommendation> recommendations = new HashMap<>();

        recommendations.put("cpu-hungry-service",
                analyzerService.analyzeService("cpu-hungry-service"));
        recommendations.put("memory-leaker-service",
                analyzerService.analyzeService("memory-leaker-service"));
        recommendations.put("db-connection-service",
                analyzerService.analyzeService("db-connection-service"));

        log.info("All services analyzed successfully");

        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get analysis history for a service
     */
    @GetMapping("/analysis-history/{serviceName}")
    public ResponseEntity<List<AnalysisResult>> getAnalysisHistory(@PathVariable String serviceName) {
        List<AnalysisResult> history = analyzerService.getAnalysisHistory(serviceName);

        return ResponseEntity.ok(history);
    }

    /**
     * Get latest analysis for a service
     */
    @GetMapping("/latest-analysis/{serviceName}")
    public ResponseEntity<AnalysisResult> getLatestAnalysis(@PathVariable String serviceName) {
        Optional<AnalysisResult> analysis = analyzerService.getLatestAnalysis(serviceName);

        return analysis.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get recent metrics for a service
     */
    @GetMapping("/metrics/{serviceName}")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "100") int limit) {

        Map<String, Object> metrics = metricsCollector.getRecentMetrics(serviceName, limit);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Trigger manual metrics collection
     */
    @PostMapping("/collect-metrics")
    public ResponseEntity<Map<String, String>> collectMetrics() {
        log.info("Manual metrics collection triggered");

        metricsCollector.collectMetrics();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Metrics collection completed");

        return ResponseEntity.ok(response);
    }

    /**
     * Get overall dashboard summary
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // Get latest analysis for all services
        Optional<AnalysisResult> cpuAnalysis = analyzerService.getLatestAnalysis("cpu-hungry-service");
        Optional<AnalysisResult> memoryAnalysis = analyzerService.getLatestAnalysis("memory-leaker-service");
        Optional<AnalysisResult> dbAnalysis = analyzerService.getLatestAnalysis("db-connection-service");

        // Calculate total savings
        double totalSavings = 0.0;
        int servicesAnalyzed = 0;

        if (cpuAnalysis.isPresent()) {
            totalSavings += cpuAnalysis.get().getEstimatedMonthlySavings();
            servicesAnalyzed++;
        }
        if (memoryAnalysis.isPresent()) {
            totalSavings += memoryAnalysis.get().getEstimatedMonthlySavings();
            servicesAnalyzed++;
        }
        if (dbAnalysis.isPresent()) {
            totalSavings += dbAnalysis.get().getEstimatedMonthlySavings();
            servicesAnalyzed++;
        }

        dashboard.put("servicesAnalyzed", servicesAnalyzed);
        dashboard.put("totalMonthlySavings", totalSavings);
        dashboard.put("totalAnnualSavings", totalSavings * 12);
        dashboard.put("cpuHungryService", cpuAnalysis.orElse(null));
        dashboard.put("memoryLeakerService", memoryAnalysis.orElse(null));
        dashboard.put("dbConnectionService", dbAnalysis.orElse(null));

        return ResponseEntity.ok(dashboard);
    }

    // ========== Anomaly Detection Endpoints ==========

    @Operation(summary = "Get Active Anomalies", description = "Get all active (unresolved) anomalies across all services")
    @ApiResponse(responseCode = "200", description = "Active anomalies retrieved successfully")
    @GetMapping("/anomalies/active")
    public ResponseEntity<List<Anomaly>> getActiveAnomalies() {
        List<Anomaly> activeAnomalies = anomalyRepository.findByResolvedFalse();
        log.info("Retrieved {} active anomalies", activeAnomalies.size());
        return ResponseEntity.ok(activeAnomalies);
    }

    @Operation(summary = "Get Service Anomalies", description = "Get recent anomalies for a specific service")
    @ApiResponse(responseCode = "200", description = "Service anomalies retrieved successfully")
    @GetMapping("/anomalies/{serviceName}")
    public ResponseEntity<List<Anomaly>> getServiceAnomalies(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @RequestParam(defaultValue = "50") int limit) {

        List<Anomaly> anomalies = anomalyRepository.findByServiceNameOrderByDetectedAtDesc(
                serviceName, PageRequest.of(0, limit));

        log.info("Retrieved {} anomalies for service: {}", anomalies.size(), serviceName);
        return ResponseEntity.ok(anomalies);
    }

    @Operation(summary = "Get Anomaly Statistics", description = "Get aggregated anomaly statistics")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @GetMapping("/anomalies/stats")
    public ResponseEntity<Map<String, Object>> getAnomalyStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total counts
        long totalAnomalies = anomalyRepository.count();
        long activeAnomalies = anomalyRepository.countActiveAnomalies();

        // Per-service counts
        long cpuHungryActive = anomalyRepository.countByServiceNameAndResolvedFalse("cpu-hungry-service");
        long memoryLeakerActive = anomalyRepository.countByServiceNameAndResolvedFalse("memory-leaker-service");
        long dbConnectionActive = anomalyRepository.countByServiceNameAndResolvedFalse("db-connection-service");

        // Recent anomalies (last hour)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Anomaly> recentAnomalies = anomalyRepository.findByDetectedAtAfter(oneHourAgo);

        // Count by severity
        Map<String, Long> bySeverity = new HashMap<>();
        for (AnomalySeverity severity : AnomalySeverity.values()) {
            long count = recentAnomalies.stream()
                    .filter(a -> a.getSeverity() == severity && !a.getResolved())
                    .count();
            bySeverity.put(severity.name(), count);
        }

        stats.put("totalAnomalies", totalAnomalies);
        stats.put("activeAnomalies", activeAnomalies);
        stats.put("recentAnomalies", recentAnomalies.size());
        stats.put("cpuHungryServiceActive", cpuHungryActive);
        stats.put("memoryLeakerServiceActive", memoryLeakerActive);
        stats.put("dbConnectionServiceActive", dbConnectionActive);
        stats.put("bySeverity", bySeverity);

        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Resolve Anomaly", description = "Mark an anomaly as resolved")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anomaly resolved successfully"),
        @ApiResponse(responseCode = "404", description = "Anomaly not found")
    })
    @PostMapping("/anomalies/{id}/resolve")
    public ResponseEntity<Map<String, String>> resolveAnomaly(
            @Parameter(description = "Anomaly ID") @PathVariable Long id) {

        Optional<Anomaly> anomalyOpt = anomalyRepository.findById(id);

        if (anomalyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Anomaly anomaly = anomalyOpt.get();
        anomaly.setResolved(true);
        anomaly.setResolvedAt(LocalDateTime.now());
        anomalyRepository.save(anomaly);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Anomaly resolved successfully");

        log.info("Resolved anomaly ID: {} for service: {}", id, anomaly.getServiceName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Anomaly Timeline", description = "Get anomalies for a service within a time range")
    @ApiResponse(responseCode = "200", description = "Timeline retrieved successfully")
    @GetMapping("/anomalies/timeline/{serviceName}")
    public ResponseEntity<List<Anomaly>> getAnomalyTimeline(
            @Parameter(description = "Service name") @PathVariable String serviceName,
            @RequestParam(required = false) Integer hoursAgo) {

        int hours = hoursAgo != null ? hoursAgo : 24; // Default to last 24 hours
        LocalDateTime startDate = LocalDateTime.now().minusHours(hours);
        LocalDateTime endDate = LocalDateTime.now();

        List<Anomaly> anomalies = anomalyRepository.findByServiceNameAndDateRange(
                serviceName, startDate, endDate);

        log.info("Retrieved {} anomalies for {} in the last {} hours",
                anomalies.size(), serviceName, hours);

        return ResponseEntity.ok(anomalies);
    }

    // ========== AI/ML Prediction Endpoints ==========

    @Operation(
        summary = "Predict Future Costs",
        description = "AI-powered cost forecasting using Holt-Winters time-series analysis. " +
                     "Predicts future costs based on historical metrics and trends."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cost forecast generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/predict/costs/{serviceName}")
    public ResponseEntity<CostForecast> predictCosts(
            @Parameter(description = "Service name to predict costs for")
            @PathVariable String serviceName,
            @Parameter(description = "Number of days to forecast (default: 30)")
            @RequestParam(defaultValue = "30") int daysAhead) {

        if (daysAhead < 1 || daysAhead > 90) {
            log.warn("Invalid daysAhead parameter: {}. Must be between 1-90", daysAhead);
            daysAhead = Math.max(1, Math.min(90, daysAhead));
        }

        log.info("Cost prediction requested for {} - {} days ahead", serviceName, daysAhead);

        CostForecast forecast = costPredictionService.predictCosts(serviceName, daysAhead);

        log.info("Cost prediction completed for {}: ${:.2f}/month → ${:.2f}/month ({} trend)",
                serviceName,
                forecast.getCurrentMonthlyCost(),
                forecast.getPredictedMonthlyCost(),
                forecast.getTrend());

        return ResponseEntity.ok(forecast);
    }

    @Operation(
        summary = "Classify Workload Pattern",
        description = "ML-based workload pattern classification using feature engineering and clustering. " +
                     "Analyzes 7 days of metrics to identify patterns (STEADY, BURSTY, PERIODIC, etc.) " +
                     "and recommends optimal resource strategies."
    )
    @ApiResponse(responseCode = "200", description = "Workload classification completed successfully")
    @GetMapping("/classify/workload/{serviceName}")
    public ResponseEntity<WorkloadProfile> classifyWorkload(
            @Parameter(description = "Service name to classify")
            @PathVariable String serviceName) {

        log.info("Workload classification requested for {}", serviceName);

        WorkloadProfile profile = workloadClassificationService.classifyWorkload(serviceName);

        log.info("Workload classified for {}: {} pattern with {:.1f}% confidence - Strategy: {}",
                serviceName,
                profile.getPattern(),
                profile.getConfidenceScore(),
                profile.getRecommendedStrategy());

        return ResponseEntity.ok(profile);
    }

    @Operation(
        summary = "Get AI Insights Dashboard",
        description = "Comprehensive AI-powered insights combining cost predictions, " +
                     "workload classification, anomaly detection, and resource recommendations"
    )
    @ApiResponse(responseCode = "200", description = "AI insights retrieved successfully")
    @GetMapping("/ai/insights/{serviceName}")
    public ResponseEntity<Map<String, Object>> getAIInsights(
            @Parameter(description = "Service name")
            @PathVariable String serviceName) {

        log.info("AI insights requested for {}", serviceName);

        Map<String, Object> insights = new HashMap<>();

        // 1. Cost Prediction
        try {
            CostForecast costForecast = costPredictionService.predictCosts(serviceName, 30);
            insights.put("costPrediction", costForecast);
        } catch (Exception e) {
            log.error("Error predicting costs: {}", e.getMessage());
            insights.put("costPrediction", null);
        }

        // 2. Workload Classification
        try {
            WorkloadProfile workloadProfile = workloadClassificationService.classifyWorkload(serviceName);
            insights.put("workloadProfile", workloadProfile);
        } catch (Exception e) {
            log.error("Error classifying workload: {}", e.getMessage());
            insights.put("workloadProfile", null);
        }

        // 3. Active Anomalies
        List<Anomaly> activeAnomalies = anomalyRepository.findByServiceNameAndResolvedFalse(serviceName);
        insights.put("activeAnomalies", activeAnomalies);
        insights.put("anomalyCount", activeAnomalies.size());

        // 4. Latest Resource Recommendation
        Optional<AnalysisResult> latestAnalysis = analyzerService.getLatestAnalysis(serviceName);
        latestAnalysis.ifPresent(analysis -> {
            insights.put("resourceRecommendation", analysis.getRecommendation());
            insights.put("estimatedSavings", analysis.getEstimatedMonthlySavings());
        });

        // 5. AI Summary
        Map<String, String> aiSummary = new HashMap<>();
        aiSummary.put("serviceName", serviceName);
        aiSummary.put("generatedAt", LocalDateTime.now().toString());
        aiSummary.put("aiModelsUsed", "Holt-Winters Forecasting, K-Means Classification, Statistical Anomaly Detection");

        insights.put("summary", aiSummary);

        log.info("AI insights generated for {}: {} active anomalies, cost trend available",
                serviceName, activeAnomalies.size());

        return ResponseEntity.ok(insights);
    }

    @Operation(
        summary = "Get All Services AI Overview",
        description = "AI-powered overview of all monitored services with predictions and classifications"
    )
    @ApiResponse(responseCode = "200", description = "Overview retrieved successfully")
    @GetMapping("/ai/overview")
    public ResponseEntity<Map<String, Object>> getAIOverview() {
        log.info("AI overview requested for all services");

        Map<String, Object> overview = new HashMap<>();
        List<String> services = Arrays.asList("cpu-hungry-service", "memory-leaker-service", "db-connection-service");

        List<Map<String, Object>> serviceInsights = new ArrayList<>();
        double totalPredictedCosts = 0.0;
        double totalCurrentCosts = 0.0;

        for (String serviceName : services) {
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("serviceName", serviceName);

            try {
                // Cost prediction
                CostForecast forecast = costPredictionService.predictCosts(serviceName, 30);
                serviceData.put("currentMonthlyCost", forecast.getCurrentMonthlyCost());
                serviceData.put("predictedMonthlyCost", forecast.getPredictedMonthlyCost());
                serviceData.put("costTrend", forecast.getTrend());

                totalCurrentCosts += forecast.getCurrentMonthlyCost();
                totalPredictedCosts += forecast.getPredictedMonthlyCost();

                // Workload classification
                WorkloadProfile profile = workloadClassificationService.classifyWorkload(serviceName);
                serviceData.put("workloadPattern", profile.getPattern());
                serviceData.put("recommendedStrategy", profile.getRecommendedStrategy());
                serviceData.put("estimatedSavings", profile.getEstimatedSavings());

                // Anomaly count
                long anomalyCount = anomalyRepository.countByServiceNameAndResolvedFalse(serviceName);
                serviceData.put("activeAnomalies", anomalyCount);

            } catch (Exception e) {
                log.error("Error generating insights for {}: {}", serviceName, e.getMessage());
                serviceData.put("error", "Unable to generate insights");
            }

            serviceInsights.add(serviceData);
        }

        overview.put("services", serviceInsights);
        overview.put("totalCurrentMonthlyCost", totalCurrentCosts);
        overview.put("totalPredictedMonthlyCost", totalPredictedCosts);
        overview.put("costChangePercentage",
            totalCurrentCosts > 0 ? ((totalPredictedCosts - totalCurrentCosts) / totalCurrentCosts * 100) : 0.0);
        overview.put("totalActiveAnomalies", anomalyRepository.countActiveAnomalies());
        overview.put("generatedAt", LocalDateTime.now());

        log.info("AI overview generated: ${:.2f} current → ${:.2f} predicted",
                totalCurrentCosts, totalPredictedCosts);

        return ResponseEntity.ok(overview);
    }
}
