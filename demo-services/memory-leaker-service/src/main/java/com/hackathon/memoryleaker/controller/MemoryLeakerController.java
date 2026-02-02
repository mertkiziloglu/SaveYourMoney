package com.hackathon.memoryleaker.controller;

import com.hackathon.memoryleaker.model.CachedData;
import com.hackathon.memoryleaker.service.MemoryLeakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemoryLeakerController {

    private final MemoryLeakService memoryLeakService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "memory-leaker-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Add data to leaky cache
     * JMeter should hit this repeatedly to cause memory growth
     */
    @PostMapping("/cache/add")
    public ResponseEntity<Map<String, Object>> addToCache(
            @RequestParam(defaultValue = "random") String key,
            @RequestParam(defaultValue = "100") int sizeKB) {

        if ("random".equals(key)) {
            key = UUID.randomUUID().toString();
        }

        long startTime = System.currentTimeMillis();

        CachedData cached = memoryLeakService.addToCache(key, sizeKB);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "cache-add");
        response.put("key", key);
        response.put("sizeKB", sizeKB);
        response.put("durationMs", duration);
        response.put("warning", "This data will NEVER be removed from cache!");

        log.info("Added {}KB to cache with key: {}", sizeKB, key);
        return ResponseEntity.ok(response);
    }

    /**
     * Process large data chunks
     * Causes temporary memory spikes + permanent leaks
     */
    @PostMapping("/process/large")
    public ResponseEntity<Map<String, Object>> processLargeData(
            @RequestParam(defaultValue = "10") int sizeMB) {

        // Limit to prevent immediate OOM
        if (sizeMB > 50) {
            sizeMB = 50;
        }

        Map<String, Object> result = memoryLeakService.processLargeData(sizeMB);

        log.info("Processed {}MB of data", sizeMB);
        return ResponseEntity.ok(result);
    }

    /**
     * Create multiple large objects
     * Simulates batch processing with memory leak
     */
    @PostMapping("/objects/create")
    public ResponseEntity<Map<String, Object>> createLargeObjects(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "50") int sizeKB) {

        long startTime = System.currentTimeMillis();

        List<Map<String, Object>> objects = memoryLeakService.createLargeObjects(count, sizeKB);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "create-objects");
        response.put("objectsCreated", count);
        response.put("sizePerObjectKB", sizeKB);
        response.put("totalSizeMB", (count * sizeKB) / 1024.0);
        response.put("durationMs", duration);
        response.put("warning", "Objects are cached and never cleaned!");

        log.info("Created {} objects of {}KB each", count, sizeKB);
        return ResponseEntity.ok(response);
    }

    /**
     * Simulate session data creation
     * Typical web app scenario with memory leak
     */
    @PostMapping("/session/create")
    public ResponseEntity<Map<String, Object>> createSession(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "200") int dataSizeKB) {

        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        memoryLeakService.createSessionData(sessionId, dataSizeKB);

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "session-create");
        response.put("sessionId", sessionId);
        response.put("dataSizeKB", dataSizeKB);
        response.put("warning", "Session data is never expired!");

        log.info("Created session {} with {}KB data", sessionId, dataSizeKB);
        return ResponseEntity.ok(response);
    }

    /**
     * Get cache and memory statistics
     * Shows the memory leak in action!
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = memoryLeakService.getCacheStats();

        log.info("Memory stats - Cache: {} items, Used: {}MB, Usage: {}%",
                stats.get("cacheSize"),
                String.format("%.2f", stats.get("usedMemoryMB")),
                String.format("%.2f", stats.get("memoryUsagePercent")));

        return ResponseEntity.ok(stats);
    }

    /**
     * Memory bomb - causes rapid memory growth
     * This is the KILLER endpoint for JMeter
     */
    @PostMapping("/memory-bomb")
    public ResponseEntity<Map<String, Object>> memoryBomb(
            @RequestParam(defaultValue = "20") int iterations) {

        long startTime = System.currentTimeMillis();

        // Create lots of data quickly
        for (int i = 0; i < iterations; i++) {
            memoryLeakService.addToCache("bomb-" + i, 500); // 500KB each
            memoryLeakService.createSessionData("bomb-session-" + i, 300);
        }

        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> stats = memoryLeakService.getCacheStats();

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "memory-bomb");
        response.put("iterations", iterations);
        response.put("estimatedLeakMB", (iterations * 800) / 1024.0);
        response.put("durationMs", duration);
        response.put("currentStats", stats);
        response.put("warning", "This endpoint intentionally leaks massive amounts of memory!");

        log.warn("MEMORY BOMB executed - {} iterations, leaked ~{}MB",
                iterations, (iterations * 800) / 1024.0);

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all leaks (for demo reset purposes)
     */
    @PostMapping("/clear-leaks")
    public ResponseEntity<Map<String, Object>> clearLeaks() {
        Map<String, Object> result = memoryLeakService.clearLeaks();

        log.warn("All leaks cleared for demo purposes");
        return ResponseEntity.ok(result);
    }
}
