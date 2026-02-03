package com.hackathon.analyzer.collector;

import com.hackathon.analyzer.model.MetricsSnapshot;
import com.hackathon.analyzer.repository.MetricsSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsCollectorServiceTest {

    @Mock
    private MetricsSnapshotRepository metricsRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private MetricsCollectorService metricsCollectorService;

    private String mockPrometheusData;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(metricsCollectorService, "cpuHungryUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(metricsCollectorService, "memoryLeakerUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(metricsCollectorService, "dbConnectionUrl", "http://localhost:8083");

        mockPrometheusData = """
                # HELP process_cpu_usage The "recent cpu usage" for the Java Virtual Machine process
                # TYPE process_cpu_usage gauge
                process_cpu_usage 0.5
                # HELP system_cpu_usage The "recent cpu usage" for the whole system
                # TYPE system_cpu_usage gauge
                system_cpu_usage 0.6
                # HELP jvm_memory_used_bytes The amount of used memory
                # TYPE jvm_memory_used_bytes gauge
                jvm_memory_used_bytes 536870912.0
                # HELP jvm_memory_max_bytes The maximum amount of memory in bytes that can be used for memory management
                # TYPE jvm_memory_max_bytes gauge
                jvm_memory_max_bytes 1073741824.0
                # HELP jvm_threads_live_threads The current number of live threads including both daemon and non-daemon threads
                # TYPE jvm_threads_live_threads gauge
                jvm_threads_live_threads 50
                """;

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockPrometheusData));
    }

    @Test
    void collectMetrics_shouldCollectFromAllServices() {
        when(metricsRepository.save(any(MetricsSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        metricsCollectorService.collectMetrics();

        verify(metricsRepository, times(3)).save(any(MetricsSnapshot.class));
    }

    @Test
    void collectMetrics_shouldParsePrometheusDataCorrectly() {
        ArgumentCaptor<MetricsSnapshot> snapshotCaptor = ArgumentCaptor.forClass(MetricsSnapshot.class);
        when(metricsRepository.save(any(MetricsSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        metricsCollectorService.collectMetrics();

        verify(metricsRepository, atLeastOnce()).save(snapshotCaptor.capture());

        MetricsSnapshot captured = snapshotCaptor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getCpuUsagePercent()).isEqualTo(50.0);
        assertThat(captured.getHeapUsedBytes()).isEqualTo(536870912L);
    }

    @Test
    void collectMetrics_whenServiceUnavailable_shouldContinue() {
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        metricsCollectorService.collectMetrics();

        verify(metricsRepository, never()).save(any(MetricsSnapshot.class));
    }

    @Test
    void getRecentMetrics_shouldReturnMetricsMap() {
        List<MetricsSnapshot> mockSnapshots = List.of(
                MetricsSnapshot.builder()
                        .serviceName("test-service")
                        .timestamp(Instant.now())
                        .cpuUsagePercent(50.0)
                        .build()
        );

        when(metricsRepository.findByServiceNameAndTimestampAfter(anyString(), any(Instant.class)))
                .thenReturn(mockSnapshots);

        Map<String, Object> result = metricsCollectorService.getRecentMetrics("test-service", 100);

        assertThat(result).containsKeys("serviceName", "snapshotCount", "snapshots");
        assertThat(result.get("serviceName")).isEqualTo("test-service");
        assertThat(result.get("snapshotCount")).isEqualTo(1);

        verify(metricsRepository, times(1))
                .findByServiceNameAndTimestampAfter(anyString(), any(Instant.class));
    }
}
