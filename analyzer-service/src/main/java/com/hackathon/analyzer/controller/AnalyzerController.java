package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.collector.MetricsCollectorService;
import com.hackathon.analyzer.discovery.ServiceDiscoveryService;
import com.hackathon.analyzer.discovery.ServiceInfo;
import com.hackathon.analyzer.ml.CostPredictionService;
import com.hackathon.analyzer.ml.WorkloadClassificationService;
import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.service.GeminiInsightService;
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
    private final GeminiInsightService geminiInsightService;
    private final ServiceDiscoveryService serviceDiscoveryService;

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
     * Analyze all services - now dynamically discovers services
     */
    @PostMapping("/analyze-all")
    public ResponseEntity<Map<String, ResourceRecommendation>> analyzeAllServices() {
        log.info("Received request to analyze all services");

        Map<String, ResourceRecommendation> recommendations = new HashMap<>();

        for (ServiceInfo service : serviceDiscoveryService.getHealthyServices()) {
            try {
                recommendations.put(service.getName(),
                        analyzerService.analyzeService(service.getName()));
            } catch (Exception e) {
                log.warn("Failed to analyze service {}: {}", service.getName(), e.getMessage());
            }
        }

        log.info("Analyzed {} services successfully", recommendations.size());

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
     * Get overall dashboard summary - now dynamically includes all discovered services
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        List<ServiceInfo> services = serviceDiscoveryService.getHealthyServices();
        double totalSavings = 0.0;
        int servicesAnalyzed = 0;
        Map<String, AnalysisResult> serviceAnalyses = new HashMap<>();

        for (ServiceInfo service : services) {
            Optional<AnalysisResult> analysis = analyzerService.getLatestAnalysis(service.getName());
            if (analysis.isPresent()) {
                totalSavings += analysis.get().getEstimatedMonthlySavings();
                servicesAnalyzed++;
                serviceAnalyses.put(service.getName(), analysis.get());
            }
        }

        dashboard.put("servicesAnalyzed", servicesAnalyzed);
        dashboard.put("totalRegisteredServices", services.size());
        dashboard.put("totalMonthlySavings", totalSavings);
        dashboard.put("totalAnnualSavings", totalSavings * 12);
        dashboard.put("services", serviceAnalyses);

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
        List<ServiceInfo> services = serviceDiscoveryService.getHealthyServices();

        List<Map<String, Object>> serviceInsights = new ArrayList<>();
        double totalPredictedCosts = 0.0;
        double totalCurrentCosts = 0.0;

        for (ServiceInfo service : services) {
            String serviceName = service.getName();
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

    // ========== Gemini AI Endpoints ==========

    @Operation(summary = "Get Gemini AI Optimization Insight",
            description = "Generate AI-powered optimization recommendations using Google Gemini 2.0 Flash. " +
                    "Provides detailed analysis in Turkish with specific resource recommendations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gemini insight generated successfully"),
            @ApiResponse(responseCode = "503", description = "Gemini API not configured or unavailable")
    })
    @GetMapping("/gemini/insight/{serviceName}")
    public ResponseEntity<Map<String, Object>> getGeminiInsight(
            @Parameter(description = "Service name to analyze") @PathVariable String serviceName) {

        log.info("Gemini insight requested for {}", serviceName);

        Map<String, Object> response = new HashMap<>();
        response.put("serviceName", serviceName);
        response.put("generatedAt", LocalDateTime.now().toString());
        response.put("geminiEnabled", geminiInsightService.isConfigured());

        if (!geminiInsightService.isConfigured()) {
            response.put("insight", null);
            response.put("message", "Gemini API is not configured. Set GEMINI_API_KEY environment variable.");
            return ResponseEntity.ok(response);
        }

        try {
            // Get latest analysis data
            Optional<AnalysisResult> latestAnalysis = analyzerService.getLatestAnalysis(serviceName);
            CostForecast costForecast = costPredictionService.predictCosts(serviceName, 30);

            double cpuUsage = latestAnalysis.map(a -> a.getP95CpuUsage() != null ? a.getP95CpuUsage() : 0.0).orElse(0.0);
            double memoryUsage = latestAnalysis.map(a -> a.getP95MemoryUsage() != null ? a.getP95MemoryUsage() : 0.0).orElse(0.0);
            double monthlyCost = costForecast.getCurrentMonthlyCost();
            double savings = latestAnalysis.map(AnalysisResult::getEstimatedMonthlySavings).orElse(0.0);
            double confidence = latestAnalysis.map(AnalysisResult::getConfidenceScore).orElse(0.0);

            // Build detected issues map
            Map<String, String> detectedIssues = new HashMap<>();
            latestAnalysis.ifPresent(analysis -> {
                if (Boolean.TRUE.equals(analysis.getCpuThrottlingDetected())) {
                    detectedIssues.put("CPU Throttling", "CPU usage exceeds limits");
                }
                if (Boolean.TRUE.equals(analysis.getMemoryLeakDetected())) {
                    detectedIssues.put("Memory Leak", "Memory shows continuous growth");
                }
                if (Boolean.TRUE.equals(analysis.getConnectionPoolExhaustion())) {
                    detectedIssues.put("Connection Pool", "Pool frequently exhausted");
                }
            });

            String geminiInsight = geminiInsightService.generateOptimizationInsight(
                    serviceName, cpuUsage, memoryUsage, monthlyCost, savings, confidence, detectedIssues);

            response.put("insight", geminiInsight);
            response.put("analysisData", Map.of(
                    "cpuUsagePercent", cpuUsage,
                    "memoryUsageMb", memoryUsage,
                    "monthlyCost", monthlyCost,
                    "estimatedSavings", savings,
                    "confidenceScore", confidence,
                    "detectedIssues", detectedIssues
            ));

            log.info("Gemini insight generated successfully for {}", serviceName);

        } catch (Exception e) {
            log.error("Error generating Gemini insight for {}: {}", serviceName, e.getMessage());
            response.put("insight", null);
            response.put("error", "Failed to generate insight: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get AI Status", description = "Check if AI API is configured and available")
    @ApiResponse(responseCode = "200", description = "Status retrieved")
    @GetMapping("/gemini/status")
    public ResponseEntity<Map<String, Object>> getGeminiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", geminiInsightService.isConfigured());
        status.put("model", geminiInsightService.getModel());
        status.put("provider", "Antigravity Proxy (Anthropic SDK compatible)");
        return ResponseEntity.ok(status);
    }
}
