package com.hackathon.analyzer.collector;

import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import com.hackathon.analyzer.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectorService {

    private final MetricsSnapshotRepository metricsRepository;
    private final WebClient.Builder webClientBuilder;
    private final AnomalyDetectionService anomalyDetectionService;

    @Value("${analyzer.services.cpu-hungry.url:http://localhost:8081}")
    private String cpuHungryUrl;

    @Value("${analyzer.services.memory-leaker.url:http://localhost:8082}")
    private String memoryLeakerUrl;

    @Value("${analyzer.services.db-connection.url:http://localhost:8083}")
    private String dbConnectionUrl;

    @Value("${analyzer.services.greedy.url:http://localhost:8086}")
    private String greedyUrl;

    /**
     * Scheduled metrics collection - runs every 10 seconds
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void collectMetrics() {
        log.debug("Starting metrics collection cycle...");

        try {
            collectServiceMetrics("cpu-hungry-service", cpuHungryUrl);
            collectServiceMetrics("memory-leaker-service", memoryLeakerUrl);
            collectServiceMetrics("db-connection-service", dbConnectionUrl);
            collectServiceMetrics("greedy-service", greedyUrl);

            log.info("Metrics collection completed successfully");
        } catch (Exception e) {
            log.error("Error during metrics collection: {}", e.getMessage(), e);
        }
    }

    /**
     * Collect metrics from a single service
     */
    private void collectServiceMetrics(String serviceName, String baseUrl) {
        try {
            String prometheusMetrics = fetchPrometheusMetrics(baseUrl);
            MetricsSnapshot snapshot = parsePrometheusMetrics(serviceName, prometheusMetrics);
            metricsRepository.save(snapshot);

            log.debug("Collected metrics for {}: CPU={}%, Heap={}MB",
                    serviceName,
                    String.format("%.2f", snapshot.getCpuUsagePercent()),
                    snapshot.getHeapUsedBytes() / (1024.0 * 1024.0));

            // Trigger anomaly detection after collecting metrics
            performAnomalyDetection(serviceName);

        } catch (Exception e) {
            log.warn("Failed to collect metrics from {}: {}", serviceName, e.getMessage());
        }
    }

    /**
     * Perform anomaly detection on recent metrics
     */
    private void performAnomalyDetection(String serviceName) {
        try {
            // Fetch last 60 snapshots (10 minutes of data at 10-second intervals)
            List<MetricsSnapshot> recentSnapshots = metricsRepository
                    .findByServiceNameOrderByTimestampDesc(serviceName, PageRequest.of(0, 60))
                    .getContent();

            if (!recentSnapshots.isEmpty()) {
                // Reverse to get chronological order
                List<MetricsSnapshot> chronologicalSnapshots = new java.util.ArrayList<>(recentSnapshots);
                java.util.Collections.reverse(chronologicalSnapshots);

                // Run anomaly detection
                anomalyDetectionService.analyzeAll(serviceName, chronologicalSnapshots);
            }
        } catch (Exception e) {
            log.warn("Failed to perform anomaly detection for {}: {}", serviceName, e.getMessage());
        }
    }

    /**
     * Fetch Prometheus metrics from service
     */
    private String fetchPrometheusMetrics(String baseUrl) {
        WebClient webClient = webClientBuilder.build();

        return webClient.get()
                .uri(baseUrl + "/actuator/prometheus")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(error -> {
                    log.warn("Failed to fetch metrics from {}: {}", baseUrl, error.getMessage());
                    return Mono.just("");
                })
                .block();
    }

    /**
     * Parse Prometheus text format into MetricsSnapshot
     */
    private MetricsSnapshot parsePrometheusMetrics(String serviceName, String prometheusText) {
        Map<String, Double> metrics = new HashMap<>();

        // Parse Prometheus format
        String[] lines = prometheusText.split("\n");
        for (String line : lines) {
            if (line.startsWith("#") || line.trim().isEmpty()) {
                continue; // Skip comments and empty lines
            }

            // Extract metric name and value
            Pattern pattern = Pattern.compile("^([a-zA-Z_:][a-zA-Z0-9_:]*)(\\{.*?\\})?\\s+([\\d.eE+-]+)");
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                String metricName = matcher.group(1);
                String value = matcher.group(3);

                try {
                    metrics.put(metricName, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
        }

        // Build MetricsSnapshot from parsed metrics
        return MetricsSnapshot.builder()
                .serviceName(serviceName)
                .timestamp(Instant.now())
                // CPU metrics
                .cpuUsagePercent(metrics.getOrDefault("process_cpu_usage", 0.0) * 100)
                .systemCpuUsagePercent(metrics.getOrDefault("system_cpu_usage", 0.0) * 100)
                // Memory metrics
                .heapUsedBytes(metrics.getOrDefault("jvm_memory_used_bytes", 0.0).longValue())
                .heapMaxBytes(metrics.getOrDefault("jvm_memory_max_bytes", 1.0).longValue())
                .heapUsagePercent(calculateHeapUsage(metrics))
                .nonHeapUsedBytes(metrics.getOrDefault("jvm_memory_used_bytes", 0.0).longValue())
                // GC metrics
                .gcPauseTimeMs(metrics.getOrDefault("jvm_gc_pause_seconds_sum", 0.0).longValue() * 1000)
                .gcCount(metrics.getOrDefault("jvm_gc_pause_seconds_count", 0.0).longValue())
                // Thread metrics
                .threadCount(metrics.getOrDefault("jvm_threads_live_threads", 0.0).intValue())
                .daemonThreadCount(metrics.getOrDefault("jvm_threads_daemon_threads", 0.0).intValue())
                // HTTP metrics
                .httpRequestCount(metrics.getOrDefault("http_server_requests_seconds_count", 0.0).longValue())
                .httpRequestDurationAvg(metrics.getOrDefault("http_server_requests_seconds_sum", 0.0) /
                        Math.max(1, metrics.getOrDefault("http_server_requests_seconds_count", 1.0)))
                .httpRequestDurationMax(metrics.getOrDefault("http_server_requests_seconds_max", 0.0) * 1000)
                // Connection pool (HikariCP)
                .hikariActiveConnections(metrics.getOrDefault("hikaricp_connections_active", 0.0).intValue())
                .hikariIdleConnections(metrics.getOrDefault("hikaricp_connections_idle", 0.0).intValue())
                .hikariMaxConnections(metrics.getOrDefault("hikaricp_connections_max", 0.0).intValue())
                .hikariMinConnections(metrics.getOrDefault("hikaricp_connections_min", 0.0).intValue())
                .hikariConnectionTimeout(metrics.getOrDefault("hikaricp_connections_timeout_total", 0.0).longValue())
                .hikariPendingConnections(metrics.getOrDefault("hikaricp_connections_pending", 0.0).intValue())
                .build();
    }

    /**
     * Calculate heap usage percentage
     */
    private Double calculateHeapUsage(Map<String, Double> metrics) {
        double used = metrics.getOrDefault("jvm_memory_used_bytes", 0.0);
        double max = metrics.getOrDefault("jvm_memory_max_bytes", 1.0);

        return max > 0 ? (used / max) * 100 : 0.0;
    }

    /**
     * Get recent metrics for a service
     */
    public Map<String, Object> getRecentMetrics(String serviceName, int limit) {
        List<MetricsSnapshot> snapshots = metricsRepository
                .findByServiceNameAndTimestampAfter(serviceName, Instant.now().minusSeconds(300));

        Map<String, Object> result = new HashMap<>();
        result.put("serviceName", serviceName);
        result.put("snapshotCount", snapshots.size());
        result.put("snapshots", snapshots.stream().limit(limit).toList());

        return result;
    }
}
