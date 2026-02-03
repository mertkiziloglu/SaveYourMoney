package com.hackathon.analyzer.service.scaling;

import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.scaling.CostAwareScaling;
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
public class CostAwareScalingService {

    private final MetricsSnapshotRepository metricsRepository;

    private static final double COST_PER_CPU_CORE_HOUR = 0.042; // AWS m5.large ~$0.096/hr / 2 cores
    private static final double COST_PER_GB_MEMORY_HOUR = 0.0052;
    private static final int HOURS_PER_MONTH = 730;

    /**
     * Generate cost-aware scaling recommendations
     */
    public CostAwareScaling analyzeCostAwareScaling(String serviceName) {
        log.info("Generating cost-aware scaling analysis for: {}", serviceName);

        // Get historical metrics
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        List<MetricsSnapshot> historicalData = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, since);

        if (historicalData.isEmpty()) {
            return createDefaultCostAnalysis(serviceName);
        }

        // Current state
        int currentReplicas = 3; // Should come from K8s API
        double currentCPU = 1.0; // 1 core per pod
        double currentMemory = 2.0; // 2GB per pod

        double currentMonthlyCost = calculateMonthlyCost(currentReplicas, currentCPU, currentMemory);
        double currentPerformanceScore = calculatePerformanceScore(historicalData);

        // Generate scaling options
        CostAwareScaling.ScalingOption performanceOpt = generatePerformanceOptimized(
                serviceName, historicalData, currentCPU, currentMemory);

        CostAwareScaling.ScalingOption costOpt = generateCostOptimized(
                serviceName, historicalData, currentCPU, currentMemory);

        CostAwareScaling.ScalingOption balanced = generateBalanced(
                serviceName, historicalData, currentCPU, currentMemory);

        // Analyze idle time
        CostAwareScaling.IdleTimeAnalysis idleAnalysis = analyzeIdleTime(historicalData);

        // Determine recommended option
        String recommendedOption = determineRecommendedOption(performanceOpt, costOpt, balanced);
        String rationale = generateRationale(recommendedOption, performanceOpt, costOpt, balanced);

