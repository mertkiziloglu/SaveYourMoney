package com.hackathon.memoryleaker.controller;

import com.hackathon.memoryleaker.model.CachedData;
import com.hackathon.memoryleaker.service.MemoryLeakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemoryLeakerController.class)
class MemoryLeakerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemoryLeakService memoryLeakService;

    @BeforeEach
    void setUp() {
        CachedData mockCachedData = CachedData.builder()
                .key("test-key")
                .data(new byte[1024])
                .timestamp(Instant.now())
                .build();

        when(memoryLeakService.addToCache(anyString(), anyInt())).thenReturn(mockCachedData);

        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("cacheSize", 10);
        mockStats.put("usedMemoryMB", 100.0);
        mockStats.put("memoryUsagePercent", 50.0);
        when(memoryLeakService.getCacheStats()).thenReturn(mockStats);

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        when(memoryLeakService.processLargeData(anyInt())).thenReturn(mockResult);
        when(memoryLeakService.clearLeaks()).thenReturn(mockResult);

        when(memoryLeakService.createLargeObjects(anyInt(), anyInt())).thenReturn(new ArrayList<>());
    }

    @Test
    void health_shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("memory-leaker-service"));
    }

    @Test
    void addToCache_shouldAddDataToCache() throws Exception {
        mockMvc.perform(post("/api/cache/add")
                        .param("key", "test-key")
                        .param("sizeKB", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("cache-add"))
                .andExpect(jsonPath("$.key").value("test-key"))
                .andExpect(jsonPath("$.sizeKB").value(100));

        verify(memoryLeakService, times(1)).addToCache("test-key", 100);
    }

    @Test
    void getStats_shouldReturnCacheStatistics() throws Exception {
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheSize").value(10))
                .andExpect(jsonPath("$.usedMemoryMB").value(100.0));

        verify(memoryLeakService, times(1)).getCacheStats();
    }

    @Test
    void processLargeData_shouldProcessData() throws Exception {
        mockMvc.perform(post("/api/process/large")
                        .param("sizeMB", "10"))
                .andExpect(status().isOk());

        verify(memoryLeakService, times(1)).processLargeData(10);
    }

    @Test
    void memoryBomb_shouldExecuteMultipleOperations() throws Exception {
        mockMvc.perform(post("/api/memory-bomb")
                        .param("iterations", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("memory-bomb"))
                .andExpect(jsonPath("$.iterations").value(5));

        verify(memoryLeakService, atLeast(5)).addToCache(anyString(), anyInt());
    }

    @Test
    void clearLeaks_shouldClearAllLeaks() throws Exception {
        mockMvc.perform(post("/api/clear-leaks"))
                .andExpect(status().isOk());

        verify(memoryLeakService, times(1)).clearLeaks();
    }
}
