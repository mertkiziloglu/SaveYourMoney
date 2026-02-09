package com.hackathon.analyzer.discovery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDiscoveryService {

    private final WebClient.Builder webClientBuilder;

    // Thread-safe registry of discovered services
    private final Map<String, ServiceInfo> serviceRegistry = new ConcurrentHashMap<>();

    @Value("${analyzer.discovery.port-range-start:8080}")
    private int portRangeStart;

    @Value("${analyzer.discovery.port-range-end:8099}")
    private int portRangeEnd;

    @Value("${analyzer.discovery.scan-enabled:true}")
    private boolean scanEnabled;

    @Value("${analyzer.discovery.scan-host:localhost}")
    private String scanHost;

    @Value("${server.port:8084}")
    private int analyzerPort;

    @PostConstruct
    public void init() {
        log.info("Service Discovery initialized. Scan range: {}:{}-{}",
                scanHost, portRangeStart, portRangeEnd);

        // Pre-register known demo services
        registerKnownDemoServices();
    }

    /**
     * Pre-register the known demo services for backwards compatibility
     */
    private void registerKnownDemoServices() {
        registerService("cpu-hungry-service", "http://localhost:8081", "PREDEFINED");
        registerService("memory-leaker-service", "http://localhost:8082", "PREDEFINED");
        registerService("db-connection-service", "http://localhost:8083", "PREDEFINED");
        registerService("greedy-service", "http://localhost:8086", "PREDEFINED");
        log.info("Pre-registered 4 demo services");
    }

    /**
     * Scheduled port scanning - runs every 30 seconds
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void scanForServices() {
        if (!scanEnabled) {
            return;
        }

        log.debug("Starting port scan for services on {}:{}-{}",
                scanHost, portRangeStart, portRangeEnd);

        int discovered = 0;

        for (int port = portRangeStart; port <= portRangeEnd; port++) {
            // Skip analyzer's own port
            if (port == analyzerPort) {
                continue;
            }

            String baseUrl = "http://" + scanHost + ":" + port;

            try {
                if (checkActuatorEndpoint(baseUrl)) {
                    String serviceName = discoverServiceName(baseUrl, port);

                    if (!serviceRegistry.containsKey(serviceName) ||
                        !"PREDEFINED".equals(serviceRegistry.get(serviceName).getDiscoveryMethod())) {

                        registerService(serviceName, baseUrl, "SCAN");
                        discovered++;
                        log.info("Discovered new service via port scan: {} at {}", serviceName, baseUrl);
                    } else {
                        // Update last seen time for existing services
                        updateLastSeen(serviceName);
                    }
                }
            } catch (Exception e) {
                // Port not responding or not a valid service - this is expected
                log.trace("Port {} not responding: {}", port, e.getMessage());
            }
        }

        if (discovered > 0) {
            log.info("Port scan completed. Discovered {} new service(s). Total registered: {}",
                    discovered, serviceRegistry.size());
        }

        // Clean up stale services (not seen for 5 minutes)
        cleanupStaleServices();
    }

    /**
     * Check if a URL has actuator/prometheus endpoint
     */
    private boolean checkActuatorEndpoint(String baseUrl) {
        try {
            WebClient webClient = webClientBuilder.build();

            String response = webClient.get()
                    .uri(baseUrl + "/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .onErrorResume(e -> Mono.empty())
                    .block();

            return response != null && response.contains("UP");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Try to discover the service name from actuator/info or generate one
     */
    private String discoverServiceName(String baseUrl, int port) {
        try {
            WebClient webClient = webClientBuilder.build();

            String infoResponse = webClient.get()
                    .uri(baseUrl + "/actuator/info")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .onErrorResume(e -> Mono.empty())
                    .block();

            // Try to extract app name from info endpoint
            if (infoResponse != null && infoResponse.contains("\"app\"")) {
                // Simple parsing - look for app.name
                int nameIndex = infoResponse.indexOf("\"name\"");
                if (nameIndex > 0) {
                    int startQuote = infoResponse.indexOf("\"", nameIndex + 7);
                    int endQuote = infoResponse.indexOf("\"", startQuote + 1);
                    if (startQuote > 0 && endQuote > startQuote) {
                        return infoResponse.substring(startQuote + 1, endQuote);
                    }
                }
            }

            // Try spring.application.name from env endpoint
            String envResponse = webClient.get()
                    .uri(baseUrl + "/actuator/env/spring.application.name")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(2))
                    .onErrorResume(e -> Mono.empty())
                    .block();

            if (envResponse != null && envResponse.contains("\"value\"")) {
                int valueIndex = envResponse.indexOf("\"value\"");
                int startQuote = envResponse.indexOf("\"", valueIndex + 8);
                int endQuote = envResponse.indexOf("\"", startQuote + 1);
                if (startQuote > 0 && endQuote > startQuote) {
                    return envResponse.substring(startQuote + 1, endQuote);
                }
            }
        } catch (Exception e) {
            log.trace("Could not discover service name from {}: {}", baseUrl, e.getMessage());
        }

        // Fallback: generate name based on port
        return "service-port-" + port;
    }

    /**
     * Register a service manually (via REST API)
     */
    public ServiceInfo registerService(String name, String url, String method) {
        ServiceInfo existing = serviceRegistry.get(name);

        ServiceInfo serviceInfo = ServiceInfo.builder()
                .name(name)
                .url(url)
                .port(extractPort(url))
                .healthy(true)
                .discoveredAt(existing != null ? existing.getDiscoveredAt() : Instant.now())
                .lastSeenAt(Instant.now())
                .discoveryMethod(method)
                .build();

        serviceRegistry.put(name, serviceInfo);
        log.info("Service registered: {} at {} (method: {})", name, url, method);

        return serviceInfo;
    }

    /**
     * Unregister a service
     */
    public boolean unregisterService(String name) {
        ServiceInfo removed = serviceRegistry.remove(name);
        if (removed != null) {
            log.info("Service unregistered: {}", name);
            return true;
        }
        return false;
    }

    /**
     * Update last seen timestamp
     */
    private void updateLastSeen(String serviceName) {
        ServiceInfo info = serviceRegistry.get(serviceName);
        if (info != null) {
            info.setLastSeenAt(Instant.now());
            info.setHealthy(true);
        }
    }

    /**
     * Remove services not seen for 5 minutes (except predefined ones)
     */
    private void cleanupStaleServices() {
        Instant cutoff = Instant.now().minusSeconds(300);

        serviceRegistry.entrySet().removeIf(entry -> {
            ServiceInfo info = entry.getValue();
            if ("PREDEFINED".equals(info.getDiscoveryMethod())) {
                return false; // Never remove predefined services
            }
            if (info.getLastSeenAt().isBefore(cutoff)) {
                log.info("Removing stale service: {} (last seen: {})",
                        entry.getKey(), info.getLastSeenAt());
                return true;
            }
            return false;
        });
    }

    /**
     * Get all registered services
     */
    public Collection<ServiceInfo> getAllServices() {
        return Collections.unmodifiableCollection(serviceRegistry.values());
    }

    /**
     * Get only healthy services
     */
    public List<ServiceInfo> getHealthyServices() {
        return serviceRegistry.values().stream()
                .filter(ServiceInfo::isHealthy)
                .toList();
    }

    /**
     * Get service by name
     */
    public Optional<ServiceInfo> getService(String name) {
        return Optional.ofNullable(serviceRegistry.get(name));
    }

    /**
     * Check if a service exists
     */
    public boolean hasService(String name) {
        return serviceRegistry.containsKey(name);
    }

    /**
     * Get service URL by name
     */
    public String getServiceUrl(String name) {
        ServiceInfo info = serviceRegistry.get(name);
        return info != null ? info.getUrl() : null;
    }

    /**
     * Extract port from URL
     */
    private int extractPort(String url) {
        try {
            String portStr = url.replaceAll(".*:(\\d+).*", "$1");
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Health check all registered services
     */
    @Scheduled(fixedDelay = 15000, initialDelay = 5000)
    public void healthCheckServices() {
        for (ServiceInfo service : serviceRegistry.values()) {
            boolean healthy = checkActuatorEndpoint(service.getUrl());
            service.setHealthy(healthy);

            if (healthy) {
                service.setLastSeenAt(Instant.now());
            }

            log.debug("Health check for {}: {}", service.getName(), healthy ? "UP" : "DOWN");
        }
    }
}
