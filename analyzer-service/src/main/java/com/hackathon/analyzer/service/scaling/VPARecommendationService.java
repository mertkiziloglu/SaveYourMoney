package com.hackathon.analyzer.service.scaling;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.scaling.VPARecommendation;
import com.hackathon.analyzer.repository.AnalysisResultRepository;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VPARecommendationService {

    private final MetricsSnapshotRepository metricsRepository;
    private final AnalysisResultRepository analysisRepository;

    private static final double SAFETY_MARGIN = 0.15; // 15% headroom

    /**
     * Generate VPA configuration recommendations
     */
    public VPARecommendation generateVPARecommendation(String serviceName) {
        log.info("Generating VPA recommendation for: {}", serviceName);

        // Get metrics and analysis
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        List<MetricsSnapshot> historicalMetrics = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, since);

        Optional<AnalysisResult> latestAnalysis = analysisRepository
                .findFirstByServiceNameOrderByAnalysisTimestampDesc(serviceName);

        if (historicalMetrics.isEmpty()) {
            return createDefaultVPARecommendation(serviceName);
        }

        // Current resources (from hardcoded values or K8s API)
        VPARecommendation.ResourceRequests currentRequests = getCurrentRequests(serviceName);
        VPARecommendation.ResourceRequests currentLimits = getCurrentLimits(serviceName);

        // Calculate recommended resources
        VPARecommendation.ResourceRequests recommendedRequests = calculateRecommendedRequests(historicalMetrics);
        VPARecommendation.ResourceRequests recommendedLimits = calculateRecommendedLimits(recommendedRequests);

        // Determine VPA mode
        String updateMode = determineVPAMode(serviceName, historicalMetrics);

        // Generate resource policy
        VPARecommendation.ResourcePolicy policy = generateResourcePolicy(recommendedRequests, recommendedLimits);

        // Calculate savings
        double savings = calculateSavings(currentRequests, recommendedRequests);

        // Determine recommendation
        String recommendation = determineRecommendation(serviceName, historicalMetrics, savings);

        String rationale = generateVPARationale(currentRequests, recommendedRequests, savings, updateMode);

        double confidence = calculateConfidence(historicalMetrics);

        return VPARecommendation.builder()
                .serviceName(serviceName)
                .currentRequests(currentRequests)
                .currentLimits(currentLimits)
                .recommendedRequests(recommendedRequests)
                .recommendedLimits(recommendedLimits)
                .updateMode(updateMode)
                .resourcePolicy(policy)
                .rationale(rationale)
                .confidenceScore(confidence)
                .estimatedMonthlySavings(savings)
                .recommendation(recommendation)
                .build();
    }

    private VPARecommendation.ResourceRequests getCurrentRequests(String serviceName) {
        // These should ideally come from K8s API
        switch (serviceName) {
            case "cpu-hungry-service":
                return VPARecommendation.ResourceRequests.builder()
                        .cpu("100m")
                        .memory("256Mi")
                        .build();
            case "memory-leaker-service":
                return VPARecommendation.ResourceRequests.builder()
                        .cpu("200m")
                        .memory("256Mi")
                        .build();
            case "db-connection-service":
                return VPARecommendation.ResourceRequests.builder()
                        .cpu("150m")
                        .memory("512Mi")
                        .build();
            default:
                return VPARecommendation.ResourceRequests.builder()
                        .cpu("100m")
                        .memory("256Mi")
                        .build();
        }
    }

    private VPARecommendation.ResourceRequests getCurrentLimits(String serviceName) {
        VPARecommendation.ResourceRequests requests = getCurrentRequests(serviceName);
        // Limits are typically 2x requests
        int cpuMillicores = Integer.parseInt(requests.getCpu().replace("m", ""));
        int memoryMi = Integer.parseInt(requests.getMemory().replace("Mi", ""));

        return VPARecommendation.ResourceRequests.builder()
                .cpu((cpuMillicores * 2) + "m")
                .memory((memoryMi * 2) + "Mi")
                .build();
    }

    private VPARecommendation.ResourceRequests calculateRecommendedRequests(List<MetricsSnapshot> metrics) {
        // Calculate P95 CPU usage
        double p95CPU = calculateP95(metrics.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .toArray());

        // Calculate max memory usage
        long maxHeapBytes = metrics.stream()
                .mapToLong(MetricsSnapshot::getHeapUsedBytes)
                .max()
                .orElse(256 * 1024 * 1024L);

        // Convert to resources with safety margin
        int cpuMillicores = (int) Math.ceil(p95CPU * 10 * (1 + SAFETY_MARGIN)); // CPU cores to millicores
        cpuMillicores = Math.max(100, cpuMillicores); // Minimum 100m

        long memoryBytes = (long) (maxHeapBytes * (1 + SAFETY_MARGIN) * 1.3); // 30% overhead for non-heap
        int memoryMi = (int) Math.ceil(memoryBytes / (1024.0 * 1024.0));
        memoryMi = Math.max(256, memoryMi); // Minimum 256Mi

        return VPARecommendation.ResourceRequests.builder()
                .cpu(cpuMillicores + "m")
                .memory(memoryMi + "Mi")
                .build();
    }

    private VPARecommendation.ResourceRequests calculateRecommendedLimits(
            VPARecommendation.ResourceRequests requests) {

        int cpuMillicores = Integer.parseInt(requests.getCpu().replace("m", ""));
        int memoryMi = Integer.parseInt(requests.getMemory().replace("Mi", ""));

        // Limits: 1.5x for CPU, 1.5x for memory
        return VPARecommendation.ResourceRequests.builder()
                .cpu((int) (cpuMillicores * 1.5) + "m")
                .memory((int) (memoryMi * 1.5) + "Mi")
                .build();
    }

    private String determineVPAMode(String serviceName, List<MetricsSnapshot> metrics) {
        // Calculate resource variance
        double cpuVariance = calculateVariance(metrics.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .toArray());

        if (cpuVariance > 500) {
            return "Auto"; // High variance - let VPA adjust automatically
        } else if (cpuVariance > 200) {
            return "Recreate"; // Moderate variance - recreate pods with new resources
        } else {
            return "Initial"; // Stable - set initial resources only
        }
    }

    private VPARecommendation.ResourcePolicy generateResourcePolicy(
            VPARecommendation.ResourceRequests requests, VPARecommendation.ResourceRequests limits) {

        int cpuRequestMillicores = Integer.parseInt(requests.getCpu().replace("m", ""));
        int memoryRequestMi = Integer.parseInt(requests.getMemory().replace("Mi", ""));

        // Set min/max bounds (50% below to 200% above)
        VPARecommendation.ResourcePolicy.ResourceRange cpuRange =
                VPARecommendation.ResourcePolicy.ResourceRange.builder()
                        .min((cpuRequestMillicores / 2) + "m")
                        .max((cpuRequestMillicores * 2) + "m")
                        .build();

        VPARecommendation.ResourcePolicy.ResourceRange memoryRange =
                VPARecommendation.ResourcePolicy.ResourceRange.builder()
                        .min((memoryRequestMi / 2) + "Mi")
                        .max((memoryRequestMi * 2) + "Mi")
                        .build();

        return VPARecommendation.ResourcePolicy.builder()
                .cpuRange(cpuRange)
                .memoryRange(memoryRange)
                .controlledResources("RequestsAndLimits")
                .build();
    }

    private double calculateSavings(VPARecommendation.ResourceRequests current,
                                   VPARecommendation.ResourceRequests recommended) {
        int currentCPU = Integer.parseInt(current.getCpu().replace("m", ""));
        int recommendedCPU = Integer.parseInt(recommended.getCpu().replace("m", ""));

        int currentMemory = Integer.parseInt(current.getMemory().replace("Mi", ""));
        int recommendedMemory = Integer.parseInt(recommended.getMemory().replace("Mi", ""));

        // Cost calculation
        double cpuCostPerMillicore = 0.03; // $30/month per core = $0.03/milliceore
        double memoryCostPerMi = 0.005; // ~$5/GB/month

        double currentCost = (currentCPU * cpuCostPerMillicore) + (currentMemory * memoryCostPerMi);
        double recommendedCost = (recommendedCPU * cpuCostPerMillicore) + (recommendedMemory * memoryCostPerMi);

        return Math.round((currentCost - recommendedCost) * 100.0) / 100.0;
    }

    private String determineRecommendation(String serviceName, List<MetricsSnapshot> metrics, double savings) {
        double cpuVariance = calculateVariance(metrics.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .toArray());

        boolean isVariable = cpuVariance > 300;
        boolean hasSignificantSavings = Math.abs(savings) > 10;

        if (isVariable && hasSignificantSavings) {
            return "Use both HPA and VPA"; // Variable load with resource optimization opportunity
        } else if (isVariable) {
            return "Use HPA"; // Variable load - scale horizontally
        } else if (hasSignificantSavings) {
            return "Use VPA"; // Stable load with right-sizing opportunity
        } else {
            return "Manual tuning"; // Already well-configured
        }
    }

    private String generateVPARationale(VPARecommendation.ResourceRequests current,
                                       VPARecommendation.ResourceRequests recommended,
                                       double savings, String mode) {
        StringBuilder rationale = new StringBuilder();

        rationale.append(String.format("VPA mode: %s. ", mode));
        rationale.append(String.format("Current: CPU=%s, Memory=%s. ", current.getCpu(), current.getMemory()));
        rationale.append(String.format("Recommended: CPU=%s, Memory=%s. ",
                recommended.getCpu(), recommended.getMemory()));

        if (savings > 0) {
            rationale.append(String.format("Estimated savings: $%.2f/month. ", savings));
        } else if (savings < 0) {
            rationale.append(String.format("Recommended increase of $%.2f/month for better performance. ",
                    Math.abs(savings)));
        }

        rationale.append("VPA will automatically right-size pods based on actual usage.");

        return rationale.toString();
    }

    private double calculateConfidence(List<MetricsSnapshot> metrics) {
        double confidence = 0.5;

        if (metrics.size() > 1000) confidence += 0.3;
        else if (metrics.size() > 500) confidence += 0.2;
        else if (metrics.size() > 100) confidence += 0.1;

        double cpuVariance = calculateVariance(metrics.stream()
                .mapToDouble(MetricsSnapshot::getCpuUsagePercent)
                .toArray());

        if (cpuVariance < 100) confidence += 0.15; // Stable workload
        else if (cpuVariance > 500) confidence -= 0.1; // Highly variable

        return Math.max(0.3, Math.min(0.95, confidence));
    }

    private VPARecommendation createDefaultVPARecommendation(String serviceName) {
        VPARecommendation.ResourceRequests defaultRequests = VPARecommendation.ResourceRequests.builder()
                .cpu("200m")
                .memory("512Mi")
                .build();

        return VPARecommendation.builder()
                .serviceName(serviceName)
                .currentRequests(defaultRequests)
                .recommendedRequests(defaultRequests)
                .updateMode("Initial")
                .recommendation("Insufficient data for VPA recommendation")
                .rationale("Default VPA configuration")
                .confidenceScore(0.3)
                .build();
    }

    private double calculateP95(double[] values) {
        if (values.length == 0) return 50.0;

        java.util.Arrays.sort(values);
        int index = (int) Math.ceil(values.length * 0.95) - 1;
        return values[Math.max(0, Math.min(index, values.length - 1))];
    }

    private double calculateVariance(double[] values) {
        if (values.length == 0) return 0.0;

        double mean = java.util.Arrays.stream(values).average().orElse(0.0);
        double variance = java.util.Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return variance;
    }
}
