package com.hackathon.analyzer.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsSnapshotTest {

    @Test
    void builder_shouldCreateMetricsSnapshot() {
        Instant now = Instant.now();

        MetricsSnapshot snapshot = MetricsSnapshot.builder()
                .serviceName("test-service")
                .timestamp(now)
                .cpuUsagePercent(50.0)
                .systemCpuUsagePercent(60.0)
                .heapUsedBytes(512L * 1024 * 1024)
                .heapMaxBytes(1024L * 1024 * 1024)
                .heapUsagePercent(50.0)
                .threadCount(100)
                .hikariActiveConnections(10)
                .hikariMaxConnections(20)
                .build();

        assertThat(snapshot.getServiceName()).isEqualTo("test-service");
        assertThat(snapshot.getTimestamp()).isEqualTo(now);
        assertThat(snapshot.getCpuUsagePercent()).isEqualTo(50.0);
        assertThat(snapshot.getHeapUsedBytes()).isEqualTo(512L * 1024 * 1024);
        assertThat(snapshot.getThreadCount()).isEqualTo(100);
        assertThat(snapshot.getHikariActiveConnections()).isEqualTo(10);
    }

    @Test
    void settersAndGetters_shouldWork() {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        snapshot.setServiceName("test");
        snapshot.setCpuUsagePercent(75.5);
        snapshot.setThreadCount(50);

        assertThat(snapshot.getServiceName()).isEqualTo("test");
        assertThat(snapshot.getCpuUsagePercent()).isEqualTo(75.5);
        assertThat(snapshot.getThreadCount()).isEqualTo(50);
    }
}
