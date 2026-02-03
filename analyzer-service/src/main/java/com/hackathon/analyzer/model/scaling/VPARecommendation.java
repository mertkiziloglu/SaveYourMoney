package com.hackathon.analyzer.model.scaling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VPARecommendation {
    private String serviceName;

    // Current resources
    private ResourceRequests currentRequests;
    private ResourceRequests currentLimits;

    // VPA recommendations
    private ResourceRequests recommendedRequests;
    private ResourceRequests recommendedLimits;

    // VPA mode
    private String updateMode; // "Off", "Initial", "Recreate", "Auto"

    // Resource policy
    private ResourcePolicy resourcePolicy;

    // Analysis
    private String rationale;
    private Double confidenceScore;
    private Double estimatedMonthlySavings;
    private String recommendation; // "Use VPA", "Use HPA", "Use both", "Manual tuning"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceRequests {
        private String cpu;
        private String memory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourcePolicy {
        private ResourceRange cpuRange;
        private ResourceRange memoryRange;
        private String controlledResources; // "RequestsAndLimits", "RequestsOnly"

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ResourceRange {
            private String min;
            private String max;
        }
    }
}
