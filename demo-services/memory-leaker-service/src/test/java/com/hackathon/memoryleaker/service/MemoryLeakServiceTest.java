package com.hackathon.memoryleaker.service;

import com.hackathon.memoryleaker.model.CachedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryLeakServiceTest {

    private MemoryLeakService memoryLeakService;

    @BeforeEach
    void setUp() {
        memoryLeakService = new MemoryLeakService();
    }

    @Test
    void addToCache_shouldReturnCachedData() {
        CachedData result = memoryLeakService.addToCache("test-key", 100);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-key");
        assertThat(result.getLargeData()).isNotNull();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void getCacheStats_shouldReturnStatistics() {
        memoryLeakService.addToCache("key1", 100);
        memoryLeakService.addToCache("key2", 200);

        Map<String, Object> stats = memoryLeakService.getCacheStats();

        assertThat(stats).containsKeys("cacheSize", "usedMemoryMB", "maxMemoryMB", "memoryUsagePercent");
        assertThat(stats.get("cacheSize")).isEqualTo(2);
    }

    @Test
    void processLargeData_shouldReturnResult() {
        Map<String, Object> result = memoryLeakService.processLargeData(5);

        assertThat(result).containsKeys("operation", "sizeMB", "durationMs");
        assertThat(result.get("operation")).isEqualTo("process-large-data");
    }

    @Test
    void createLargeObjects_shouldReturnObjectList() {
        List<Map<String, Object>> result = memoryLeakService.createLargeObjects(5, 100);

        assertThat(result).hasSize(5);
    }

    @Test
    void clearLeaks_shouldClearCache() {
        memoryLeakService.addToCache("key1", 100);
        memoryLeakService.addToCache("key2", 200);

        Map<String, Object> result = memoryLeakService.clearLeaks();

        assertThat(result.get("cacheCleared")).isEqualTo(true);

        Map<String, Object> stats = memoryLeakService.getCacheStats();
        assertThat(stats.get("cacheSize")).isEqualTo(0);
    }
}
