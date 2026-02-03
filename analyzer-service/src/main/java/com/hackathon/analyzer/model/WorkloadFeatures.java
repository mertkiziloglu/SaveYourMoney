package com.hackathon.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Extracted features from workload metrics for ML classification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadFeatures {

    // Statistical features
    private double cpuMean;
    private double cpuStdDev;
    private double cpuVariance;
    private double cpuMin;
    private double cpuMax;

    private double memoryMean;
    private double memoryStdDev;
    private double memoryVariance;
    private double memoryMin;
    private double memoryMax;

    // Trend features
    private double cpuTrendSlope;          // Linear regression slope
    private double memoryTrendSlope;       // Linear regression slope
    private double growthRate;             // Overall growth rate

    // Pattern features
    private double periodicityScore;       // 0-1: how periodic is the pattern
    private double burstinessScore;        // 0-1: how bursty is the pattern
    private double stabilityScore;         // 0-1: how stable is the pattern

    // Time-based features
    private double weekdayVsWeekendRatio;  // Weekday avg / Weekend avg
    private double peakHourUtilization;    // Max hourly average
    private double offPeakUtilization;     // Min hourly average

    // Autocorrelation features
    private double autocorrelation24h;     // Daily pattern strength
    private double autocorrelation7d;      // Weekly pattern strength

    /**
     * Convert features to vector for ML algorithms
     */
    public double[] toVector() {
        return new double[] {
            cpuMean, cpuStdDev, cpuVariance,
            memoryMean, memoryStdDev, memoryVariance,
            cpuTrendSlope, memoryTrendSlope, growthRate,
            periodicityScore, burstinessScore, stabilityScore,
            weekdayVsWeekendRatio, peakHourUtilization, offPeakUtilization,
            autocorrelation24h, autocorrelation7d
        };
    }
}
