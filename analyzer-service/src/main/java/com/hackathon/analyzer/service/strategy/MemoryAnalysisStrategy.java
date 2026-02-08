package com.hackathon.analyzer.service.strategy;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Memory resource analysis strategy.
 * Detects memory leaks via trend analysis and calculates optimal
 * memory request/limit and JVM heap settings.
 */
@Slf4j
@Component
public class MemoryAnalysisStrategy implements ResourceAnalysisStrategy {

    private static final double SAFETY_MARGIN = 0.20;
    private static final double LEAK_GROWTH_THRESHOLD = 1.20;
    private static final int MIN_LEAK_SAMPLES = 10;
    private static final long MIN_MEMORY_MI = 256;
    private static final double LIMIT_MULTIPLIER = 1.5;
    private static final double JVM_XMS_RATIO = 0.75;
    private static final double JVM_XMX_RATIO = 0.85;

    @Override
    public Map<String, String> detectIssues(List<MetricsSnapshot> snapshots) {
        Map<String, String> issues = new HashMap<>();

        if (isMemoryLeaking(snapshots)) {
            issues.put("Memory Leak",
                    "Heap usage shows continuous growth pattern — second-half average exceeds first-half by >20%");
            log.warn("Memory leak detected in {} snapshots", snapshots.size());
        }
        return issues;
    }

    @Override
    public void applyRecommendations(AnalysisResult.AnalysisResultBuilder builder,
            List<MetricsSnapshot> snapshots,
            DescriptiveStatistics stats) {
        String memoryRequest = calculateMemoryRequest(snapshots);
        String memoryLimit = calculateMemoryLimit(memoryRequest);
        String jvmXms = calculateJvmXms(memoryRequest);
        String jvmXmx = calculateJvmXmx(memoryRequest);

        builder.recommendedMemoryRequest(memoryRequest)
                .recommendedMemoryLimit(memoryLimit)
                .recommendedJvmXms(jvmXms)
                .recommendedJvmXmx(jvmXmx)
                .p95MemoryUsage(stats.getPercentile(95))
                .p99MemoryUsage(stats.getPercentile(99))
                .maxMemoryUsage(stats.getMax())
                .memoryLeakDetected(isMemoryLeaking(snapshots));
    }

    @Override
    public String getMetricType() {
        return "MEMORY";
    }

    private boolean isMemoryLeaking(List<MetricsSnapshot> snapshots) {
        if (snapshots.size() < MIN_LEAK_SAMPLES) {
            return false;
        }

        List<Double> memoryUsages = snapshots.stream()
                .map(MetricsSnapshot::getHeapUsagePercent)
                .collect(Collectors.toList());

        int midpoint = memoryUsages.size() / 2;
        double firstHalfAvg = memoryUsages.subList(0, midpoint).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalfAvg = memoryUsages.subList(midpoint, memoryUsages.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        return secondHalfAvg > firstHalfAvg * LEAK_GROWTH_THRESHOLD;
    }

    private String calculateMemoryRequest(List<MetricsSnapshot> snapshots) {
        long maxHeapBytes = snapshots.stream()
                .mapToLong(MetricsSnapshot::getHeapUsedBytes)
                .max()
                .orElse(MIN_MEMORY_MI * 1024 * 1024);

        long recommendedBytes = (long) (maxHeapBytes * (1 + SAFETY_MARGIN));
        long recommendedMi = Math.max(MIN_MEMORY_MI, recommendedBytes / (1024 * 1024));
        return recommendedMi + "Mi";
    }

    private String calculateMemoryLimit(String request) {
        long requestMi = Long.parseLong(request.replace("Mi", ""));
        return (long) (requestMi * LIMIT_MULTIPLIER) + "Mi";
    }

    private String calculateJvmXms(String memoryRequest) {
        long requestMi = Long.parseLong(memoryRequest.replace("Mi", ""));
        return (long) (requestMi * JVM_XMS_RATIO) + "m";
    }

    private String calculateJvmXmx(String memoryRequest) {
        long requestMi = Long.parseLong(memoryRequest.replace("Mi", ""));
        return (long) (requestMi * JVM_XMX_RATIO) + "m";
    }
}
