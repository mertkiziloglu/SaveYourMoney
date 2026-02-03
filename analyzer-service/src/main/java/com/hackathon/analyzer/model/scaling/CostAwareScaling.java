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
public class CostAwareScaling {
    private String serviceName;

    // Current state
    private Integer currentReplicas;
    private Double currentMonthlyCost;
    private Double currentPerformanceScore;

    // Scaling options
    private ScalingOption performanceOptimized;
    private ScalingOption costOptimized;
    private ScalingOption balanced;

    // Recommendations
    private String recommendedOption; // "PERFORMANCE", "COST", "BALANCED"
    private String rationale;

    // Idle time analysis
    private IdleTimeAnalysis idleTimeAnalysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScalingOption {
        private String strategy;
        private Integer minReplicas;
        private Integer maxReplicas;
        private Integer averageReplicas;

        // Resource sizing
        private String cpuRequest;
        private String memoryRequest;

        // Cost analysis
        private Double monthlyCost;
        private Double savingsPercentage;
        private Double savingsAmount;

        // Performance analysis
        private Double expectedP95ResponseTime;
        private Double expectedP99ResponseTime;
        private Double performanceScore; // 0-100

        // Trade-offs
        private List<String> pros;
        private List<String> cons;

        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdleTimeAnalysis {
        private Double idlePercentage;
        private List<IdlePeriod> idlePeriods;
        private Double potentialSavings;
        private String recommendation;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class IdlePeriod {
            private Integer hourOfDay;
            private DayOfWeek dayOfWeek;
            private Double averageUsage;
            private Integer recommendedReplicas;
        }

        public enum DayOfWeek {
            MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
        }
    }
}
