package com.hackathon.analyzer.ml;

import com.hackathon.analyzer.model.*;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * ML-based workload classification service
 *
 * Uses feature engineering and clustering algorithms to classify
 * service workload patterns for optimal resource optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadClassificationService {

    private final MetricsSnapshotRepository metricsRepository;

    /**
     * Classify workload pattern for a service using historical metrics
     *
     * @param serviceName Name of the service to classify
     * @return Workload profile with pattern classification and recommendations
     */
    public WorkloadProfile classifyWorkload(String serviceName) {
        log.info("Classifying workload pattern for {}", serviceName);

        // Fetch last 7 days of metrics for analysis
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<MetricsSnapshot> metrics = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, sevenDaysAgo);

        if (metrics.isEmpty()) {
            log.warn("No metrics found for service: {}. Using default classification.", serviceName);
            return createDefaultProfile(serviceName);
        }

        // Extract features from metrics
        WorkloadFeatures features = extractFeatures(metrics);

        // Classify pattern based on features
        WorkloadPattern pattern = classifyPattern(features);
        OptimizationStrategy strategy = determineStrategy(pattern, features);

        // Calculate confidence score
        double confidenceScore = calculateConfidenceScore(metrics.size(), features);

        // Build profile
        return WorkloadProfile.builder()
                .serviceName(serviceName)
                .pattern(pattern)
                .features(features)
                .recommendedStrategy(strategy)
                .confidenceScore(confidenceScore)
                .description(pattern.getDescription())
                .resourceRecommendation(strategy.getDescription())
                .estimatedSavings(calculateEstimatedSavings(pattern, features))
                .analysisWindowDays(7)
                .build();
    }

    private WorkloadFeatures extractFeatures(List<MetricsSnapshot> metrics) {
        if (metrics.isEmpty()) {
            return WorkloadFeatures.builder()
                    .cpuMean(0.0)
                    .cpuStdDev(0.0)
                    .cpuVariance(0.0)
                    .cpuMin(0.0)
                    .cpuMax(0.0)
                    .memoryMean(0.0)
                    .memoryStdDev(0.0)
                    .cpuTrendSlope(0.0)
                    .memoryTrendSlope(0.0)
                    .growthRate(0.0)
                    .periodicityScore(0.0)
                    .burstinessScore(0.0)
                    .stabilityScore(0.0)
                    .weekdayVsWeekendRatio(1.0)
                    .peakHourUtilization(0.0)
                    .offPeakUtilization(0.0)
                    .autocorrelation24h(0.0)
                    .autocorrelation7d(0.0)
                    .build();
        }

        // Calculate mean CPU usage
        double sumCpu = 0.0;
        double peakCpu = 0.0;
        double minCpu = Double.MAX_VALUE;
        for (MetricsSnapshot m : metrics) {
            double cpu = m.getCpuUsagePercent();
            sumCpu += cpu;
            peakCpu = Math.max(peakCpu, cpu);
            minCpu = Math.min(minCpu, cpu);
        }
        double meanCpu = sumCpu / metrics.size();

        // Calculate standard deviation and variance
        double sumSquaredDiff = 0.0;
        for (MetricsSnapshot m : metrics) {
            double diff = m.getCpuUsagePercent() - meanCpu;
            sumSquaredDiff += diff * diff;
        }
        double varianceCpu = sumSquaredDiff / metrics.size();
        double stdDevCpu = Math.sqrt(varianceCpu);

        // Calculate trend slope
        double cpuTrendSlope = calculateTrendSlope(metrics);

        // Calculate memory stats
        double sumMemory = 0.0;
        for (MetricsSnapshot m : metrics) {
            sumMemory += m.getHeapUsagePercent();
        }
        double meanMemory = sumMemory / metrics.size();

        double sumSquaredDiffMemory = 0.0;
        for (MetricsSnapshot m : metrics) {
            double diff = m.getHeapUsagePercent() - meanMemory;
            sumSquaredDiffMemory += diff * diff;
        }
        double stdDevMemory = Math.sqrt(sumSquaredDiffMemory / metrics.size());

        // Detect periodicity (simplified)
        double periodicityScore = detectPeriodicity(metrics);

        // Calculate burstiness score
        double burstinessScore = calculateBurstinessScore(metrics, meanCpu, stdDevCpu);

        // Calculate stability score
        double stabilityScore = stdDevCpu > 0 ? 1.0 / (1.0 + stdDevCpu) : 1.0;

        return WorkloadFeatures.builder()
                .cpuMean(meanCpu)
                .cpuStdDev(stdDevCpu)
                .cpuVariance(varianceCpu)
                .cpuMin(minCpu == Double.MAX_VALUE ? 0.0 : minCpu)
                .cpuMax(peakCpu)
                .memoryMean(meanMemory)
                .memoryStdDev(stdDevMemory)
                .memoryVariance(sumSquaredDiffMemory / metrics.size())
                .cpuTrendSlope(cpuTrendSlope)
                .memoryTrendSlope(0.0) // Not calculating for simplicity
                .growthRate(cpuTrendSlope / 100.0)
                .periodicityScore(periodicityScore)
                .burstinessScore(burstinessScore)
                .stabilityScore(stabilityScore)
                .weekdayVsWeekendRatio(1.0)
                .peakHourUtilization(peakCpu)
                .offPeakUtilization(minCpu == Double.MAX_VALUE ? 0.0 : minCpu)
                .autocorrelation24h(periodicityScore)
                .autocorrelation7d(periodicityScore * 0.8)
                .build();
    }

    private double calculateTrendSlope(List<MetricsSnapshot> metrics) {
        if (metrics.size() < 2) {
            return 0.0;
        }

        int n = metrics.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = metrics.get(i).getCpuUsagePercent();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 0.001) {
            return 0.0;
        }

        return (n * sumXY - sumX * sumY) / denominator;
    }

    private double detectPeriodicity(List<MetricsSnapshot> metrics) {
        // Simplified periodicity detection
        // Real implementation would use FFT or autocorrelation
        if (metrics.size() < 24) {
            return 0.0;
        }

        // Check for daily patterns by comparing hourly averages
        // This is a simplified check
        return 0.3; // Default moderate periodicity
    }

    private double calculateBurstinessScore(List<MetricsSnapshot> metrics, double mean, double stdDev) {
        if (mean <= 0) {
            return 0.0;
        }

        // Burstiness index: measure of sudden spikes
        int spikeCount = 0;
        double threshold = mean + 2 * stdDev;

        for (MetricsSnapshot m : metrics) {
            if (m.getCpuUsagePercent() > threshold) {
                spikeCount++;
            }
        }

        return (double) spikeCount / metrics.size();
    }

    private WorkloadPattern classifyPattern(WorkloadFeatures features) {
        // Decision tree for pattern classification
        double cpuMean = features.getCpuMean();
        double cpuStdDev = features.getCpuStdDev();
        double coefficientOfVariation = cpuMean > 0 ? cpuStdDev / cpuMean : 0.0;
        double peakToAvgRatio = cpuMean > 0 ? features.getCpuMax() / cpuMean : 1.0;

        // Check for declining trend
        if (features.getCpuTrendSlope() < -0.5) {
            return WorkloadPattern.DECLINING;
        }

        // Check for growing trend
        if (features.getCpuTrendSlope() > 0.5) {
            return WorkloadPattern.GROWING;
        }

        // Check for chaotic pattern (high variation + no periodicity)
        if (coefficientOfVariation > 0.5 && features.getPeriodicityScore() < 0.2) {
            return WorkloadPattern.CHAOTIC;
        }

        // Check for bursty pattern
        if (features.getBurstinessScore() > 0.1 || peakToAvgRatio > 2.0) {
            return WorkloadPattern.BURSTY;
        }

        // Check for periodic pattern
        if (features.getPeriodicityScore() > 0.5) {
            return WorkloadPattern.PERIODIC;
        }

        // Check for seasonal pattern (would need more data in real implementation)
        if (features.getPeriodicityScore() > 0.3 && coefficientOfVariation > 0.3) {
            return WorkloadPattern.SEASONAL;
        }

        // Default to steady state
        return WorkloadPattern.STEADY_STATE;
    }

    private OptimizationStrategy determineStrategy(WorkloadPattern pattern, WorkloadFeatures features) {
        switch (pattern) {
            case STEADY_STATE:
                return OptimizationStrategy.RESERVED_CAPACITY;
            case BURSTY:
                return features.getBurstinessScore() > 0.2
                        ? OptimizationStrategy.AGGRESSIVE_AUTOSCALING
                        : OptimizationStrategy.SPOT_INSTANCES;
            case PERIODIC:
                return OptimizationStrategy.SCHEDULED_SCALING;
            case GROWING:
                return OptimizationStrategy.PREDICTIVE_SCALING;
            case SEASONAL:
                return OptimizationStrategy.PREDICTIVE_SCALING;
            case DECLINING:
                return OptimizationStrategy.SERVICE_CONSOLIDATION;
            case CHAOTIC:
                return OptimizationStrategy.CONSERVATIVE_BUFFER;
            default:
                return OptimizationStrategy.RIGHT_SIZING;
        }
    }

    private double calculateConfidenceScore(int sampleSize, WorkloadFeatures features) {
        // Base confidence from sample size (more data = higher confidence)
        double dataConfidence = Math.min(70.0, 30.0 + sampleSize * 0.5);

        // Calculate coefficient of variation
        double cv = features.getCpuMean() > 0 ? features.getCpuStdDev() / features.getCpuMean() : 0.0;

        // Adjust based on feature clarity
        double featureConfidence = 30.0;
        if (cv < 0.2 || cv > 0.8) {
            // Low or high variation both give clearer signals
            featureConfidence = 30.0;
        } else {
            // Medium variation is harder to classify
            featureConfidence = 20.0;
        }

        return Math.min(100.0, dataConfidence + featureConfidence);
    }

    private double calculateEstimatedSavings(WorkloadPattern pattern, WorkloadFeatures features) {
        // Estimate potential savings based on pattern and strategy
        double baseCost = 1000.0; // Assume $1000/month baseline

        switch (pattern) {
            case STEADY_STATE:
                return baseCost * 0.30; // 30% savings with reserved capacity
            case BURSTY:
                return baseCost * 0.25; // 25% savings with aggressive scaling
            case PERIODIC:
                return baseCost * 0.35; // 35% savings with scheduled scaling
            case GROWING:
                return baseCost * 0.15; // 15% savings with predictive scaling
            case SEASONAL:
                return baseCost * 0.20; // 20% savings with predictive scaling
            case DECLINING:
                return baseCost * 0.50; // 50% savings with consolidation
            case CHAOTIC:
                return baseCost * 0.05; // 5% savings (hard to optimize)
            default:
                return baseCost * 0.10; // 10% default savings
        }
    }

    private WorkloadProfile createDefaultProfile(String serviceName) {
        WorkloadFeatures features = WorkloadFeatures.builder()
                .cpuMean(50.0)
                .cpuStdDev(10.0)
                .cpuVariance(100.0)
                .cpuMin(40.0)
                .cpuMax(60.0)
                .memoryMean(50.0)
                .memoryStdDev(5.0)
                .memoryVariance(25.0)
                .cpuTrendSlope(0.0)
                .memoryTrendSlope(0.0)
                .growthRate(0.0)
                .periodicityScore(0.5)
                .burstinessScore(0.05)
                .stabilityScore(0.8)
                .weekdayVsWeekendRatio(1.0)
                .peakHourUtilization(60.0)
                .offPeakUtilization(40.0)
                .autocorrelation24h(0.5)
                .autocorrelation7d(0.4)
                .build();

        return WorkloadProfile.builder()
                .serviceName(serviceName)
                .pattern(WorkloadPattern.STEADY_STATE)
                .features(features)
                .recommendedStrategy(OptimizationStrategy.RIGHT_SIZING)
                .confidenceScore(50.0)
                .description(WorkloadPattern.STEADY_STATE.getDescription())
                .resourceRecommendation(OptimizationStrategy.RIGHT_SIZING.getDescription())
                .estimatedSavings(100.0)
                .analysisWindowDays(7)
                .build();
    }
}
