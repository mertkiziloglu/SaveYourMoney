package com.hackathon.analyzer.model.scaling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingAnalysis {
    private String serviceName;
    private Instant analysisTimestamp;

    // Recommendations
    private HPARecommendation hpaRecommendation;
    private VPARecommendation vpaRecommendation;
    private CostAwareScaling costAwareScaling;

    // Predictions
    private List<ScalingPrediction> next24HoursPredictions;
    private TimeSeriesPattern detectedPattern;

    // Custom metrics
    private CustomMetricsAnalysis customMetricsAnalysis;

    // Summary
    private ScalingSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScalingSummary {
        private String primaryRecommendation; // "USE_HPA", "USE_VPA", "USE_BOTH", "MANUAL_SCALING"
        private String urgency; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
        private List<String> keyFindings;
        private List<String> actionItems;
        private Double expectedMonthlySavings;
        private Double confidenceScore;
    }
}