        return CostAwareScaling.builder()
                .serviceName(serviceName)
                .currentReplicas(currentReplicas)
                .currentMonthlyCost(currentMonthlyCost)
                .currentPerformanceScore(currentPerformanceScore)
                .performanceOptimized(performanceOpt)
                .costOptimized(costOpt)
                .balanced(balanced)
                .recommendedOption(recommendedOption)
                .rationale(rationale)
                .idleTimeAnalysis(idleAnalysis)
                .build();
    }

    private CostAwareScaling.ScalingOption generatePerformanceOptimized(
            String serviceName, List<MetricsSnapshot> data, double cpuPerPod, double memoryPerPod) {

        // For performance: target 60% utilization, allow more replicas
        double avgCPU = calculateAverageCPU(data);
        double p95CPU = calculateP95CPU(data);

        int minReplicas = 3;
        int maxReplicas = 10;
        int avgReplicas = calculateReplicasForUtilization(p95CPU, 60.0, 3);

        // Slightly larger pods for better performance
        String cpuRequest = ((int) Math.ceil(cpuPerPod * 1.2 * 1000)) + "m";
        String memoryRequest = ((int) Math.ceil(memoryPerPod * 1.2 * 1024)) + "Mi";

        double monthlyCost = calculateMonthlyCost(avgReplicas, cpuPerPod * 1.2, memoryPerPod * 1.2);
        double savingsPercentage = 0.0; // Base case - no savings, optimized for performance

        List<String> pros = Arrays.asList(
                "Best response times and reliability",
                "Handles traffic spikes gracefully",
                "Low risk of performance degradation"
        );

        List<String> cons = Arrays.asList(
                "Higher operational costs",
                "Some resource over-provisioning",
                "More pods to manage"
        );

        return CostAwareScaling.ScalingOption.builder()
                .strategy("Performance Optimized")
                .minReplicas(minReplicas)
                .maxReplicas(maxReplicas)
                .averageReplicas(avgReplicas)
                .cpuRequest(cpuRequest)
                .memoryRequest(memoryRequest)
                .monthlyCost(Math.round(monthlyCost * 100.0) / 100.0)
                .savingsPercentage(savingsPercentage)
                .savingsAmount(0.0)
                .expectedP95ResponseTime(100.0)
                .expectedP99ResponseTime(200.0)
                .performanceScore(95.0)
                .pros(pros)
                .cons(cons)
                .description("Optimized for best performance with minimal response time and high reliability")
                .build();
    }

    private CostAwareScaling.ScalingOption generateCostOptimized(
            String serviceName, List<MetricsSnapshot> data, double cpuPerPod, double memoryPerPod) {

        // For cost: target 80% utilization, fewer replicas
        double avgCPU = calculateAverageCPU(data);
        double p95CPU = calculateP95CPU(data);

        int minReplicas = 2;
        int maxReplicas = 6;
        int avgReplicas = calculateReplicasForUtilization(avgCPU, 80.0, 2);

        // Right-sized pods based on actual usage
        String cpuRequest = ((int) Math.ceil(cpuPerPod * 0.8 * 1000)) + "m";
        String memoryRequest = ((int) Math.ceil(memoryPerPod * 0.8 * 1024)) + "Mi";

        double monthlyCost = calculateMonthlyCost(avgReplicas, cpuPerPod * 0.8, memoryPerPod * 0.8);
        double baseCost = calculateMonthlyCost(3, cpuPerPod, memoryPerPod);
        double savingsAmount = baseCost - monthlyCost;
        double savingsPercentage = (savingsAmount / baseCost) * 100;

        List<String> pros = Arrays.asList(
                "40-50% cost reduction",
                "Right-sized resources",
                "Optimal resource utilization"
        );

        List<String> cons = Arrays.asList(
                "Higher CPU/memory utilization",
                "Less headroom for spikes",
                "May need manual intervention during peaks"
        );

        return CostAwareScaling.ScalingOption.builder()
                .strategy("Cost Optimized")
                .minReplicas(minReplicas)
                .maxReplicas(maxReplicas)
                .averageReplicas(avgReplicas)
                .cpuRequest(cpuRequest)
                .memoryRequest(memoryRequest)
                .monthlyCost(Math.round(monthlyCost * 100.0) / 100.0)
                .savingsPercentage(Math.round(savingsPercentage * 100.0) / 100.0)
                .savingsAmount(Math.round(savingsAmount * 100.0) / 100.0)
                .expectedP95ResponseTime(150.0)
                .expectedP99ResponseTime(300.0)
                .performanceScore(75.0)
                .pros(pros)
                .cons(cons)
                .description("Optimized for cost savings with acceptable performance trade-offs")
                .build();
    }

    private CostAwareScaling.ScalingOption generateBalanced(
            String serviceName, List<MetricsSnapshot> data, double cpuPerPod, double memoryPerPod) {

        // Balanced: target 70% utilization
        double avgCPU = calculateAverageCPU(data);
        double p95CPU = calculateP95CPU(data);

        int minReplicas = 2;
        int maxReplicas = 8;
        int avgReplicas = calculateReplicasForUtilization(p95CPU, 70.0, 2);

        String cpuRequest = ((int) Math.ceil(cpuPerPod * 1000)) + "m";
        String memoryRequest = ((int) Math.ceil(memoryPerPod * 1024)) + "Mi";

        double monthlyCost = calculateMonthlyCost(avgReplicas, cpuPerPod, memoryPerPod);
        double baseCost = calculateMonthlyCost(3, cpuPerPod, memoryPerPod);
        double savingsAmount = baseCost - monthlyCost;
        double savingsPercentage = (savingsAmount / baseCost) * 100;

        List<String> pros = Arrays.asList(
                "Good performance-cost balance",
                "20-30% cost reduction",
                "Reasonable headroom for spikes",
                "Recommended for most workloads"
        );

        List<String> cons = Arrays.asList(
                "Not the cheapest option",
                "Not the fastest option"
        );

        return CostAwareScaling.ScalingOption.builder()
                .strategy("Balanced")
                .minReplicas(minReplicas)
                .maxReplicas(maxReplicas)
                .averageReplicas(avgReplicas)
                .cpuRequest(cpuRequest)
                .memoryRequest(memoryRequest)
                .monthlyCost(Math.round(monthlyCost * 100.0) / 100.0)
                .savingsPercentage(Math.round(savingsPercentage * 100.0) / 100.0)
                .savingsAmount(Math.round(savingsAmount * 100.0) / 100.0)
                .expectedP95ResponseTime(120.0)
                .expectedP99ResponseTime(250.0)
                .performanceScore(85.0)
                .pros(pros)
                .cons(cons)
                .description("Best balance between cost savings and performance - recommended for production")
                .build();
    }

    private CostAwareScaling.IdleTimeAnalysis analyzeIdleTime(List<MetricsSnapshot> data) {
        Map<Integer, List<Double>> hourlyUsage = new HashMap<>();

        for (MetricsSnapshot snapshot : data) {
            LocalDateTime time = LocalDateTime.ofInstant(snapshot.getTimestamp(), ZoneId.systemDefault());
            int hour = time.getHour();

            hourlyUsage.computeIfAbsent(hour, k -> new ArrayList<>())
                    .add(snapshot.getCpuUsagePercent());
        }

        double overallAverage = data.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average()
                .orElse(50.0);

        // Find idle periods (usage < 30%)
        List<CostAwareScaling.IdleTimeAnalysis.IdlePeriod> idlePeriods = new ArrayList<>();
        int idleHours = 0;

        for (Map.Entry<Integer, List<Double>> entry : hourlyUsage.entrySet()) {
            double avgUsage = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            if (avgUsage < 30.0) {
                idleHours++;
                idlePeriods.add(CostAwareScaling.IdleTimeAnalysis.IdlePeriod.builder()
                        .hourOfDay(entry.getKey())
                        .averageUsage(Math.round(avgUsage * 100.0) / 100.0)
                        .recommendedReplicas(1) // Minimum replicas during idle
                        .build());
            }
        }

        double idlePercentage = (idleHours / 24.0) * 100;
        double potentialSavings = (idlePercentage / 100) * calculateMonthlyCost(2, 1.0, 2.0);

        String recommendation;
        if (idlePercentage > 40) {
            recommendation = "High idle time detected. Consider scheduled scaling or serverless architecture.";
        } else if (idlePercentage > 20) {
            recommendation = "Moderate idle time. Implement time-based HPA for cost savings.";
        } else {
            recommendation = "Low idle time. Current scaling approach is appropriate.";
        }

        return CostAwareScaling.IdleTimeAnalysis.builder()
                .idlePercentage(Math.round(idlePercentage * 100.0) / 100.0)
                .idlePeriods(idlePeriods)
                .potentialSavings(Math.round(potentialSavings * 100.0) / 100.0)
                .recommendation(recommendation)
                .build();
    }

    private String determineRecommendedOption(CostAwareScaling.ScalingOption perf,
                                             CostAwareScaling.ScalingOption cost,
                                             CostAwareScaling.ScalingOption balanced) {
        // Balanced is recommended for most cases
        if (balanced.getSavingsPercentage() > 15 && balanced.getPerformanceScore() > 80) {
            return "BALANCED";
        }

        // If performance is critical and cost difference is small
        if (perf.getMonthlyCost() - balanced.getMonthlyCost() < 50) {
            return "PERFORMANCE";
        }

        // If cost savings are significant
        if (cost.getSavingsPercentage() > 40) {
            return "COST";
        }

        return "BALANCED";
    }

    private String generateRationale(String recommended, CostAwareScaling.ScalingOption perf,
                                    CostAwareScaling.ScalingOption cost,
                                    CostAwareScaling.ScalingOption balanced) {
        switch (recommended) {
            case "PERFORMANCE":
                return String.format("Performance-optimized approach recommended. " +
                        "Provides %.0f performance score with minimal cost difference ($%.2f/month more than balanced).",
                        perf.getPerformanceScore(),
                        perf.getMonthlyCost() - balanced.getMonthlyCost());

            case "COST":
                return String.format("Cost-optimized approach recommended. " +
                        "Saves $%.2f/month (%.1f%% reduction) with acceptable performance trade-offs.",
                        cost.getSavingsAmount(),
                        cost.getSavingsPercentage());

            case "BALANCED":
            default:
                return String.format("Balanced approach recommended. " +
                        "Provides %.0f performance score while saving $%.2f/month (%.1f%% reduction).",
                        balanced.getPerformanceScore(),
                        balanced.getSavingsAmount(),
                        balanced.getSavingsPercentage());
        }
    }

    private double calculateMonthlyCost(int replicas, double cpuPerPod, double memoryPerPod) {
        double cpuCostPerPod = cpuPerPod * COST_PER_CPU_CORE_HOUR * HOURS_PER_MONTH;
        double memoryCostPerPod = memoryPerPod * COST_PER_GB_MEMORY_HOUR * HOURS_PER_MONTH;
        return replicas * (cpuCostPerPod + memoryCostPerPod);
    }

    private double calculateAverageCPU(List<MetricsSnapshot> data) {
        return data.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .average()
                .orElse(50.0);
    }

    private double calculateP95CPU(List<MetricsSnapshot> data) {
        List<Double> sorted = data.stream()
                .map(MetricsSnapshot::getCpuUsagePercent)
                .sorted()
                .collect(Collectors.toList());

        if (sorted.isEmpty()) return 50.0;

        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.max(0, index));
    }

    private int calculateReplicasForUtilization(double actualUsage, double targetUtilization, int minReplicas) {
        int replicas = (int) Math.ceil(actualUsage / targetUtilization);
        return Math.max(minReplicas, replicas);
    }

    private double calculatePerformanceScore(List<MetricsSnapshot> data) {
        double avgCPU = calculateAverageCPU(data);
        double p95CPU = calculateP95CPU(data);

        // Lower utilization = better performance score
        double score = 100 - (avgCPU * 0.5) - (p95CPU * 0.3);
        return Math.max(0, Math.min(100, score));
    }

    private CostAwareScaling createDefaultCostAnalysis(String serviceName) {
        return CostAwareScaling.builder()
                .serviceName(serviceName)
                .currentReplicas(3)
                .currentMonthlyCost(300.0)
                .recommendedOption("BALANCED")
                .rationale("Insufficient data for detailed analysis")
                .build();
    }
}
