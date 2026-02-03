package com.hackathon.analyzer.service.scaling;

import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.scaling.ScalingPrediction;
import com.hackathon.analyzer.model.scaling.TimeSeriesPattern;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictiveScalingService {

    private final MetricsSnapshotRepository metricsRepository;

    /**
     * Predict scaling needs for the next 24 hours
     */
    public List<ScalingPrediction> predictNext24Hours(String serviceName) {
        log.info("Generating 24-hour predictions for service: {}", serviceName);

        // Get historical data (last 7 days)
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        List<MetricsSnapshot> historicalData = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, since);

        if (historicalData.size() < 100) {
            log.warn("Insufficient data for predictions: {} samples", historicalData.size());
            return Collections.emptyList();
        }

        // Detect patterns
        TimeSeriesPattern pattern = detectPatterns(historicalData);

        // Generate hourly predictions for next 24 hours
        List<ScalingPrediction> predictions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int hour = 1; hour <= 24; hour++) {
            LocalDateTime forecastTime = now.plusHours(hour);
            ScalingPrediction prediction = predictForTime(serviceName, forecastTime, historicalData, pattern);
            predictions.add(prediction);
        }

        return predictions;
    }

    /**
     * Predict scaling for a specific future time
     */
    public ScalingPrediction predictForTime(String serviceName, LocalDateTime targetTime,
                                           List<MetricsSnapshot> historicalData, TimeSeriesPattern pattern) {

        int hourOfDay = targetTime.getHour();
        DayOfWeek dayOfWeek = targetTime.getDayOfWeek();

        // Get similar historical periods
        List<MetricsSnapshot> similarPeriods = findSimilarPeriods(historicalData, hourOfDay, dayOfWeek);

        if (similarPeriods.isEmpty()) {
            return createDefaultPrediction(serviceName, targetTime);
        }

        // Calculate predicted metrics
        double predictedCPU = similarPeriods.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average()
                .orElse(50.0);

        double predictedMemory = similarPeriods.stream()
                .mapToDouble(MetricsSnapshot::getHeapUsagePercent)
                .average()
                .orElse(50.0);

        // Apply trend adjustment
        predictedCPU = applyTrendAdjustment(predictedCPU, pattern);
        predictedMemory = applyTrendAdjustment(predictedMemory, pattern);

        // Calculate recommended replicas
        int currentReplicas = 3; // Default, should come from K8s API
        int recommendedReplicas = calculateRecommendedReplicas(predictedCPU, predictedMemory, currentReplicas);

        // Calculate confidence
        double confidence = calculateConfidence(similarPeriods.size(), pattern);

        // Generate reason
        String reason = generatePredictionReason(hourOfDay, dayOfWeek, predictedCPU, pattern);

        // Detect scaling events
        List<ScalingPrediction.ScalingEvent> events = detectScalingEvents(
                targetTime, predictedCPU, recommendedReplicas, pattern);

        return ScalingPrediction.builder()
                .serviceName(serviceName)
                .predictionTime(LocalDateTime.now())
                .forecastFor(targetTime)
                .predictedCPUUsage(Math.round(predictedCPU * 100.0) / 100.0)
                .predictedMemoryUsage(Math.round(predictedMemory * 100.0) / 100.0)
                .predictedRequestRate(estimateRequestRate(predictedCPU))
                .currentReplicas(currentReplicas)
                .recommendedReplicas(recommendedReplicas)
                .confidence(confidence)
                .reason(reason)
                .detectedPattern(pattern)
                .upcomingEvents(events)
                .build();
    }

    /**
     * Detect time series patterns in historical data
     */
    public TimeSeriesPattern detectPatterns(List<MetricsSnapshot> historicalData) {
        if (historicalData.isEmpty()) {
            return TimeSeriesPattern.builder()
                    .hasDailyPattern(false)
                    .hasWeeklyPattern(false)
                    .trend("UNKNOWN")
                    .build();
        }

        // Detect daily pattern
        boolean hasDailyPattern = detectDailyPattern(historicalData);

        // Detect weekly pattern
        boolean hasWeeklyPattern = detectWeeklyPattern(historicalData);

        // Identify peak hours
        List<Integer> peakHours = identifyPeakHours(historicalData);

        // Identify low activity hours
        List<Integer> lowActivityHours = identifyLowActivityHours(historicalData);

        // Detect trend
        String trend = detectTrend(historicalData);
        double trendStrength = calculateTrendStrength(historicalData);

        // Calculate volatility
        double volatility = calculateVolatility(historicalData);

        // Weekday patterns
        Map<DayOfWeek, TimeSeriesPattern.LoadLevel> weekdayPatterns = analyzeWeekdayPatterns(historicalData);

        // Generate description
        String description = generatePatternDescription(hasDailyPattern, hasWeeklyPattern,
                peakHours, trend, volatility);

        return TimeSeriesPattern.builder()
                .hasDailyPattern(hasDailyPattern)
                .hasWeeklyPattern(hasWeeklyPattern)
                .hasMonthlyPattern(false) // Not enough data for monthly patterns
                .peakHours(peakHours)
                .lowActivityHours(lowActivityHours)
                .weekdayPatterns(weekdayPatterns)
                .trend(trend)
                .trendStrength(trendStrength)
                .hasSeasonality(hasDailyPattern || hasWeeklyPattern)
                .seasonalityPeriodHours(hasDailyPattern ? 24 : (hasWeeklyPattern ? 168 : 0))
                .volatility(volatility)
                .description(description)
                .build();
    }

    // Private helper methods

    private boolean detectDailyPattern(List<MetricsSnapshot> data) {
        // Group by hour and calculate variance
        Map<Integer, List<Double>> hourlyData = new HashMap<>();

        for (MetricsSnapshot snapshot : data) {
            LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
            int hour = time.getHour();

            hourlyData.computeIfAbsent(hour, k -> new ArrayList<>())
                    .add(snapshot.getCpuUsagePercent());
        }

        // Check if peak hours are consistent
        double maxVariance = 0;
        for (List<Double> hourData : hourlyData.values()) {
            if (hourData.size() > 1) {
                double variance = calculateVariance(hourData);
                maxVariance = Math.max(maxVariance, variance);
            }
        }

        // If different hours have significantly different average usage, there's a daily pattern
        List<Double> hourlyAverages = hourlyData.values().stream()
                .filter(list -> !list.isEmpty())
                .map(this::calculateAverage)
                .collect(Collectors.toList());

        double overallVariance = calculateVariance(hourlyAverages);
        return overallVariance > 100.0; // Threshold for significant daily pattern
    }

    private boolean detectWeeklyPattern(List<MetricsSnapshot> data) {
        Map<DayOfWeek, List<Double>> weekdayData = new HashMap<>();

        for (MetricsSnapshot snapshot : data) {
            LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
            DayOfWeek day = time.getDayOfWeek();

            weekdayData.computeIfAbsent(day, k -> new ArrayList<>())
                    .add(snapshot.getCpuUsagePercent());
        }

        // Calculate average usage per day
        List<Double> dailyAverages = weekdayData.values().stream()
                .filter(list -> !list.isEmpty())
                .map(this::calculateAverage)
                .collect(Collectors.toList());

        if (dailyAverages.size() < 5) {
            return false; // Not enough data
        }

        double variance = calculateVariance(dailyAverages);
        return variance > 50.0; // Threshold for weekly pattern
    }

    private List<Integer> identifyPeakHours(List<MetricsSnapshot> data) {
        Map<Integer, Double> hourlyAverage = new HashMap<>();

        for (MetricsSnapshot snapshot : data) {
            LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
            int hour = time.getHour();

            hourlyAverage.merge(hour, snapshot.getCpuUsagePercent(),
                    (old, newVal) -> (old + newVal) / 2);
        }

        double overallAverage = hourlyAverage.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(50.0);

        // Peak hours are those with usage > 120% of average
        return hourlyAverage.entrySet().stream()
                .filter(e -> e.getValue() > overallAverage * 1.2)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    private List<Integer> identifyLowActivityHours(List<MetricsSnapshot> data) {
        Map<Integer, Double> hourlyAverage = new HashMap<>();

        for (MetricsSnapshot snapshot : data) {
            LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
            int hour = time.getHour();

            hourlyAverage.merge(hour, snapshot.getCpuUsagePercent(),
                    (old, newVal) -> (old + newVal) / 2);
        }

        double overallAverage = hourlyAverage.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(50.0);

        // Low activity hours are those with usage < 80% of average
        return hourlyAverage.entrySet().stream()
                .filter(e -> e.getValue() < overallAverage * 0.8)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    private String detectTrend(List<MetricsSnapshot> data) {
        if (data.size() < 10) {
            return "STABLE";
        }

        // Simple linear regression
        List<MetricsSnapshot> sortedData = data.stream()
                .sorted(Comparator.comparing(MetricsSnapshot::getTimestamp))
                .collect(Collectors.toList());

        double firstQuarterAvg = sortedData.subList(0, sortedData.size() / 4).stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average()
                .orElse(0);

        double lastQuarterAvg = sortedData.subList(sortedData.size() * 3 / 4, sortedData.size()).stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average()
                .orElse(0);

        double change = ((lastQuarterAvg - firstQuarterAvg) / firstQuarterAvg) * 100;

        if (Math.abs(change) < 10) {
            return "STABLE";
        } else if (change > 10 && change < 30) {
            return "INCREASING";
        } else if (change >= 30) {
            return "RAPIDLY_INCREASING";
        } else if (change < -10 && change > -30) {
            return "DECREASING";
        } else {
            return "RAPIDLY_DECREASING";
        }
    }

    private double calculateTrendStrength(List<MetricsSnapshot> data) {
        String trend = detectTrend(data);
        switch (trend) {
            case "RAPIDLY_INCREASING":
            case "RAPIDLY_DECREASING":
                return 0.9;
            case "INCREASING":
            case "DECREASING":
                return 0.6;
            case "STABLE":
                return 0.3;
            default:
                return 0.5;
        }
    }

    private double calculateVolatility(List<MetricsSnapshot> data) {
        List<Double> cpuValues = data.stream()
                .map(MetricsSnapshot::getCpuUsagePercent)
                .collect(Collectors.toList());

        return Math.sqrt(calculateVariance(cpuValues));
    }

    private Map<DayOfWeek, TimeSeriesPattern.LoadLevel> analyzeWeekdayPatterns(List<MetricsSnapshot> data) {
        Map<DayOfWeek, List<Double>> weekdayData = new HashMap<>();

        for (MetricsSnapshot snapshot : data) {
            LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
            DayOfWeek day = time.getDayOfWeek();

            weekdayData.computeIfAbsent(day, k -> new ArrayList<>())
                    .add(snapshot.getCpuUsagePercent());
        }

        Map<DayOfWeek, TimeSeriesPattern.LoadLevel> patterns = new HashMap<>();
        double overallAvg = data.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average()
                .orElse(50.0);

        for (Map.Entry<DayOfWeek, List<Double>> entry : weekdayData.entrySet()) {
            double dayAvg = calculateAverage(entry.getValue());
            TimeSeriesPattern.LoadLevel level = categorizeLoadLevel(dayAvg, overallAvg);
            patterns.put(entry.getKey(), level);
        }

        return patterns;
    }

    private TimeSeriesPattern.LoadLevel categorizeLoadLevel(double value, double average) {
        if (value < average * 0.5) return TimeSeriesPattern.LoadLevel.VERY_LOW;
        if (value < average * 0.8) return TimeSeriesPattern.LoadLevel.LOW;
        if (value < average * 1.2) return TimeSeriesPattern.LoadLevel.MEDIUM;
        if (value < average * 1.5) return TimeSeriesPattern.LoadLevel.HIGH;
        return TimeSeriesPattern.LoadLevel.VERY_HIGH;
    }

    private String generatePatternDescription(boolean daily, boolean weekly,
                                             List<Integer> peaks, String trend, double volatility) {
        StringBuilder desc = new StringBuilder();

        if (daily) {
            desc.append("Daily pattern detected. ");
            if (!peaks.isEmpty()) {
                desc.append("Peak hours: ").append(peaks).append(". ");
            }
        }

        if (weekly) {
            desc.append("Weekly pattern detected. ");
        }

        desc.append("Trend: ").append(trend).append(". ");

        if (volatility > 20) {
            desc.append("High volatility detected - expect significant fluctuations.");
        } else if (volatility > 10) {
            desc.append("Moderate volatility - some fluctuations expected.");
        } else {
            desc.append("Low volatility - stable usage pattern.");
        }

        return desc.toString();
    }

    private List<MetricsSnapshot> findSimilarPeriods(List<MetricsSnapshot> data,
                                                     int targetHour, DayOfWeek targetDay) {
        return data.stream()
                .filter(snapshot -> {
                    LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
                    return time.getHour() == targetHour && time.getDayOfWeek() == targetDay;
                })
                .collect(Collectors.toList());
    }

    private double applyTrendAdjustment(double value, TimeSeriesPattern pattern) {
        double adjustment = 1.0;

        switch (pattern.getTrend()) {
            case "RAPIDLY_INCREASING":
                adjustment = 1.15;
                break;
            case "INCREASING":
                adjustment = 1.05;
                break;
            case "DECREASING":
                adjustment = 0.95;
                break;
            case "RAPIDLY_DECREASING":
                adjustment = 0.85;
                break;
        }

        return value * adjustment;
    }

    private int calculateRecommendedReplicas(double cpuUsage, double memoryUsage, int currentReplicas) {
        // Target 70% CPU utilization per pod
        double targetUtilization = 70.0;

        int cpuBasedReplicas = (int) Math.ceil((cpuUsage / targetUtilization) * currentReplicas);
        int memoryBasedReplicas = (int) Math.ceil((memoryUsage / targetUtilization) * currentReplicas);

        int recommended = Math.max(cpuBasedReplicas, memoryBasedReplicas);

        // Apply bounds
        recommended = Math.max(2, Math.min(10, recommended));

        return recommended;
    }

    private double calculateConfidence(int sampleSize, TimeSeriesPattern pattern) {
        double baseConfidence = 0.5;

        // More samples = higher confidence
        if (sampleSize > 50) baseConfidence += 0.2;
        else if (sampleSize > 20) baseConfidence += 0.1;

        // Clear patterns = higher confidence
        if (pattern.getHasDailyPattern()) baseConfidence += 0.15;
        if (pattern.getHasWeeklyPattern()) baseConfidence += 0.10;

        // Low volatility = higher confidence
        if (pattern.getVolatility() < 10) baseConfidence += 0.15;
        else if (pattern.getVolatility() > 20) baseConfidence -= 0.1;

        return Math.min(0.95, Math.max(0.3, baseConfidence));
    }

    private String generatePredictionReason(int hour, DayOfWeek day, double predictedCPU,
                                           TimeSeriesPattern pattern) {
        StringBuilder reason = new StringBuilder();

        if (pattern.getPeakHours() != null && pattern.getPeakHours().contains(hour)) {
            reason.append("Peak hour detected. ");
        } else if (pattern.getLowActivityHours() != null && pattern.getLowActivityHours().contains(hour)) {
            reason.append("Low activity period. ");
        }

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            reason.append("Weekend - typically lower load. ");
        }

        if (predictedCPU > 80) {
            reason.append("High CPU usage expected - scale up recommended.");
        } else if (predictedCPU < 30) {
            reason.append("Low CPU usage expected - scale down opportunity.");
        } else {
            reason.append("Normal load expected.");
        }

        return reason.toString();
    }

    private double estimateRequestRate(double cpuUsage) {
        // Rough estimation: 1% CPU = ~10 requests/second
        return cpuUsage * 10;
    }

    private List<ScalingPrediction.ScalingEvent> detectScalingEvents(
            LocalDateTime targetTime, double predictedCPU, int recommendedReplicas, TimeSeriesPattern pattern) {

        List<ScalingPrediction.ScalingEvent> events = new ArrayList<>();

        // High load event
        if (predictedCPU > 80) {
            events.add(ScalingPrediction.ScalingEvent.builder()
                    .eventTime(targetTime)
                    .eventType("PEAK_LOAD")
                    .recommendedReplicas(recommendedReplicas)
                    .reason("High CPU usage predicted")
                    .confidence(0.8)
                    .build());
        }

        // Low load event
        if (predictedCPU < 30) {
            events.add(ScalingPrediction.ScalingEvent.builder()
                    .eventTime(targetTime)
                    .eventType("LOW_ACTIVITY")
                    .recommendedReplicas(recommendedReplicas)
                    .reason("Low CPU usage predicted - cost optimization opportunity")
                    .confidence(0.7)
                    .build());
        }

        return events;
    }

    private ScalingPrediction createDefaultPrediction(String serviceName, LocalDateTime targetTime) {
        return ScalingPrediction.builder()
                .serviceName(serviceName)
                .predictionTime(LocalDateTime.now())
                .forecastFor(targetTime)
                .predictedCPUUsage(50.0)
                .predictedMemoryUsage(50.0)
                .currentReplicas(3)
                .recommendedReplicas(3)
                .confidence(0.3)
                .reason("Insufficient historical data for accurate prediction")
                .upcomingEvents(Collections.emptyList())
                .build();
    }

    private double calculateAverage(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateVariance(List<Double> values) {
        double mean = calculateAverage(values);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return variance;
    }
}
