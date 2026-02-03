package com.hackathon.analyzer.model.scaling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HPARecommendation {
    private String serviceName;
    private Integer minReplicas;
    private Integer maxReplicas;
    private Integer currentReplicas;
    private Integer recommendedReplicas;

    // Target metrics
    private Integer targetCPUUtilizationPercentage;
    private Integer targetMemoryUtilizationPercentage;

    // Scaling policies
    private ScalingPolicy scaleUpPolicy;
    private ScalingPolicy scaleDownPolicy;

    // Custom metrics
    private List<CustomMetricTarget> customMetrics;

    // Analysis
    private String rationale;
    private Double confidenceScore;
    private Double estimatedCostImpact;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScalingPolicy {
        private Integer stabilizationWindowSeconds;
        private Integer periodSeconds;
        private Integer percentagePerScale;
        private Integer podsPerScale;
        private String behavior; // "Conservative", "Moderate", "Aggressive"
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomMetricTarget {
        private String metricName;
        private String metricType; // "Pods", "Object", "External"
        private Double targetValue;
        private String targetType; // "Value", "AverageValue"
        private String description;
    }
}
