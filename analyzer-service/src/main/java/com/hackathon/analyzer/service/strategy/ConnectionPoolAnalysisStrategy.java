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
 *
 * Exhaustion is detected via two signals:
 * 1. Active connections reaching maximum pool size
 * 2. Pending (waiting) connections indicating pool pressure
 */
@Slf4j
@Component
public class ConnectionPoolAnalysisStrategy implements ResourceAnalysisStrategy {

    private static final double SAFETY_MARGIN = 0.20;
    /** Lowered from 0.50 to 0.10 to catch burst-type exhaustion patterns */
    private static final double EXHAUSTION_RATIO = 0.10;
    private static final int MIN_POOL_SIZE = 10;
    private static final int MIN_IDLE = 5;
    private static final int IDLE_DIVISOR = 5;

    @Override
    public Map<String, String> detectIssues(List<MetricsSnapshot> snapshots) {
        Map<String, String> issues = new HashMap<>();

        if (isPoolExhausted(snapshots)) {
            issues.put("Connection Pool Exhaustion",
                    "HikariCP pool frequently at maximum capacity — active connections equal max or pending requests detected");
            log.warn("Connection pool exhaustion detected");
        }
        return issues;
    }

    @Override
    public void applyRecommendations(AnalysisResult.AnalysisResultBuilder builder,
            List<MetricsSnapshot> snapshots,
            DescriptiveStatistics stats) {
        Integer maxPool = calculateMaxPoolSize(stats, snapshots);
        Integer minIdle = calculateMinIdle(maxPool);

        builder.recommendedMaxPoolSize(maxPool)
                .recommendedMinIdle(minIdle)
                .connectionPoolExhaustion(isPoolExhausted(snapshots));
    }

    @Override
    public String getMetricType() {
        return "CONNECTION_POOL";
    }

    /**
     * Pool is considered exhausted if:
     * - Active connections equal max connections in >10% of samples, OR
     * - Any sample has pending (waiting) connections > 0
     */
    private boolean isPoolExhausted(List<MetricsSnapshot> snapshots) {
        long exhaustedCount = snapshots.stream()
                .filter(s -> s.getHikariActiveConnections() != null
                        && s.getHikariMaxConnections() != null
                        && s.getHikariMaxConnections() > 0)
                .filter(s -> s.getHikariActiveConnections().equals(s.getHikariMaxConnections()))
                .count();

        boolean activeAtMax = exhaustedCount > snapshots.size() * EXHAUSTION_RATIO;

        // Also check for pending connections — any pending request means pool pressure
        boolean hasPending = snapshots.stream()
                .anyMatch(s -> s.getHikariPendingConnections() != null
                        && s.getHikariPendingConnections() > 0);

        if (activeAtMax) {
            log.info("Pool exhaustion: {}/{} samples at max capacity", exhaustedCount, snapshots.size());
        }
        if (hasPending) {
            log.info("Pool pressure: pending connections detected in snapshots");
        }

        return activeAtMax || hasPending;
    }

    /**
     * Calculate recommended max pool size based on:
     * - P95 of active connections + safety margin
     * - Max observed active connections + pending (to handle burst demand)
     * - At least current max connections if exhaustion is detected
     */
    private Integer calculateMaxPoolSize(DescriptiveStatistics connectionStats,
            List<MetricsSnapshot> snapshots) {
        if (connectionStats.getN() == 0) {
            return null;
        }

        double p95Active = connectionStats.getPercentile(95);
        int recommendedFromP95 = (int) Math.ceil(p95Active * (1 + SAFETY_MARGIN));

        // Calculate demand-based recommendation:
        // max active + max pending gives us the true demand
        int maxActive = snapshots.stream()
                .filter(s -> s.getHikariActiveConnections() != null)
                .mapToInt(MetricsSnapshot::getHikariActiveConnections)
                .max().orElse(0);

        int maxPending = snapshots.stream()
                .filter(s -> s.getHikariPendingConnections() != null)
                .mapToInt(MetricsSnapshot::getHikariPendingConnections)
                .max().orElse(0);

        // Total demand = active connections + pending requests
        int totalDemand = maxActive + maxPending;
        int recommendedFromDemand = (int) Math.ceil(totalDemand * (1 + SAFETY_MARGIN));

        // Take the higher of P95-based and demand-based recommendations
        int recommended = Math.max(recommendedFromP95, recommendedFromDemand);

        log.info("Pool sizing: P95 active={}, max active={}, max pending={}, total demand={}, recommended={}",
                p95Active, maxActive, maxPending, totalDemand, recommended);

        return Math.max(MIN_POOL_SIZE, recommended);
    }

    private Integer calculateMinIdle(Integer maxPool) {
        if (maxPool == null) {
            return null;
        }
        return Math.max(MIN_IDLE, maxPool / IDLE_DIVISOR);
    }
}
