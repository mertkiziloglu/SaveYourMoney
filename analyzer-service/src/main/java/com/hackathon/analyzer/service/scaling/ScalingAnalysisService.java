package com.hackathon.analyzer.service.scaling;

import com.hackathon.analyzer.model.scaling.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScalingAnalysisService {

    private final HPARecommendationService hpaService;
    private final VPARecommendationService vpaService;
    private final PredictiveScalingService predictiveService;
    private final CostAwareScalingService costAwareService;
    private final CustomMetricsAnalysisService customMetricsService;

    /**
     * Generate comprehensive scaling analysis
     */
    public ScalingAnalysis analyzeScaling(String serviceName) {
        log.info("Starting comprehensive scaling analysis for: {}", serviceName);

        // Generate all recommendations
        HPARecommendation hpaRecommendation = hpaService.generateHPARecommendation(serviceName);
        VPARecommendation vpaRecommendation = vpaService.generateVPARecommendation(serviceName);
        CostAwareScaling costAwareScaling = costAwareService.analyzeCostAwareScaling(serviceName);
        CustomMetricsAnalysis customMetrics = customMetricsService.analyzeCustomMetrics(serviceName);

        // Generate predictions
        List<ScalingPrediction> predictions = predictiveService.predictNext24Hours(serviceName);

        // Get detected pattern
        TimeSeriesPattern pattern = null;
        if (!predictions.isEmpty()) {
            pattern = predictions.get(0).getDetectedPattern();
        }

        // Generate summary
        ScalingAnalysis.ScalingSummary summary = generateSummary(
                hpaRecommendation, vpaRecommendation, costAwareScaling, predictions);

        return ScalingAnalysis.builder()
                .serviceName(serviceName)
                .analysisTimestamp(Instant.now())
                .hpaRecommendation(hpaRecommendation)
                .vpaRecommendation(vpaRecommendation)
                .costAwareScaling(costAwareScaling)
                .next24HoursPredictions(predictions)
                .detectedPattern(pattern)
                .customMetricsAnalysis(customMetrics)
                .summary(summary)
                .build();
    }

    private ScalingAnalysis.ScalingSummary generateSummary(
            HPARecommendation hpa, VPARecommendation vpa,
            CostAwareScaling cost, List<ScalingPrediction> predictions) {

        // Determine primary recommendation
        String primaryRecommendation = determinePrimaryRecommendation(hpa, vpa, cost);

        // Determine urgency
        String urgency = determineUrgency(hpa, predictions);

        // Generate key findings
        List<String> keyFindings = generateKeyFindings(hpa, vpa, cost, predictions);

        // Generate action items
        List<String> actionItems = generateActionItems(primaryRecommendation, hpa, vpa, cost);

        // Calculate expected savings
        double expectedSavings = calculateExpectedSavings(cost, vpa);

        // Calculate overall confidence
        double confidence = (hpa.getConfidenceScore() + vpa.getConfidenceScore()) / 2.0;

        return ScalingAnalysis.ScalingSummary.builder()
                .primaryRecommendation(primaryRecommendation)
                .urgency(urgency)
                .keyFindings(keyFindings)
                .actionItems(actionItems)
                .expectedMonthlySavings(expectedSavings)
                .confidenceScore(confidence)
                .build();
    }

    private String determinePrimaryRecommendation(HPARecommendation hpa,
                                                  VPARecommendation vpa,
                                                  CostAwareScaling cost) {
        // Check VPA recommendation first
        if (vpa.getRecommendation() != null) {
            if (vpa.getRecommendation().contains("both")) {
                return "USE_BOTH";
            } else if (vpa.getRecommendation().contains("HPA")) {
                return "USE_HPA";
            } else if (vpa.getRecommendation().contains("VPA")) {
                return "USE_VPA";
            }
        }

        // Default to HPA for most services
        return "USE_HPA";
    }

    private String determineUrgency(HPARecommendation hpa, List<ScalingPrediction> predictions) {
        // Check if current configuration is problematic
        if (hpa.getCurrentReplicas() < hpa.getMinReplicas()) {
            return "CRITICAL";
        }

        // Check predictions for upcoming issues
        long criticalEvents = predictions.stream()
                .flatMap(p -> p.getUpcomingEvents().stream())
                .filter(e -> e.getEventType().equals("PEAK_LOAD"))
                .count();

        if (criticalEvents > 5) {
            return "HIGH";
        } else if (criticalEvents > 2) {
            return "MEDIUM";
        }

        // Check cost savings opportunity
        if (hpa.getEstimatedCostImpact() < -50) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private List<String> generateKeyFindings(HPARecommendation hpa, VPARecommendation vpa,
                                            CostAwareScaling cost, List<ScalingPrediction> predictions) {
        List<String> findings = new ArrayList<>();

        // HPA findings
        if (hpa.getCurrentReplicas() != hpa.getRecommendedReplicas()) {
            findings.add(String.format("Current replica count (%d) differs from recommended (%d)",
                    hpa.getCurrentReplicas(), hpa.getRecommendedReplicas()));
        }

        // VPA findings
        if (vpa.getEstimatedMonthlySavings() != null && Math.abs(vpa.getEstimatedMonthlySavings()) > 20) {
            findings.add(String.format("Resource right-sizing can save $%.2f/month",
                    vpa.getEstimatedMonthlySavings()));
        }

        // Cost findings
        if (cost.getCostOptimized() != null && cost.getCostOptimized().getSavingsPercentage() > 30) {
            findings.add(String.format("Cost-optimized strategy can reduce costs by %.1f%%",
                    cost.getCostOptimized().getSavingsPercentage()));
        }

        // Pattern findings
        if (!predictions.isEmpty() && predictions.get(0).getDetectedPattern() != null) {
            TimeSeriesPattern pattern = predictions.get(0).getDetectedPattern();
            if (pattern.getHasDailyPattern()) {
                findings.add("Daily usage pattern detected - consider scheduled scaling");
            }
            if (pattern.getTrend().contains("INCREASING")) {
                findings.add("Increasing trend detected - proactive scaling recommended");
            }
        }

        return findings;
    }

    private List<String> generateActionItems(String primaryRecommendation,
                                            HPARecommendation hpa, VPARecommendation vpa,
                                            CostAwareScaling cost) {
        List<String> actions = new ArrayList<>();

        switch (primaryRecommendation) {
            case "USE_HPA":
                actions.add("Deploy HPA with recommended configuration");
                actions.add(String.format("Set min replicas: %d, max replicas: %d, target CPU: %d%%",
                        hpa.getMinReplicas(), hpa.getMaxReplicas(), hpa.getTargetCPUUtilizationPercentage()));
                break;

            case "USE_VPA":
                actions.add("Deploy VPA with recommended configuration");
                actions.add(String.format("Set resources - CPU: %s, Memory: %s",
                        vpa.getRecommendedRequests().getCpu(), vpa.getRecommendedRequests().getMemory()));
                break;

            case "USE_BOTH":
                actions.add("Deploy both HPA and VPA for optimal scaling");
                actions.add("HPA will handle horizontal scaling, VPA will right-size resources");
                break;

            case "MANUAL_SCALING":
                actions.add("Current configuration is optimal");
                actions.add("Monitor metrics and reassess periodically");
                break;
        }

        // Add cost optimization actions
        if (cost.getRecommendedOption() != null) {
            actions.add(String.format("Consider %s scaling strategy for cost optimization",
                    cost.getRecommendedOption().toLowerCase()));
        }

        return actions;
    }

    private double calculateExpectedSavings(CostAwareScaling cost, VPARecommendation vpa) {
        double savings = 0.0;

        // Add cost-aware savings
        if (cost.getBalanced() != null) {
            savings += cost.getBalanced().getSavingsAmount();
        }

        // Add VPA savings
        if (vpa.getEstimatedMonthlySavings() != null && vpa.getEstimatedMonthlySavings() > 0) {
            savings += vpa.getEstimatedMonthlySavings();
        }

        return Math.round(savings * 100.0) / 100.0;
    }
}
