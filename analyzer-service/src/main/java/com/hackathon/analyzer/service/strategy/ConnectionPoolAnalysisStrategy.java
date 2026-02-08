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
 * Connection pool resource analysis strategy.
 * Detects HikariCP pool exhaustion and calculates optimal
 * maximum pool size and minimum idle connections.
 */
@Slf4j
@Component
public class ConnectionPoolAnalysisStrategy implements ResourceAnalysisStrategy {

    private static final double SAFETY_MARGIN = 0.20;
    private static final double EXHAUSTION_RATIO = 0.5;
    private static final int MIN_POOL_SIZE = 10;
    private static final int MIN_IDLE = 5;
    private static final int IDLE_DIVISOR = 5;

    @Override
    public Map<String, String> detectIssues(List<MetricsSnapshot> snapshots) {
        Map<String, String> issues = new HashMap<>();

        if (isPoolExhausted(snapshots)) {
            issues.put("Connection Pool Exhaustion",
                    "HikariCP pool frequently at maximum capacity — >50% of samples at max connections");
            log.warn("Connection pool exhaustion detected");
        }
        return issues;
    }

    @Override
    public void applyRecommendations(AnalysisResult.AnalysisResultBuilder builder,
            List<MetricsSnapshot> snapshots,
            DescriptiveStatistics stats) {
        Integer maxPool = calculateMaxPoolSize(stats);
        Integer minIdle = calculateMinIdle(maxPool);

        builder.recommendedMaxPoolSize(maxPool)
                .recommendedMinIdle(minIdle)
                .connectionPoolExhaustion(isPoolExhausted(snapshots));
    }

    @Override
    public String getMetricType() {
        return "CONNECTION_POOL";
    }

    private boolean isPoolExhausted(List<MetricsSnapshot> snapshots) {
        long exhaustedCount = snapshots.stream()
                .filter(s -> s.getHikariActiveConnections() != null
                        && s.getHikariMaxConnections() != null)
                .filter(s -> s.getHikariActiveConnections().equals(s.getHikariMaxConnections()))
                .count();

        return exhaustedCount > snapshots.size() * EXHAUSTION_RATIO;
    }

    private Integer calculateMaxPoolSize(DescriptiveStatistics connectionStats) {
        if (connectionStats.getN() == 0) {
            return null;
        }
        double p95Active = connectionStats.getPercentile(95);
        int recommended = (int) Math.ceil(p95Active * (1 + SAFETY_MARGIN));
        return Math.max(MIN_POOL_SIZE, recommended);
    }

    private Integer calculateMinIdle(Integer maxPool) {
        if (maxPool == null) {
            return null;
        }
        return Math.max(MIN_IDLE, maxPool / IDLE_DIVISOR);
    }
}
