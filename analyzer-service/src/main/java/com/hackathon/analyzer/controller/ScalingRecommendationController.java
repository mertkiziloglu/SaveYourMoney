package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.model.scaling.*;
import com.hackathon.analyzer.service.scaling.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/scaling")
@RequiredArgsConstructor
@Tag(name = "Scaling", description = "Intelligent pod scaling recommendations with HPA, VPA, and predictive analytics")
public class ScalingRecommendationController {

    private final ScalingAnalysisService scalingAnalysisService;
    private final HPARecommendationService hpaService;
    private final VPARecommendationService vpaService;
    private final PredictiveScalingService predictiveService;
    private final CostAwareScalingService costAwareService;
    private final CustomMetricsAnalysisService customMetricsService;

    @Operation(summary = "Comprehensive Scaling Analysis",
            description = "Get complete scaling analysis including HPA, VPA, predictions, and cost optimization")
    @PostMapping("/analyze/{serviceName}")
    public ResponseEntity<ScalingAnalysis> analyzeScaling(
            @Parameter(description = "Service name to analyze")
            @PathVariable String serviceName) {

        log.info("Received comprehensive scaling analysis request for: {}", serviceName);

        ScalingAnalysis analysis = scalingAnalysisService.analyzeScaling(serviceName);

        return ResponseEntity.ok(analysis);
    }

    @Operation(summary = "HPA Recommendations",
            description = "Get Horizontal Pod Autoscaler configuration recommendations")
    @GetMapping("/hpa/{serviceName}")
    public ResponseEntity<HPARecommendation> getHPARecommendation(
            @PathVariable String serviceName) {

        log.info("Generating HPA recommendation for: {}", serviceName);

        HPARecommendation recommendation = hpaService.generateHPARecommendation(serviceName);

        return ResponseEntity.ok(recommendation);
    }

    @Operation(summary = "VPA Recommendations",
            description = "Get Vertical Pod Autoscaler resource recommendations")
    @GetMapping("/vpa/{serviceName}")
    public ResponseEntity<VPARecommendation> getVPARecommendation(
            @PathVariable String serviceName) {

        log.info("Generating VPA recommendation for: {}", serviceName);

        VPARecommendation recommendation = vpaService.generateVPARecommendation(serviceName);

        return ResponseEntity.ok(recommendation);
    }

    @Operation(summary = "Predictive Scaling",
            description = "Get 24-hour scaling predictions based on historical patterns")
    @GetMapping("/predict/{serviceName}")
    public ResponseEntity<Map<String, Object>> getPredictions(
            @Parameter(description = "Service name")
            @PathVariable String serviceName,
            @Parameter(description = "Hours ahead to predict (default: 24)")
            @RequestParam(defaultValue = "24") int hoursAhead) {

        log.info("Generating {}-hour predictions for: {}", hoursAhead, serviceName);

        List<ScalingPrediction> predictions = predictiveService.predictNext24Hours(serviceName);

        // Limit to requested hours
        if (predictions.size() > hoursAhead) {
            predictions = predictions.subList(0, hoursAhead);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("serviceName", serviceName);
        response.put("predictions", predictions);
        response.put("totalPredictions", predictions.size());

        if (!predictions.isEmpty() && predictions.get(0).getDetectedPattern() != null) {
            response.put("detectedPattern", predictions.get(0).getDetectedPattern());
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cost-Aware Scaling Options",
            description = "Compare performance-optimized, cost-optimized, and balanced scaling strategies")
    @GetMapping("/cost-comparison/{serviceName}")
    public ResponseEntity<CostAwareScaling> getCostComparison(
            @PathVariable String serviceName) {

        log.info("Generating cost-aware scaling analysis for: {}", serviceName);

        CostAwareScaling analysis = costAwareService.analyzeCostAwareScaling(serviceName);

        return ResponseEntity.ok(analysis);
    }

    @Operation(summary = "Custom Metrics Analysis",
            description = "Analyze custom metrics like RPS, connection pool, response time for scaling decisions")
    @GetMapping("/custom-metrics/{serviceName}")
    public ResponseEntity<CustomMetricsAnalysis> getCustomMetricsAnalysis(
            @PathVariable String serviceName) {

        log.info("Analyzing custom metrics for: {}", serviceName);

        CustomMetricsAnalysis analysis = customMetricsService.analyzeCustomMetrics(serviceName);

        return ResponseEntity.ok(analysis);
    }

    @Operation(summary = "Scaling Summary",
            description = "Get quick summary of scaling recommendations and urgency")
    @GetMapping("/summary/{serviceName}")
    public ResponseEntity<Map<String, Object>> getScalingSummary(
            @PathVariable String serviceName) {

        log.info("Generating scaling summary for: {}", serviceName);

        ScalingAnalysis analysis = scalingAnalysisService.analyzeScaling(serviceName);

        Map<String, Object> summary = new HashMap<>();
        summary.put("serviceName", serviceName);
        summary.put("summary", analysis.getSummary());
        summary.put("hpaRecommended", analysis.getHpaRecommendation().getRecommendedReplicas());
        summary.put("expectedSavings", analysis.getSummary().getExpectedMonthlySavings());
        summary.put("urgency", analysis.getSummary().getUrgency());

        return ResponseEntity.ok(summary);
    }

    @Operation(summary = "Analyze All Services",
            description = "Get scaling analysis for all monitored services")
    @PostMapping("/analyze-all")
    public ResponseEntity<Map<String, ScalingAnalysis>> analyzeAllServices() {
        log.info("Analyzing scaling for all services");

        Map<String, ScalingAnalysis> analyses = new HashMap<>();

        // Analyze all demo services
        analyses.put("cpu-hungry-service",
                scalingAnalysisService.analyzeScaling("cpu-hungry-service"));
        analyses.put("memory-leaker-service",
                scalingAnalysisService.analyzeScaling("memory-leaker-service"));
        analyses.put("db-connection-service",
                scalingAnalysisService.analyzeScaling("db-connection-service"));

        return ResponseEntity.ok(analyses);
    }

    @Operation(summary = "Detect Traffic Patterns",
            description = "Detect daily/weekly traffic patterns for a service")
    @GetMapping("/patterns/{serviceName}")
    public ResponseEntity<Map<String, Object>> detectPatterns(
            @PathVariable String serviceName) {

        log.info("Detecting traffic patterns for: {}", serviceName);

        List<ScalingPrediction> predictions = predictiveService.predictNext24Hours(serviceName);

        Map<String, Object> response = new HashMap<>();
        response.put("serviceName", serviceName);

        if (!predictions.isEmpty() && predictions.get(0).getDetectedPattern() != null) {
            TimeSeriesPattern pattern = predictions.get(0).getDetectedPattern();
            response.put("pattern", pattern);
            response.put("hasDailyPattern", pattern.getHasDailyPattern());
            response.put("hasWeeklyPattern", pattern.getHasWeeklyPattern());
            response.put("peakHours", pattern.getPeakHours());
            response.put("trend", pattern.getTrend());
            response.put("description", pattern.getDescription());
        } else {
            response.put("message", "Insufficient data to detect patterns");
        }

        return ResponseEntity.ok(response);
    }
}
