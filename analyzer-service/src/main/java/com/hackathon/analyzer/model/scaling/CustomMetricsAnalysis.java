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
public class CustomMetricsAnalysis {
    private String serviceName;
    private List<CustomMetricScaling> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomMetricScaling {
        private String metricName;
        private MetricType type;

        // Current state
        private Double currentValue;
        private Double currentPerPodValue;

        // Target
        private Double targetValue;
        private Double targetPerPodValue;

        // Scaling calculation
        private Integer currentReplicas;
        private Integer recommendedReplicas;

        // Thresholds
        private Double scaleUpThreshold;
        private Double scaleDownThreshold;

        // Analysis
        private String rationale;
        private String recommendation;

        public enum MetricType {
            REQUESTS_PER_SECOND,
            QUEUE_DEPTH,
            CONNECTION_POOL_USAGE,
            ACTIVE_SESSIONS,
            RESPONSE_TIME_P95,
            ERROR_RATE,
            CUSTOM
        }
    }
}
