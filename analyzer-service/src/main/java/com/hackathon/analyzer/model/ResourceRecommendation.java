package com.hackathon.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRecommendation {

    private String serviceName;

    // Kubernetes resources
    private KubernetesResources kubernetes;

    // JVM configuration
    private JvmConfiguration jvm;

    // Connection pool (for db services)
    private ConnectionPoolConfig connectionPool;

    // Thread pool
    private ThreadPoolConfig threadPool;

    // Cost analysis
    private CostAnalysis costAnalysis;

    // Confidence and rationale
    private Double confidenceScore;
    private String rationale;
    private Map<String, String> detectedIssues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KubernetesResources {
        private String cpuRequest;
        private String cpuLimit;
        private String memoryRequest;
        private String memoryLimit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmConfiguration {
        private String xms;
        private String xmx;
        private String gcType;
        private Map<String, String> additionalFlags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionPoolConfig {
        private Integer maximumPoolSize;
        private Integer minimumIdle;
        private Long connectionTimeout;
        private Long idleTimeout;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadPoolConfig {
        private Integer maxThreads;
        private Integer minSpareThreads;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostAnalysis {
        private Double currentMonthlyCost;
        private Double recommendedMonthlyCost;
        private Double monthlySavings;
        private Double annualSavings;
        private Integer savingsPercentage;
    }
}
