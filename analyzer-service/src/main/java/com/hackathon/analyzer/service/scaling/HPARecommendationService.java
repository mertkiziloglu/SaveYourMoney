package com.hackathon.analyzer.service.scaling;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.scaling.HPARecommendation;
import com.hackathon.analyzer.repository.AnalysisResultRepository;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HPARecommendationService {

    private final MetricsSnapshotRepository metricsRepository;
    private final AnalysisResultRepository analysisRepository;

    /**
     * Generate HPA configuration recommendations
     */
    public HPARecommendation generateHPARecommendation(String serviceName) {
        log.info("Generating HPA recommendation for: {}", serviceName);

        // Get recent metrics and analysis
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        List<MetricsSnapshot> recentMetrics = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, since);

        Optional<AnalysisResult> latestAnalysis = analysisRepository
                .findFirstByServiceNameOrderByAnalysisTimestampDesc(serviceName);

        if (recentMetrics.isEmpty()) {
            return createDefaultHPARecommendation(serviceName);
        }

        // Calculate metrics
        double avgCPU = calculateAverage(recentMetrics, MetricsSnapshot::getCpuUsagePercent);
        double p95CPU = calculatePercentile(recentMetrics, MetricsSnapshot::getCpuUsagePercent, 95);
        double maxCPU = recentMetrics.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .max()
                .orElse(50.0);

        double avgMemory = calculateAverage(recentMetrics, MetricsSnapshot::getHeapUsagePercent);
        double p95Memory = calculatePercentile(recentMetrics, MetricsSnapshot::getHeapUsagePercent, 95);

        // Determine scaling behavior based on workload pattern
        String workloadPattern = determineWorkloadPattern(avgCPU, p95CPU, maxCPU);

        // Calculate HPA parameters
        int minReplicas = calculateMinReplicas(avgCPU, workloadPattern);
        int maxReplicas = calculateMaxReplicas(p95CPU, maxCPU, workloadPattern);
        int currentReplicas = 3; // Should come from K8s API
        int recommendedReplicas = calculateRecommendedReplicas(avgCPU, p95CPU);

        int targetCPU = calculateTargetCPU(avgCPU, p95CPU, workloadPattern);
        int targetMemory = calculateTargetMemory(avgMemory, p95Memory);

        // Generate scaling policies
        HPARecommendation.ScalingPolicy scaleUpPolicy = generateScaleUpPolicy(workloadPattern);
        HPARecommendation.ScalingPolicy scaleDownPolicy = generateScaleDownPolicy(workloadPattern);

        // Custom metrics
        List<HPARecommendation.CustomMetricTarget> customMetrics = generateCustomMetrics(recentMetrics);

        // Calculate confidence and cost impact
        double confidence = calculateConfidence(recentMetrics.size(), avgCPU, p95CPU);
        double costImpact = estimateCostImpact(currentReplicas, recommendedReplicas);

        String rationale = generateHPARationale(workloadPattern, minReplicas, maxReplicas,
                targetCPU, avgCPU, p95CPU);

        return HPARecommendation.builder()
                .serviceName(serviceName)
                .minReplicas(minReplicas)
                .maxReplicas(maxReplicas)
                .currentReplicas(currentReplicas)
                .recommendedReplicas(recommendedReplicas)
                .targetCPUUtilizationPercentage(targetCPU)
                .targetMemoryUtilizationPercentage(targetMemory)
                .scaleUpPolicy(scaleUpPolicy)
                .scaleDownPolicy(scaleDownPolicy)
                .customMetrics(customMetrics)
                .rationale(rationale)
                .confidenceScore(confidence)
                .estimatedCostImpact(costImpact)
                .build();
    }

    private String determineWorkloadPattern(double avgCPU, double p95CPU, double maxCPU) {
        double volatility = maxCPU - avgCPU;

        if (volatility > 50) {
            return "HIGHLY_VARIABLE"; // Burst traffic
        } else if (volatility > 30) {
            return "MODERATE_VARIABLE"; // Some spikes
        } else if (avgCPU > 70) {
            return "HIGH_STEADY"; // Consistently high
        } else if (avgCPU < 30) {
            return "LOW_STEADY"; // Consistently low
        } else {
            return "STABLE"; // Normal, predictable
        }
    }

    private int calculateMinReplicas(double avgCPU, String pattern) {
        switch (pattern) {
            case "HIGH_STEADY":
                return 3; // Need baseline capacity
            case "HIGHLY_VARIABLE":
                return 2; // Can start lower, will scale up quickly
            case "LOW_STEADY":
                return 1; // Minimal baseline
            default:
                return 2; // Safe default
        }
    }

    private int calculateMaxReplicas(double p95CPU, double maxCPU, String pattern) {
        int baseMax = (int) Math.ceil(maxCPU / 70.0 * 3); // Target 70% utilization

        switch (pattern) {
            case "HIGHLY_VARIABLE":
                return Math.min(15, baseMax + 5); // Extra headroom for spikes
            case "HIGH_STEADY":
                return Math.min(10, baseMax + 2);
            case "LOW_STEADY":
                return Math.min(5, baseMax);
            default:
                return Math.min(10, baseMax + 3);
        }
    }

    private int calculateRecommendedReplicas(double avgCPU, double p95CPU) {
        // Target 70% utilization at P95
        return Math.max(2, (int) Math.ceil(p95CPU / 70.0 * 3));
    }

    private int calculateTargetCPU(double avgCPU, double p95CPU, String pattern) {
        switch (pattern) {
            case "HIGHLY_VARIABLE":
                return 60; // Lower target for burst capacity
            case "HIGH_STEADY":
                return 75; // Can run hotter
            case "LOW_STEADY":
                return 80; // Maximize utilization
            default:
                return 70; // Balanced
        }
    }

    private int calculateTargetMemory(double avgMemory, double p95Memory) {
        // Conservative memory target
        if (p95Memory > 80) {
            return 70;
        } else if (p95Memory > 60) {
            return 75;
        } else {
            return 80;
        }
    }

    private HPARecommendation.ScalingPolicy generateScaleUpPolicy(String pattern) {
        switch (pattern) {
            case "HIGHLY_VARIABLE":
                return HPARecommendation.ScalingPolicy.builder()
                        .stabilizationWindowSeconds(0) // Scale up immediately
                        .periodSeconds(15)
                        .percentagePerScale(100) // Double quickly
                        .podsPerScale(null)
                        .behavior("Aggressive")
                        .description("Fast scale-up for handling traffic spikes")
                        .build();

            case "HIGH_STEADY":
                return HPARecommendation.ScalingPolicy.builder()
                        .stabilizationWindowSeconds(30)
                        .periodSeconds(30)
                        .percentagePerScale(50)
                        .podsPerScale(2)
                        .behavior("Moderate")
                        .description("Steady scale-up for sustained load")
                        .build();

            default:
                return HPARecommendation.ScalingPolicy.builder()
                        .stabilizationWindowSeconds(30)
                        .periodSeconds(30)
                        .percentagePerScale(50)
                        .podsPerScale(1)
                        .behavior("Moderate")
                        .description("Balanced scale-up policy")
                        .build();
        }
    }

    private HPARecommendation.ScalingPolicy generateScaleDownPolicy(String pattern) {
        // Scale down should always be conservative
        return HPARecommendation.ScalingPolicy.builder()
                .stabilizationWindowSeconds(300) // 5 minutes
                .periodSeconds(60)
                .percentagePerScale(25) // Reduce by 25% at a time
                .podsPerScale(1)
                .behavior("Conservative")
                .description("Conservative scale-down to prevent thrashing")
                .build();
    }

    private List<HPARecommendation.CustomMetricTarget> generateCustomMetrics(List<MetricsSnapshot> metrics) {
        List<HPARecommendation.CustomMetricTarget> customMetrics = new ArrayList<>();

        // Check if we have HTTP metrics
        boolean hasHttpMetrics = metrics.stream()
                .anyMatch(m -> m.getHttpRequestCount() != null && m.getHttpRequestCount() > 0);

        if (hasHttpMetrics) {
            // Requests per second metric
            customMetrics.add(HPARecommendation.CustomMetricTarget.builder()
                    .metricName("http_requests_per_second")
                    .metricType("Pods")
                    .targetValue(1000.0) // 1000 RPS per pod
                    .targetType("AverageValue")
                    .description("Scale based on HTTP request rate")
                    .build());
        }

        // Check for database connections
        boolean hasDbMetrics = metrics.stream()
                .anyMatch(m -> m.getHikariActiveConnections() != null);

        if (hasDbMetrics) {
            customMetrics.add(HPARecommendation.CustomMetricTarget.builder()
                    .metricName("hikari_active_connections")
                    .metricType("Pods")
                    .targetValue(15.0) // 15 active connections per pod
                    .targetType("AverageValue")
                    .description("Scale based on database connection pool usage")
                    .build());
        }

        return customMetrics;
    }

    private double calculateConfidence(int sampleSize, double avgCPU, double p95CPU) {
        double confidence = 0.5;

        if (sampleSize > 100) confidence += 0.2;
        else if (sampleSize > 50) confidence += 0.1;

        double variance = p95CPU - avgCPU;
        if (variance < 20) confidence += 0.2; // Stable workload
        else if (variance > 40) confidence -= 0.1; // Variable workload

        return Math.max(0.3, Math.min(0.95, confidence));
    }

    private double estimateCostImpact(int currentReplicas, int recommendedReplicas) {
        double cpuCostPerPod = 30.0; // $30/month per vCPU
        double change = (recommendedReplicas - currentReplicas) * cpuCostPerPod;
        return Math.round(change * 100.0) / 100.0;
    }

    private String generateHPARationale(String pattern, int minReplicas, int maxReplicas,
                                       int targetCPU, double avgCPU, double p95CPU) {
        return String.format("Workload pattern: %s. Average CPU: %.1f%%, P95 CPU: %.1f%%. " +
                        "Recommended HPA: min=%d, max=%d, target=%d%%. " +
                        "This configuration provides optimal scaling for your workload pattern.",
                pattern, avgCPU, p95CPU, minReplicas, maxReplicas, targetCPU);
    }

    private HPARecommendation createDefaultHPARecommendation(String serviceName) {
        return HPARecommendation.builder()
                .serviceName(serviceName)
                .minReplicas(2)
                .maxReplicas(10)
                .currentReplicas(3)
                .recommendedReplicas(3)
                .targetCPUUtilizationPercentage(70)
                .targetMemoryUtilizationPercentage(75)
                .rationale("Default HPA configuration - insufficient metrics for detailed analysis")
                .confidenceScore(0.3)
                .build();
    }

    private double calculateAverage(List<MetricsSnapshot> metrics,
                                   java.util.function.ToDoubleFunction<MetricsSnapshot> extractor) {
        return metrics.stream()
                .mapToDouble(extractor)
                .average()
                .orElse(50.0);
    }

    private double calculatePercentile(List<MetricsSnapshot> metrics,
                                      java.util.function.ToDoubleFunction<MetricsSnapshot> extractor,
                                      int percentile) {
        List<Double> sorted = metrics.stream()
                .mapToDouble(extractor)
                .sorted()
                .boxed()
                .collect(java.util.stream.Collectors.toList());

        if (sorted.isEmpty()) return 50.0;

        int index = (int) Math.ceil(sorted.size() * (percentile / 100.0)) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
