package com.hackathon.analyzer.service.scaling;

import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.model.scaling.CustomMetricsAnalysis;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomMetricsAnalysisService {

    private final MetricsSnapshotRepository metricsRepository;

    /**
     * Analyze custom metrics for scaling decisions
     */
    public CustomMetricsAnalysis analyzeCustomMetrics(String serviceName) {
        log.info("Analyzing custom metrics for: {}", serviceName);

        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        List<MetricsSnapshot> metrics = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, since);

        List<CustomMetricsAnalysis.CustomMetricScaling> recommendations = new ArrayList<>();

        if (!metrics.isEmpty()) {
            // Analyze HTTP request rate
            recommendations.add(analyzeRequestRate(metrics));

            // Analyze connection pool usage
            recommendations.add(analyzeConnectionPool(metrics));

            // Analyze response time
            recommendations.add(analyzeResponseTime(metrics));
        }

        return CustomMetricsAnalysis.builder()
                .serviceName(serviceName)
                .recommendations(recommendations)
                .build();
    }

    private CustomMetricsAnalysis.CustomMetricScaling analyzeRequestRate(List<MetricsSnapshot> metrics) {
        // Calculate requests per second
        double totalRequests = metrics.stream()
                .mapToDouble(m -> m.getHttpRequestCount() != null ? m.getHttpRequestCount() : 0)
                .sum();

        double durationMinutes = metrics.size() / 6.0; // Assuming 10-second intervals
        double requestsPerSecond = (totalRequests / (durationMinutes * 60));

        int currentReplicas = 3;
        double currentPerPodRPS = requestsPerSecond / currentReplicas;

        // Target: 1000 RPS per pod
        double targetPerPodRPS = 1000.0;
        int recommendedReplicas = (int) Math.ceil(requestsPerSecond / targetPerPodRPS);
        recommendedReplicas = Math.max(2, Math.min(10, recommendedReplicas));

        String rationale = String.format(
                "Current: %.1f RPS total, %.1f RPS per pod. Target: %.1f RPS per pod.",
                requestsPerSecond, currentPerPodRPS, targetPerPodRPS);

        String recommendation;
        if (currentPerPodRPS > targetPerPodRPS * 1.2) {
            recommendation = "Scale up - request rate exceeds target";
        } else if (currentPerPodRPS < targetPerPodRPS * 0.5) {
            recommendation = "Scale down - request rate well below target";
        } else {
            recommendation = "Current scaling is appropriate";
        }

        return CustomMetricsAnalysis.CustomMetricScaling.builder()
                .metricName("http_requests_per_second")
                .type(CustomMetricsAnalysis.CustomMetricScaling.MetricType.REQUESTS_PER_SECOND)
                .currentValue(requestsPerSecond)
                .currentPerPodValue(currentPerPodRPS)
                .targetValue(requestsPerSecond) // Total target
                .targetPerPodValue(targetPerPodRPS)
                .currentReplicas(currentReplicas)
                .recommendedReplicas(recommendedReplicas)
                .scaleUpThreshold(targetPerPodRPS * 1.2)
                .scaleDownThreshold(targetPerPodRPS * 0.5)
                .rationale(rationale)
                .recommendation(recommendation)
                .build();
    }

    private CustomMetricsAnalysis.CustomMetricScaling analyzeConnectionPool(List<MetricsSnapshot> metrics) {
        // Calculate average active connections
        double avgActiveConnections = metrics.stream()
                .filter(m -> m.getHikariActiveConnections() != null)
                .mapToDouble(MetricsSnapshot::getHikariActiveConnections)
                .average()
                .orElse(0.0);

        double maxActiveConnections = metrics.stream()
                .filter(m -> m.getHikariActiveConnections() != null)
                .mapToDouble(MetricsSnapshot::getHikariActiveConnections)
                .max()
                .orElse(0.0);

        int currentReplicas = 3;
        double currentPerPodConnections = avgActiveConnections / currentReplicas;

        // Target: 15 active connections per pod
        double targetPerPodConnections = 15.0;
        int recommendedReplicas = (int) Math.ceil(maxActiveConnections / targetPerPodConnections);
        recommendedReplicas = Math.max(2, Math.min(10, recommendedReplicas));

        String rationale = String.format(
                "Avg connections: %.1f, Max: %.1f. Current per pod: %.1f. Target: %.1f per pod.",
                avgActiveConnections, maxActiveConnections, currentPerPodConnections, targetPerPodConnections);

        String recommendation;
        if (currentPerPodConnections > targetPerPodConnections * 1.3) {
            recommendation = "Scale up - connection pool usage high";
        } else if (currentPerPodConnections < targetPerPodConnections * 0.4) {
            recommendation = "Scale down - connection pool underutilized";
        } else {
            recommendation = "Connection pool usage is healthy";
        }

        return CustomMetricsAnalysis.CustomMetricScaling.builder()
                .metricName("hikari_active_connections")
                .type(CustomMetricsAnalysis.CustomMetricScaling.MetricType.CONNECTION_POOL_USAGE)
                .currentValue(avgActiveConnections)
                .currentPerPodValue(currentPerPodConnections)
                .targetValue(avgActiveConnections)
                .targetPerPodValue(targetPerPodConnections)
                .currentReplicas(currentReplicas)
                .recommendedReplicas(recommendedReplicas)
                .scaleUpThreshold(targetPerPodConnections * 1.3)
                .scaleDownThreshold(targetPerPodConnections * 0.4)
                .rationale(rationale)
                .recommendation(recommendation)
                .build();
    }

    private CustomMetricsAnalysis.CustomMetricScaling analyzeResponseTime(List<MetricsSnapshot> metrics) {
        // Calculate P95 response time
        double p95ResponseTime = metrics.stream()
                .filter(m -> m.getHttpRequestDurationP95() != null)
                .mapToDouble(MetricsSnapshot::getHttpRequestDurationP95)
                .max()
                .orElse(0.0) * 1000; // Convert to ms

        int currentReplicas = 3;

        // Target: P95 < 200ms
        double targetP95 = 200.0;
        int recommendedReplicas = currentReplicas;

        if (p95ResponseTime > targetP95 * 1.5) {
            recommendedReplicas = Math.min(10, currentReplicas + 2);
        } else if (p95ResponseTime < targetP95 * 0.5) {
            recommendedReplicas = Math.max(2, currentReplicas - 1);
        }

        String rationale = String.format(
                "P95 response time: %.1fms. Target: %.1fms. %s",
                p95ResponseTime, targetP95,
                p95ResponseTime > targetP95 ? "Performance degradation detected" : "Performance is good");

        String recommendation;
        if (p95ResponseTime > targetP95 * 1.5) {
            recommendation = "Scale up - response time degraded";
        } else if (p95ResponseTime < targetP95 * 0.5) {
            recommendation = "Consider scaling down - excellent performance";
        } else {
            recommendation = "Response time within acceptable range";
        }

        return CustomMetricsAnalysis.CustomMetricScaling.builder()
                .metricName("http_request_duration_p95")
                .type(CustomMetricsAnalysis.CustomMetricScaling.MetricType.RESPONSE_TIME_P95)
                .currentValue(p95ResponseTime)
                .currentPerPodValue(p95ResponseTime) // Same for all pods
                .targetValue(targetP95)
                .targetPerPodValue(targetP95)
                .currentReplicas(currentReplicas)
                .recommendedReplicas(recommendedReplicas)
                .scaleUpThreshold(targetP95 * 1.5)
                .scaleDownThreshold(targetP95 * 0.5)
                .rationale(rationale)
                .recommendation(recommendation)
                .build();
    }
}
