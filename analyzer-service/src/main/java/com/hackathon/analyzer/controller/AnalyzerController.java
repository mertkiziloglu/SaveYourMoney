package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.collector.MetricsCollectorService;
import com.hackathon.analyzer.ml.CostPredictionService;
import com.hackathon.analyzer.ml.WorkloadClassificationService;
import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.service.ResourceAnalyzerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Analyzer", description = "AI-powered resource analysis and optimization recommendations")
public class AnalyzerController {

    private final ResourceAnalyzerService analyzerService;
    private final MetricsCollectorService metricsCollector;
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

    /**
     * Analyze a specific service and get recommendations
     */
    @Operation(summary = "Analyze Service", description = "Analyze a specific microservice and generate optimization recommendations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PostMapping("/analyze/{serviceName}")
    public ResponseEntity<ResourceRecommendation> analyzeService(
            @Parameter(description = "Service name (e.g., cpu-hungry-service)") @PathVariable String serviceName) {
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
        recommendations.put("greedy-service",
                analyzerService.analyzeService("greedy-service"));

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
        Optional<AnalysisResult> greedyAnalysis = analyzerService.getLatestAnalysis("greedy-service");

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
        if (greedyAnalysis.isPresent()) {
            totalSavings += greedyAnalysis.get().getEstimatedMonthlySavings();
            servicesAnalyzed++;
        }

        dashboard.put("servicesAnalyzed", servicesAnalyzed);
        dashboard.put("totalMonthlySavings", totalSavings);
        dashboard.put("totalAnnualSavings", totalSavings * 12);
        dashboard.put("cpuHungryService", cpuAnalysis.orElse(null));
        dashboard.put("memoryLeakerService", memoryAnalysis.orElse(null));
        dashboard.put("dbConnectionService", dbAnalysis.orElse(null));
        dashboard.put("greedyService", greedyAnalysis.orElse(null));

        return ResponseEntity.ok(dashboard);
    }

    // ========== AI/ML Prediction Endpoints ==========

    @Operation(summary = "Predict Future Costs", description = "AI-powered cost forecasting using Holt-Winters time-series analysis. "
            +
            "Predicts future costs based on historical metrics and trends.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cost forecast generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/predict/costs/{serviceName}")
    public ResponseEntity<CostForecast> predictCosts(
            @Parameter(description = "Service name to predict costs for") @PathVariable String serviceName,
            @Parameter(description = "Number of days to forecast (default: 30)") @RequestParam(defaultValue = "30") int daysAhead) {

        if (daysAhead < 1 || daysAhead > 90) {
            log.warn("Invalid daysAhead parameter: {}. Must be between 1-90", daysAhead);
            daysAhead = Math.max(1, Math.min(90, daysAhead));
        }

        log.info("Cost prediction requested for {} - {} days ahead", serviceName, daysAhead);

        CostForecast forecast = costPredictionService.predictCosts(serviceName, daysAhead);

        log.info("Cost prediction completed for {}: ${} current → ${} predicted ({})",
                serviceName,
                String.format("%.2f", forecast.getCurrentMonthlyCost()),
                String.format("%.2f", forecast.getPredictedMonthlyCost()),
                forecast.getTrend());

        return ResponseEntity.ok(forecast);
    }

    @Operation(summary = "Classify Workload Pattern", description = "ML-based workload pattern classification using feature engineering and clustering. "
            +
            "Analyzes 7 days of metrics to identify patterns (STEADY, BURSTY, PERIODIC, etc.) " +
            "and recommends optimal resource strategies.")
    @ApiResponse(responseCode = "200", description = "Workload classification completed successfully")
    @GetMapping("/classify/workload/{serviceName}")
    public ResponseEntity<WorkloadProfile> classifyWorkload(
            @Parameter(description = "Service name to classify") @PathVariable String serviceName) {

        log.info("Workload classification requested for {}", serviceName);

        WorkloadProfile profile = workloadClassificationService.classifyWorkload(serviceName);

        log.info("Workload classified for {}: {} pattern with {}% confidence - Strategy: {}",
                serviceName,
                profile.getPattern(),
                String.format("%.1f", profile.getConfidenceScore()),
                profile.getRecommendedStrategy());

        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get AI Insights Dashboard", description = "Comprehensive AI-powered insights combining cost predictions, "
            +
            "workload classification, anomaly detection, and resource recommendations")
    @ApiResponse(responseCode = "200", description = "AI insights retrieved successfully")
    @GetMapping("/ai/insights/{serviceName}")
    public ResponseEntity<Map<String, Object>> getAIInsights(
            @Parameter(description = "Service name") @PathVariable String serviceName) {

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

        // 3. Latest Resource Recommendation
        Optional<AnalysisResult> latestAnalysis = analyzerService.getLatestAnalysis(serviceName);
        latestAnalysis.ifPresent(analysis -> {
            Map<String, Object> recommendation = new HashMap<>();
            recommendation.put("cpuRequest", analysis.getRecommendedCpuRequest());
            recommendation.put("cpuLimit", analysis.getRecommendedCpuLimit());
            recommendation.put("memoryRequest", analysis.getRecommendedMemoryRequest());
            recommendation.put("memoryLimit", analysis.getRecommendedMemoryLimit());
            recommendation.put("jvmXms", analysis.getRecommendedJvmXms());
            recommendation.put("jvmXmx", analysis.getRecommendedJvmXmx());
            insights.put("resourceRecommendation", recommendation);
            insights.put("estimatedSavings", analysis.getEstimatedMonthlySavings());
        });

        // 4. AI Summary
        Map<String, String> aiSummary = new HashMap<>();
        aiSummary.put("serviceName", serviceName);
        aiSummary.put("generatedAt", LocalDateTime.now().toString());
        aiSummary.put("aiModelsUsed",
                "Holt-Winters Forecasting, K-Means Classification, Statistical Anomaly Detection");

        insights.put("summary", aiSummary);

        log.info("AI insights generated for {}", serviceName);

        return ResponseEntity.ok(insights);
    }

    @Operation(summary = "Get All Services AI Overview", description = "AI-powered overview of all monitored services with predictions and classifications")
    @ApiResponse(responseCode = "200", description = "Overview retrieved successfully")
    @GetMapping("/ai/overview")
    public ResponseEntity<Map<String, Object>> getAIOverview() {
        log.info("AI overview requested for all services");

        Map<String, Object> overview = new HashMap<>();
        List<String> services = Arrays.asList("cpu-hungry-service", "memory-leaker-service", "db-connection-service",
                "greedy-service");

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
        overview.put("generatedAt", LocalDateTime.now());

        log.info("AI overview generated: ${} current → ${} predicted",
                String.format("%.2f", totalCurrentCosts),
                String.format("%.2f", totalPredictedCosts));

        return ResponseEntity.ok(overview);
    }
}
