package com.hackathon.analyzer.service.strategy;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CPU resource analysis strategy.
 * Detects throttling and calculates optimal CPU request/limit in millicores.
 */
@Slf4j
@Component
public class CpuAnalysisStrategy implements ResourceAnalysisStrategy {

    private static final double SAFETY_MARGIN = 0.20;
    private static final double CPU_THROTTLE_P95_THRESHOLD = 80.0;
    private static final double CPU_THROTTLE_MAX_THRESHOLD = 90.0;
    private static final int MIN_CPU_MILLICORES = 100;
    private static final int BURST_MULTIPLIER = 2;

    @Override
    public Map<String, String> detectIssues(List<MetricsSnapshot> snapshots) {
        Map<String, String> issues = new HashMap<>();
        DescriptiveStatistics cpuStats = buildStats(snapshots);

        if (cpuStats.getPercentile(95) > CPU_THROTTLE_P95_THRESHOLD
                || cpuStats.getMax() > CPU_THROTTLE_MAX_THRESHOLD) {
            issues.put("CPU Throttling",
                    String.format("CPU P95=%.1f%% exceeds %.0f%% threshold — performance degradation likely",
                            cpuStats.getPercentile(95), CPU_THROTTLE_P95_THRESHOLD));
            log.warn("CPU throttling detected: P95={:.1f}%, max={:.1f}%",
                    cpuStats.getPercentile(95), cpuStats.getMax());
        }
        return issues;
    }

    @Override
    public void applyRecommendations(AnalysisResult.AnalysisResultBuilder builder,
            List<MetricsSnapshot> snapshots,
            DescriptiveStatistics stats) {
        String cpuRequest = calculateCpuRequest(stats);
        String cpuLimit = calculateCpuLimit(cpuRequest);

        builder.recommendedCpuRequest(cpuRequest)
                .recommendedCpuLimit(cpuLimit)
                .p95CpuUsage(stats.getPercentile(95))
                .p99CpuUsage(stats.getPercentile(99))
                .maxCpuUsage(stats.getMax())
                .cpuThrottlingDetected(
                        stats.getPercentile(95) > CPU_THROTTLE_P95_THRESHOLD
                                || stats.getMax() > CPU_THROTTLE_MAX_THRESHOLD);
    }

    @Override
    public String getMetricType() {
        return "CPU";
    }

    private DescriptiveStatistics buildStats(List<MetricsSnapshot> snapshots) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        snapshots.forEach(s -> stats.addValue(s.getCpuUsagePercent()));
        return stats;
    }

    private String calculateCpuRequest(DescriptiveStatistics cpuStats) {
        double p95Percent = cpuStats.getPercentile(95);
        double cpuCores = (p95Percent / 100.0) * (1 + SAFETY_MARGIN);
        int millicores = (int) Math.ceil(cpuCores * 1000);
        millicores = Math.max(MIN_CPU_MILLICORES, millicores);
        return millicores + "m";
    }

    private String calculateCpuLimit(String request) {
        int requestMillicores = Integer.parseInt(request.replace("m", ""));
        return (requestMillicores * BURST_MULTIPLIER) + "m";
    }
}
