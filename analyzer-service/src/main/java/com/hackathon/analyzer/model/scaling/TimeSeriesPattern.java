package com.hackathon.analyzer.model.scaling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPattern {
    private Boolean hasDailyPattern;
    private Boolean hasWeeklyPattern;
    private Boolean hasMonthlyPattern;

    // Peak hours (0-23)
    private List<Integer> peakHours;

    // Low activity periods
    private List<Integer> lowActivityHours;

    // Day of week patterns
    private Map<DayOfWeek, LoadLevel> weekdayPatterns;

    // Trend
    private String trend; // "INCREASING", "DECREASING", "STABLE", "VOLATILE"
    private Double trendStrength; // 0.0 to 1.0

    // Seasonality
    private Boolean hasSeasonality;
    private Integer seasonalityPeriodHours;

    // Volatility
    private Double volatility; // Standard deviation of usage

    // Description
    private String description;

    public enum LoadLevel {
        VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
    }
}
