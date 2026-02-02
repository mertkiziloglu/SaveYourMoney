package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.collector.MetricsCollectorService;
import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.ResourceRecommendation;
import com.hackathon.analyzer.service.ResourceAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyzerController {

    private final ResourceAnalyzerService analyzerService;
    private final MetricsCollectorService metricsCollector;

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
    @PostMapping("/analyze/{serviceName}")
    public ResponseEntity<ResourceRecommendation> analyzeService(@PathVariable String serviceName) {
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
}
