package com.hackathon.analyzer.service;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.ResourceRecommendation;
import com.hackathon.analyzer.repository.AnalysisResultRepository;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceAnalyzerService {

    private final MetricsSnapshotRepository metricsRepository;
    private final AnalysisResultRepository analysisRepository;

    private static final double SAFETY_MARGIN = 0.20; // 20% safety margin
    private static final double COST_PER_CPU_CORE_MONTH = 30.0; // $30 per vCPU
    private static final double COST_PER_GB_MEMORY_MONTH = 5.0; // $5 per GB

    /**
     * Analyze a service and generate recommendations
     */
    public ResourceRecommendation analyzeService(String serviceName) {
        log.info("Starting analysis for service: {}", serviceName);

        // Fetch recent metrics (last 5 minutes)
        Instant since = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<MetricsSnapshot> snapshots = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, since);

        if (snapshots.isEmpty()) {
            log.warn("No metrics found for service: {}", serviceName);
            return createDefaultRecommendation(serviceName);
        }

        log.info("Analyzing {} metrics snapshots for {}", snapshots.size(), serviceName);

        // Perform analysis
        AnalysisResult analysis = performAnalysis(serviceName, snapshots);
        analysisRepository.save(analysis);

        // Generate recommendations
        ResourceRecommendation recommendation = generateRecommendations(serviceName, analysis);

        log.info("Analysis completed for {}. Confidence: {}%",
                serviceName, recommendation.getConfidenceScore());

        return recommendation;
    }

    /**
     * Perform statistical analysis on metrics
     */
    private AnalysisResult performAnalysis(String serviceName, List<MetricsSnapshot> snapshots) {
        DescriptiveStatistics cpuStats = new DescriptiveStatistics();
        DescriptiveStatistics memoryStats = new DescriptiveStatistics();
        DescriptiveStatistics connectionStats = new DescriptiveStatistics();

        for (MetricsSnapshot snapshot : snapshots) {
            cpuStats.addValue(snapshot.getCpuUsagePercent());
            memoryStats.addValue(snapshot.getHeapUsagePercent());

            if (snapshot.getHikariActiveConnections() != null) {
                connectionStats.addValue(snapshot.getHikariActiveConnections());
            }
        }

        // Detect issues
        boolean cpuThrottling = detectCpuThrottling(cpuStats);
        boolean memoryLeak = detectMemoryLeak(snapshots);
        boolean poolExhaustion = detectConnectionPoolExhaustion(snapshots);

        return AnalysisResult.builder()
                .serviceName(serviceName)
                .analysisTimestamp(Instant.now())
                // Current config (suboptimal - hardcoded for demo)
                .currentCpuRequest(getCurrentCpuRequest(serviceName))
                .currentCpuLimit(getCurrentCpuLimit(serviceName))
                .currentMemoryRequest(getCurrentMemoryRequest(serviceName))
                .currentMemoryLimit(getCurrentMemoryLimit(serviceName))
                // Recommended config (calculated)
                .recommendedCpuRequest(calculateCpuRequest(cpuStats))
                .recommendedCpuLimit(calculateCpuLimit(cpuStats))
                .recommendedMemoryRequest(calculateMemoryRequest(memoryStats, snapshots))
                .recommendedMemoryLimit(calculateMemoryLimit(memoryStats, snapshots))
                // JVM recommendations
                .recommendedJvmXms(calculateJvmXms(memoryStats, snapshots))
                .recommendedJvmXmx(calculateJvmXmx(memoryStats, snapshots))
                // Connection pool
                .recommendedMaxPoolSize(calculateMaxPoolSize(connectionStats))
                .recommendedMinIdle(calculateMinIdle(connectionStats))
                // Statistics
                .p95CpuUsage(cpuStats.getPercentile(95))
                .p99CpuUsage(cpuStats.getPercentile(99))
                .maxCpuUsage(cpuStats.getMax())
                .p95MemoryUsage(memoryStats.getPercentile(95))
                .p99MemoryUsage(memoryStats.getPercentile(99))
                .maxMemoryUsage(memoryStats.getMax())
                // Issues
                .cpuThrottlingDetected(cpuThrottling)
                .memoryLeakDetected(memoryLeak)
                .connectionPoolExhaustion(poolExhaustion)
                // Cost savings
                .estimatedMonthlySavings(calculateSavings(serviceName, cpuStats, memoryStats))
                .confidenceScore(calculateConfidence(snapshots.size(), cpuStats, memoryStats))
                .build();
    }

    /**
     * Generate recommendations based on analysis
     */
    private ResourceRecommendation generateRecommendations(String serviceName, AnalysisResult analysis) {
        Map<String, String> detectedIssues = new HashMap<>();

        if (analysis.getCpuThrottlingDetected()) {
            detectedIssues.put("CPU Throttling", "CPU usage exceeds limits causing performance degradation");
        }
        if (analysis.getMemoryLeakDetected()) {
            detectedIssues.put("Memory Leak", "Memory usage shows continuous growth pattern");
        }
        if (analysis.getConnectionPoolExhaustion()) {
            detectedIssues.put("Connection Pool Exhaustion", "Connection pool frequently at maximum capacity");
        }

        String rationale = buildRationale(analysis);

        return ResourceRecommendation.builder()
                .serviceName(serviceName)
                .kubernetes(ResourceRecommendation.KubernetesResources.builder()
                        .cpuRequest(analysis.getRecommendedCpuRequest())
                        .cpuLimit(analysis.getRecommendedCpuLimit())
                        .memoryRequest(analysis.getRecommendedMemoryRequest())
                        .memoryLimit(analysis.getRecommendedMemoryLimit())
                        .build())
                .jvm(ResourceRecommendation.JvmConfiguration.builder()
                        .xms(analysis.getRecommendedJvmXms())
                        .xmx(analysis.getRecommendedJvmXmx())
                        .gcType("G1GC")
                        .additionalFlags(Map.of(
                                "XX:+UseG1GC", "",
                                "XX:MaxGCPauseMillis", "200",
                                "XX:+HeapDumpOnOutOfMemoryError", ""
                        ))
                        .build())
                .connectionPool(ResourceRecommendation.ConnectionPoolConfig.builder()
                        .maximumPoolSize(analysis.getRecommendedMaxPoolSize())
                        .minimumIdle(analysis.getRecommendedMinIdle())
                        .connectionTimeout(30000L)
                        .idleTimeout(600000L)
                        .build())
                .threadPool(ResourceRecommendation.ThreadPoolConfig.builder()
                        .maxThreads(200)
                        .minSpareThreads(25)
                        .build())
                .costAnalysis(calculateCostAnalysis(analysis))
                .confidenceScore(analysis.getConfidenceScore())
                .rationale(rationale)
                .detectedIssues(detectedIssues)
                .build();
    }

    /**
     * Detect CPU throttling
     */
    private boolean detectCpuThrottling(DescriptiveStatistics cpuStats) {
        // If P95 > 80% or max > 90%, likely throttling
        return cpuStats.getPercentile(95) > 80.0 || cpuStats.getMax() > 90.0;
    }

    /**
     * Detect memory leak (continuous growth)
     */
    private boolean detectMemoryLeak(List<MetricsSnapshot> snapshots) {
        if (snapshots.size() < 10) return false;

        // Check if memory usage is continuously increasing
        List<Double> memoryUsages = snapshots.stream()
                .map(MetricsSnapshot::getHeapUsagePercent)
                .collect(Collectors.toList());

        // Simple trend detection: compare first half avg with second half avg
        int midpoint = memoryUsages.size() / 2;
        double firstHalfAvg = memoryUsages.subList(0, midpoint).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalfAvg = memoryUsages.subList(midpoint, memoryUsages.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        // If second half is significantly higher, it's a leak
        return secondHalfAvg > firstHalfAvg * 1.2; // 20% increase
    }

    /**
     * Detect connection pool exhaustion
     */
    private boolean detectConnectionPoolExhaustion(List<MetricsSnapshot> snapshots) {
        long exhaustedCount = snapshots.stream()
                .filter(s -> s.getHikariActiveConnections() != null && s.getHikariMaxConnections() != null)
                .filter(s -> s.getHikariActiveConnections().equals(s.getHikariMaxConnections()))
                .count();

        // If >50% of samples show maxed out pool, it's exhaustion
        return exhaustedCount > snapshots.size() * 0.5;
    }

    /**
     * Calculate CPU request based on P95 + safety margin
     */
    private String calculateCpuRequest(DescriptiveStatistics cpuStats) {
        double p95Percent = cpuStats.getPercentile(95);
        double cpuCores = (p95Percent / 100.0) * (1 + SAFETY_MARGIN);

        // Convert to millicores
        int millicores = (int) Math.ceil(cpuCores * 1000);
        millicores = Math.max(100, millicores); // Minimum 100m

        return millicores + "m";
    }

    /**
     * Calculate CPU limit (2x request for burst capacity)
     */
    private String calculateCpuLimit(DescriptiveStatistics cpuStats) {
        String request = calculateCpuRequest(cpuStats);
        int requestMillicores = Integer.parseInt(request.replace("m", ""));
        int limitMillicores = requestMillicores * 2;

        return limitMillicores + "m";
    }

    /**
     * Calculate memory request based on P95 heap usage
     */
    private String calculateMemoryRequest(DescriptiveStatistics memoryStats, List<MetricsSnapshot> snapshots) {
        // Get max heap used in bytes
        long maxHeapBytes = snapshots.stream()
                .mapToLong(MetricsSnapshot::getHeapUsedBytes)
                .max()
                .orElse(256 * 1024 * 1024L);

        // Add safety margin
        long recommendedBytes = (long) (maxHeapBytes * (1 + SAFETY_MARGIN));

        // Convert to Mi
        long recommendedMi = recommendedBytes / (1024 * 1024);
        recommendedMi = Math.max(256, recommendedMi); // Minimum 256Mi

        return recommendedMi + "Mi";
    }

    /**
     * Calculate memory limit (1.5x request)
     */
    private String calculateMemoryLimit(DescriptiveStatistics memoryStats, List<MetricsSnapshot> snapshots) {
        String request = calculateMemoryRequest(memoryStats, snapshots);
        long requestMi = Long.parseLong(request.replace("Mi", ""));
        long limitMi = (long) (requestMi * 1.5);

        return limitMi + "Mi";
    }

    /**
     * Calculate JVM Xms (initial heap)
     */
    private String calculateJvmXms(DescriptiveStatistics memoryStats, List<MetricsSnapshot> snapshots) {
        String memoryRequest = calculateMemoryRequest(memoryStats, snapshots);
        long requestMi = Long.parseLong(memoryRequest.replace("Mi", ""));

        // Xms = 75% of memory request
        long xmsMi = (long) (requestMi * 0.75);

        return xmsMi + "m";
    }

    /**
     * Calculate JVM Xmx (max heap)
     */
    private String calculateJvmXmx(DescriptiveStatistics memoryStats, List<MetricsSnapshot> snapshots) {
        String memoryRequest = calculateMemoryRequest(memoryStats, snapshots);
        long requestMi = Long.parseLong(memoryRequest.replace("Mi", ""));

        // Xmx = 85% of memory request (leave space for non-heap)
        long xmxMi = (long) (requestMi * 0.85);

        return xmxMi + "m";
    }

    /**
     * Calculate connection pool max size
     */
    private Integer calculateMaxPoolSize(DescriptiveStatistics connectionStats) {
        if (connectionStats.getN() == 0) {
            return null; // Not a DB service
        }

        double p95Active = connectionStats.getPercentile(95);
        int recommended = (int) Math.ceil(p95Active * (1 + SAFETY_MARGIN));

        return Math.max(10, recommended); // Minimum 10
    }

    /**
     * Calculate connection pool min idle
     */
    private Integer calculateMinIdle(DescriptiveStatistics connectionStats) {
        if (connectionStats.getN() == 0) {
            return null;
        }

        Integer maxPool = calculateMaxPoolSize(connectionStats);
        if (maxPool == null) return null;

        return Math.max(5, maxPool / 5); // 20% of max
    }

    /**
     * Calculate cost analysis
     */
    private ResourceRecommendation.CostAnalysis calculateCostAnalysis(AnalysisResult analysis) {
        // Current costs
        double currentCpuCores = parseCpuToCore(analysis.getCurrentCpuRequest());
        double currentMemoryGb = parseMemoryToGb(analysis.getCurrentMemoryRequest());
        double currentMonthlyCost = (currentCpuCores * COST_PER_CPU_CORE_MONTH) +
                (currentMemoryGb * COST_PER_GB_MEMORY_MONTH);

        // Recommended costs
        double recommendedCpuCores = parseCpuToCore(analysis.getRecommendedCpuRequest());
        double recommendedMemoryGb = parseMemoryToGb(analysis.getRecommendedMemoryRequest());
        double recommendedMonthlyCost = (recommendedCpuCores * COST_PER_CPU_CORE_MONTH) +
                (recommendedMemoryGb * COST_PER_GB_MEMORY_MONTH);

        double monthlySavings = currentMonthlyCost - recommendedMonthlyCost;
        double annualSavings = monthlySavings * 12;
        int savingsPercentage = (int) ((monthlySavings / currentMonthlyCost) * 100);

        return ResourceRecommendation.CostAnalysis.builder()
                .currentMonthlyCost(Math.round(currentMonthlyCost * 100.0) / 100.0)
                .recommendedMonthlyCost(Math.round(recommendedMonthlyCost * 100.0) / 100.0)
                .monthlySavings(Math.round(monthlySavings * 100.0) / 100.0)
                .annualSavings(Math.round(annualSavings * 100.0) / 100.0)
                .savingsPercentage(savingsPercentage)
                .build();
    }

    /**
     * Calculate estimated monthly savings
     */
    private Double calculateSavings(String serviceName, DescriptiveStatistics cpuStats, DescriptiveStatistics memoryStats) {
        // Estimate current resource allocation (conservative estimates)
        double currentCpuCores = 1.0; // Assume 1 vCPU default
        double currentMemoryGb = 2.0; // Assume 2GB default

        // Calculate recommended resources based on P95 usage with safety margin
        double p95Cpu = cpuStats.getPercentile(95);
        double p95Memory = memoryStats.getPercentile(95);

        double recommendedCpuCores = Math.ceil((p95Cpu / 100.0) * (1 + SAFETY_MARGIN) * 10) / 10.0;
        double recommendedMemoryGb = Math.ceil((p95Memory / 100.0) * (1 + SAFETY_MARGIN) * 10) / 10.0;

        // Calculate costs
        double currentMonthlyCost = (currentCpuCores * COST_PER_CPU_CORE_MONTH) +
                                   (currentMemoryGb * COST_PER_GB_MEMORY_MONTH);
        double recommendedMonthlyCost = (recommendedCpuCores * COST_PER_CPU_CORE_MONTH) +
                                       (recommendedMemoryGb * COST_PER_GB_MEMORY_MONTH);

        double savings = currentMonthlyCost - recommendedMonthlyCost;
        return Math.max(0.0, Math.round(savings * 100.0) / 100.0); // Round to 2 decimals, ensure non-negative
    }

    /**
     * Calculate confidence score based on data quality
     */
    private Double calculateConfidence(int sampleCount, DescriptiveStatistics cpuStats, DescriptiveStatistics memoryStats) {
        double baseConfidence = 0.5;

        // More samples = higher confidence
        if (sampleCount > 50) baseConfidence += 0.2;
        else if (sampleCount > 20) baseConfidence += 0.1;

        // Low variance = higher confidence
        if (cpuStats.getStandardDeviation() < 10) baseConfidence += 0.15;
        if (memoryStats.getStandardDeviation() < 10) baseConfidence += 0.15;

        return Math.min(0.95, baseConfidence);
    }

    /**
     * Build rationale text
     */
    private String buildRationale(AnalysisResult analysis) {
        StringBuilder rationale = new StringBuilder();

        rationale.append(String.format("Analysis based on %s metrics. ",
                "recent"));

        rationale.append(String.format("CPU P95: %.1f%%, recommending %s (current: %s). ",
                analysis.getP95CpuUsage(),
                analysis.getRecommendedCpuRequest(),
                analysis.getCurrentCpuRequest()));

        rationale.append(String.format("Memory P95: %.1f%%, recommending %s (current: %s). ",
                analysis.getP95MemoryUsage(),
                analysis.getRecommendedMemoryRequest(),
                analysis.getCurrentMemoryRequest()));

        if (analysis.getCpuThrottlingDetected()) {
            rationale.append("CPU throttling detected. ");
        }
        if (analysis.getMemoryLeakDetected()) {
            rationale.append("Potential memory leak detected. ");
        }
        if (analysis.getConnectionPoolExhaustion()) {
            rationale.append("Connection pool exhaustion detected. ");
        }

        return rationale.toString();
    }

    /**
     * Get current configurations (hardcoded for demo services)
     */
    private String getCurrentCpuRequest(String serviceName) {
        return switch (serviceName) {
            case "cpu-hungry-service" -> "100m";
            case "memory-leaker-service" -> "200m";
            case "db-connection-service" -> "150m";
            default -> "100m";
        };
    }

    private String getCurrentCpuLimit(String serviceName) {
        return switch (serviceName) {
            case "cpu-hungry-service" -> "200m";
            case "memory-leaker-service" -> "400m";
            case "db-connection-service" -> "300m";
            default -> "200m";
        };
    }

    private String getCurrentMemoryRequest(String serviceName) {
        return switch (serviceName) {
            case "cpu-hungry-service" -> "256Mi";
            case "memory-leaker-service" -> "256Mi";
            case "db-connection-service" -> "512Mi";
            default -> "256Mi";
        };
    }

    private String getCurrentMemoryLimit(String serviceName) {
        return switch (serviceName) {
            case "cpu-hungry-service" -> "512Mi";
            case "memory-leaker-service" -> "512Mi";
            case "db-connection-service" -> "1Gi";
            default -> "512Mi";
        };
    }

    /**
     * Parse CPU string to cores
     */
    private double parseCpuToCore(String cpu) {
        if (cpu.endsWith("m")) {
            return Double.parseDouble(cpu.replace("m", "")) / 1000.0;
        }
        return Double.parseDouble(cpu);
    }

    /**
     * Parse memory string to GB
     */
    private double parseMemoryToGb(String memory) {
        if (memory.endsWith("Mi")) {
            return Double.parseDouble(memory.replace("Mi", "")) / 1024.0;
        } else if (memory.endsWith("Gi")) {
            return Double.parseDouble(memory.replace("Gi", ""));
        }
        return Double.parseDouble(memory) / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * Create default recommendation when no metrics available
     */
    private ResourceRecommendation createDefaultRecommendation(String serviceName) {
        return ResourceRecommendation.builder()
                .serviceName(serviceName)
                .confidenceScore(0.0)
                .rationale("No metrics available for analysis")
                .detectedIssues(Map.of("No Data", "Service metrics not found"))
                .build();
    }

    /**
     * Get all analysis results for a service
     */
    public List<AnalysisResult> getAnalysisHistory(String serviceName) {
        return analysisRepository.findByServiceNameOrderByAnalysisTimestampDesc(serviceName);
    }

    /**
     * Get latest analysis for a service
     */
    public Optional<AnalysisResult> getLatestAnalysis(String serviceName) {
        return analysisRepository.findFirstByServiceNameOrderByAnalysisTimestampDesc(serviceName);
    }
}
