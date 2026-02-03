package com.hackathon.analyzer.service;

import com.hackathon.analyzer.config.AnomalyDetectionConfig;
import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.repository.AnomalyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final AnomalyRepository anomalyRepository;
    private final AnomalyDetectionConfig config;

    /**
     * Analyze all metrics for a service and detect anomalies
     */
    @Transactional
    public List<Anomaly> analyzeAll(String serviceName, List<MetricsSnapshot> snapshots) {
        if (!config.isEnabled() || snapshots.isEmpty()) {
            return List.of();
        }

        List<Anomaly> detectedAnomalies = new ArrayList<>();

        // Analyze different metric types
        detectedAnomalies.addAll(analyzeCpuMetrics(serviceName, snapshots));
        detectedAnomalies.addAll(analyzeMemoryMetrics(serviceName, snapshots));
        detectedAnomalies.addAll(analyzeConnectionPoolMetrics(serviceName, snapshots));
        detectedAnomalies.addAll(analyzeResponseTimeMetrics(serviceName, snapshots));

        // Save all detected anomalies
        if (!detectedAnomalies.isEmpty()) {
            detectedAnomalies = anomalyRepository.saveAll(detectedAnomalies);
            log.info("Detected {} anomalies for service: {}", detectedAnomalies.size(), serviceName);
        }

        return detectedAnomalies;
    }

    /**
     * Analyze CPU usage metrics
     */
    public List<Anomaly> analyzeCpuMetrics(String serviceName, List<MetricsSnapshot> snapshots) {
        List<Anomaly> anomalies = new ArrayList<>();

        // Extract CPU usage values
        List<Double> cpuValues = snapshots.stream()
            .map(MetricsSnapshot::getCpuUsagePercent)
            .filter(v -> v != null && v > 0)
            .collect(Collectors.toList());

        if (cpuValues.size() < 10) {
            return anomalies; // Not enough data
        }

        // Calculate statistics
        DescriptiveStatistics stats = new DescriptiveStatistics();
        cpuValues.forEach(stats::addValue);

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double currentValue = cpuValues.get(cpuValues.size() - 1);
        double ema = calculateExponentialMovingAverage(cpuValues, config.getEma().getAlpha());

        // Detect CPU spike
        double zScore = calculateZScore(currentValue, mean, stdDev);
        if (Math.abs(zScore) > config.getThreshold().getMedium()) {
            AnomalySeverity severity = calculateSeverity(Math.abs(zScore));

            anomalies.add(Anomaly.builder()
                .serviceName(serviceName)
                .metricType(MetricType.CPU_USAGE)
                .metricName("cpu_usage_percent")
                .anomalyType(zScore > 0 ? AnomalyType.SPIKE : AnomalyType.DROP)
                .severity(severity)
                .actualValue(currentValue)
                .expectedValue(ema)
                .zScore(zScore)
                .threshold(config.getThreshold().getMedium())
                .detectedAt(LocalDateTime.now())
                .description(String.format("CPU usage %s detected: %.2f%% (expected: %.2f%%, z-score: %.2f)",
                    zScore > 0 ? "spike" : "drop", currentValue, ema, zScore))
                .build());
        }

        // Detect sustained high CPU
        int sustainedCount = config.getCpu().getSustained().getCount();
        double sustainedThreshold = config.getCpu().getSustained().getThreshold();

        if (cpuValues.size() >= sustainedCount) {
            List<Double> recentValues = cpuValues.subList(cpuValues.size() - sustainedCount, cpuValues.size());
            boolean allHighCpu = recentValues.stream().allMatch(v -> v > sustainedThreshold);

            if (allHighCpu) {
                anomalies.add(Anomaly.builder()
                    .serviceName(serviceName)
                    .metricType(MetricType.CPU_USAGE)
                    .metricName("cpu_usage_percent")
                    .anomalyType(AnomalyType.SUSTAINED_HIGH)
                    .severity(AnomalySeverity.HIGH)
                    .actualValue(currentValue)
                    .expectedValue(sustainedThreshold)
                    .zScore(0.0) // Not applicable for sustained detection
                    .threshold(sustainedThreshold)
                    .detectedAt(LocalDateTime.now())
                    .description(String.format("Sustained high CPU detected: %.2f%% for %d consecutive samples (threshold: %.2f%%)",
                        currentValue, sustainedCount, sustainedThreshold))
                    .build());
            }
        }

        return anomalies;
    }

    /**
     * Analyze memory usage metrics
     */
    public List<Anomaly> analyzeMemoryMetrics(String serviceName, List<MetricsSnapshot> snapshots) {
        List<Anomaly> anomalies = new ArrayList<>();

        // Extract heap usage percentages
        List<Double> memoryValues = snapshots.stream()
            .map(MetricsSnapshot::getHeapUsagePercent)
            .filter(v -> v != null && v > 0)
            .collect(Collectors.toList());

        if (memoryValues.size() < 10) {
            return anomalies;
        }

        // Calculate statistics
        DescriptiveStatistics stats = new DescriptiveStatistics();
        memoryValues.forEach(stats::addValue);

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double currentValue = memoryValues.get(memoryValues.size() - 1);
        double ema = calculateExponentialMovingAverage(memoryValues, config.getEma().getAlpha());

        // Detect memory spike
        double zScore = calculateZScore(currentValue, mean, stdDev);
        if (Math.abs(zScore) > config.getThreshold().getMedium()) {
            AnomalySeverity severity = calculateSeverity(Math.abs(zScore));

            anomalies.add(Anomaly.builder()
                .serviceName(serviceName)
                .metricType(MetricType.MEMORY_USAGE)
                .metricName("heap_usage_percent")
                .anomalyType(zScore > 0 ? AnomalyType.SPIKE : AnomalyType.DROP)
                .severity(severity)
                .actualValue(currentValue)
                .expectedValue(ema)
                .zScore(zScore)
                .threshold(config.getThreshold().getMedium())
                .detectedAt(LocalDateTime.now())
                .description(String.format("Memory usage %s detected: %.2f%% (expected: %.2f%%, z-score: %.2f)",
                    zScore > 0 ? "spike" : "drop", currentValue, ema, zScore))
                .build());
        }

        // Detect memory leak using linear regression
        if (memoryValues.size() >= 20) {
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < memoryValues.size(); i++) {
                regression.addData(i, memoryValues.get(i));
            }

            double slope = regression.getSlope();
            double slopeThreshold = config.getMemory().getLeak().getSlope().getThreshold();

            if (slope > slopeThreshold) {
                anomalies.add(Anomaly.builder()
                    .serviceName(serviceName)
                    .metricType(MetricType.MEMORY_USAGE)
                    .metricName("heap_usage_percent")
                    .anomalyType(AnomalyType.PATTERN_BREAK)
                    .severity(AnomalySeverity.HIGH)
                    .actualValue(currentValue)
                    .expectedValue(memoryValues.get(0))
                    .zScore(0.0)
                    .threshold(slopeThreshold)
                    .detectedAt(LocalDateTime.now())
                    .description(String.format("Potential memory leak detected: increasing trend with slope %.4f (threshold: %.4f)",
                        slope, slopeThreshold))
                    .build());
            }
        }

        return anomalies;
    }

    /**
     * Analyze connection pool metrics
     */
    public List<Anomaly> analyzeConnectionPoolMetrics(String serviceName, List<MetricsSnapshot> snapshots) {
        List<Anomaly> anomalies = new ArrayList<>();

        // Filter snapshots that have connection pool data
        List<MetricsSnapshot> poolSnapshots = snapshots.stream()
            .filter(s -> s.getHikariActiveConnections() != null && s.getHikariMaxConnections() != null)
            .collect(Collectors.toList());

        if (poolSnapshots.isEmpty()) {
            return anomalies; // This service doesn't use HikariCP
        }

        // Calculate pool usage ratios
        List<Double> poolUsageRatios = poolSnapshots.stream()
            .map(s -> s.getHikariActiveConnections().doubleValue() / s.getHikariMaxConnections().doubleValue())
            .collect(Collectors.toList());

        if (poolUsageRatios.size() < 5) {
            return anomalies;
        }

        double currentRatio = poolUsageRatios.get(poolUsageRatios.size() - 1);
        double exhaustionThreshold = config.getPool().getExhaustion().getRatio();

        // Detect pool exhaustion
        if (currentRatio >= exhaustionThreshold) {
            MetricsSnapshot currentSnapshot = poolSnapshots.get(poolSnapshots.size() - 1);

            anomalies.add(Anomaly.builder()
                .serviceName(serviceName)
                .metricType(MetricType.CONNECTION_POOL)
                .metricName("hikari_pool_usage")
                .anomalyType(AnomalyType.SPIKE)
                .severity(AnomalySeverity.CRITICAL)
                .actualValue(currentRatio * 100)
                .expectedValue(exhaustionThreshold * 100)
                .zScore(0.0)
                .threshold(exhaustionThreshold)
                .detectedAt(LocalDateTime.now())
                .description(String.format("Connection pool exhaustion: %d/%d connections in use (%.1f%%)",
                    currentSnapshot.getHikariActiveConnections(),
                    currentSnapshot.getHikariMaxConnections(),
                    currentRatio * 100))
                .build());
        }

        // Detect unusual spikes in pool usage
        DescriptiveStatistics stats = new DescriptiveStatistics();
        poolUsageRatios.forEach(stats::addValue);

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double zScore = calculateZScore(currentRatio, mean, stdDev);

        if (Math.abs(zScore) > config.getThreshold().getHigh() && currentRatio > 0.5) {
            AnomalySeverity severity = calculateSeverity(Math.abs(zScore));

            anomalies.add(Anomaly.builder()
                .serviceName(serviceName)
                .metricType(MetricType.CONNECTION_POOL)
                .metricName("hikari_pool_usage")
                .anomalyType(AnomalyType.SPIKE)
                .severity(severity)
                .actualValue(currentRatio * 100)
                .expectedValue(mean * 100)
                .zScore(zScore)
                .threshold(config.getThreshold().getHigh())
                .detectedAt(LocalDateTime.now())
                .description(String.format("Connection pool usage spike: %.1f%% (expected: %.1f%%, z-score: %.2f)",
                    currentRatio * 100, mean * 100, zScore))
                .build());
        }

        return anomalies;
    }

    /**
     * Analyze response time metrics
     */
    public List<Anomaly> analyzeResponseTimeMetrics(String serviceName, List<MetricsSnapshot> snapshots) {
        List<Anomaly> anomalies = new ArrayList<>();

        // Extract P95 response times
        List<Double> p95Values = snapshots.stream()
            .map(MetricsSnapshot::getHttpRequestDurationP95)
            .filter(v -> v != null && v > 0)
            .collect(Collectors.toList());

        if (p95Values.size() < 10) {
            return anomalies;
        }

        // Calculate statistics
        DescriptiveStatistics stats = new DescriptiveStatistics();
        p95Values.forEach(stats::addValue);

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        double currentValue = p95Values.get(p95Values.size() - 1);
        double ema = calculateExponentialMovingAverage(p95Values, config.getEma().getAlpha());

        // Detect latency spike
        double zScore = calculateZScore(currentValue, mean, stdDev);
        if (zScore > config.getThreshold().getMedium()) {
            AnomalySeverity severity = calculateSeverity(Math.abs(zScore));

            anomalies.add(Anomaly.builder()
                .serviceName(serviceName)
                .metricType(MetricType.RESPONSE_TIME)
                .metricName("http_request_duration_p95")
                .anomalyType(AnomalyType.SPIKE)
                .severity(severity)
                .actualValue(currentValue)
                .expectedValue(ema)
                .zScore(zScore)
                .threshold(config.getThreshold().getMedium())
                .detectedAt(LocalDateTime.now())
                .description(String.format("Response time spike detected: %.2fms (expected: %.2fms, z-score: %.2f)",
                    currentValue, ema, zScore))
                .build());
        }

        // Detect sustained high latency
        double spikeThreshold = config.getResponse().getTime().getSpike().getThreshold();
        if (currentValue > spikeThreshold) {
            anomalies.add(Anomaly.builder()
                .serviceName(serviceName)
                .metricType(MetricType.RESPONSE_TIME)
                .metricName("http_request_duration_p95")
                .anomalyType(AnomalyType.SUSTAINED_HIGH)
                .severity(AnomalySeverity.HIGH)
                .actualValue(currentValue)
                .expectedValue(spikeThreshold)
                .zScore(0.0)
                .threshold(spikeThreshold)
                .detectedAt(LocalDateTime.now())
                .description(String.format("High latency detected: %.2fms exceeds threshold of %.2fms",
                    currentValue, spikeThreshold))
                .build());
        }

        return anomalies;
    }

    /**
     * Calculate Z-score for a value
     */
    private double calculateZScore(double value, double mean, double stdDev) {
        if (stdDev == 0) {
            return 0;
        }
        return (value - mean) / stdDev;
    }

    /**
     * Calculate simple moving average
     */
    private double calculateMovingAverage(List<Double> values, int windowSize) {
        if (values.isEmpty()) {
            return 0;
        }

        int size = Math.min(windowSize, values.size());
        List<Double> window = values.subList(values.size() - size, values.size());

        return window.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    /**
     * Calculate Exponential Moving Average (EMA)
     */
    private double calculateExponentialMovingAverage(List<Double> values, double alpha) {
        if (values.isEmpty()) {
            return 0;
        }

        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1 - alpha) * ema;
        }

        return ema;
    }

    /**
     * Calculate severity based on Z-score
     */
    private AnomalySeverity calculateSeverity(double absZScore) {
        if (absZScore >= config.getThreshold().getCritical()) {
            return AnomalySeverity.CRITICAL;
        } else if (absZScore >= config.getThreshold().getHigh()) {
            return AnomalySeverity.HIGH;
        } else if (absZScore >= config.getThreshold().getMedium()) {
            return AnomalySeverity.MEDIUM;
        } else {
            return AnomalySeverity.LOW;
        }
    }
}
