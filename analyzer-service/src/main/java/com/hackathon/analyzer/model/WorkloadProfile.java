package com.hackathon.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workload pattern classification result
 *
 * Machine learning-based classification of service workload patterns
 * to enable better resource optimization strategies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadProfile {

    private String serviceName;

    /**
     * Detected workload pattern
     */
    private WorkloadPattern pattern;

    /**
     * Extracted features used for classification
     */
    private WorkloadFeatures features;

    /**
     * Recommended optimization strategy based on pattern
     */
    private OptimizationStrategy recommendedStrategy;

    /**
     * Classification confidence score (0-100)
     */
    private double confidenceScore;

    /**
     * Human-readable description of the pattern
     */
    private String description;

    /**
     * Recommended resource allocation approach
     */
    private String resourceRecommendation;

    /**
     * Predicted cost savings with recommended strategy
     */
    private double estimatedSavings;

    /**
     * Time period analyzed (in days)
     */
    private int analysisWindowDays;
}
