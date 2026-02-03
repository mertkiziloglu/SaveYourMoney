package com.hackathon.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cost forecast prediction result
 *
 * Uses time-series forecasting to predict future costs
 * based on historical metrics and trends
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostForecast {

    private String serviceName;

    private int daysAhead;

    /**
     * Predicted daily costs for the forecast period
     */
    private double[] predictions;

    /**
     * Upper bound of 95% confidence interval
     */
    private double[] upperBound;

    /**
     * Lower bound of 95% confidence interval
     */
    private double[] lowerBound;

    /**
     * Confidence level (e.g., 95.0 for 95% confidence)
     */
    private double confidenceLevel;

    /**
     * ML model used for prediction
     */
    private String modelType;

    /**
     * Current monthly cost (baseline)
     */
    private double currentMonthlyCost;

    /**
     * Predicted monthly cost (average of forecast)
     */
    private double predictedMonthlyCost;

    /**
     * Trend direction: INCREASING, DECREASING, STABLE
     */
    private String trend;

    /**
     * Percentage change from current cost
     */
    private double percentageChange;

    /**
     * Model accuracy score (0-100)
     */
    private double accuracyScore;

    /**
     * Warning if predicted cost exceeds threshold
     */
    private String warning;
}
