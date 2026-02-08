package com.hackathon.memoryleaker.service;

import com.hackathon.memoryleaker.model.CachedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MemoryLeakService {

    // INTENTIONAL MEMORY LEAK: This cache grows indefinitely!
    private final Map<String, CachedData> leakyCache = new ConcurrentHashMap<>();

    // Growing list that's never cleaned
    private final List<byte[]> leakyList = new ArrayList<>();

    private final Random random = new Random();

    /**
     * Add data to leaky cache (never removed!)
     */
    public CachedData addToCache(String key, int dataSizeKB) {
        // Generate large byte array
        byte[] largeData = new byte[dataSizeKB * 1024];
        random.nextBytes(largeData);

        CachedData cachedData = new CachedData(
                key,
                largeData,
                Instant.now(),
                "Metadata for " + key);

        // LEAK: Never removed from cache!
        leakyCache.put(key, cachedData);

        log.debug("Added {}KB to cache. Total cache size: {} items",
                dataSizeKB, leakyCache.size());

        return cachedData;
    }

    /**
     * Process large data (allocates memory temporarily)
     */
    public Map<String, Object> processLargeData(int dataSizeMB) {
        long startTime = System.currentTimeMillis();

        // Allocate large chunk of memory
        int arraySize = dataSizeMB * 1024 * 1024;
        byte[] largeArray = new byte[arraySize];

        // Do some processing
        Arrays.fill(largeArray, (byte) random.nextInt(256));

        // LEAK: Keep reference to some data
        leakyList.add(Arrays.copyOf(largeArray, Math.min(arraySize, 10 * 1024 * 1024)));

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("processedMB", dataSizeMB);
        result.put("durationMs", duration);
        result.put("leakyListSize", leakyList.size());

        log.debug("Processed {}MB data in {}ms. Leaky list size: {}",
                dataSizeMB, duration, leakyList.size());

        return result;
    }

    /**
     * Create multiple large objects (simulates user sessions, etc.)
     */
    public List<Map<String, Object>> createLargeObjects(int count, int sizeKB) {
        List<Map<String, Object>> objects = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            byte[] data = new byte[sizeKB * 1024];
            random.nextBytes(data);

            Map<String, Object> obj = new HashMap<>();
            obj.put("id", UUID.randomUUID().toString());
            obj.put("data", data);
            obj.put("timestamp", Instant.now());

            objects.add(obj);

            // LEAK: Add to cache too
            leakyCache.put("object-" + i, new CachedData(
                    "object-" + i,
                    data,
                    Instant.now(),
                    "Large object " + i));
        }

        log.debug("Created {} large objects of {}KB each. Total cache: {} items",
                count, sizeKB, leakyCache.size());

        return objects;
    }

    /**
     * Simulate session data accumulation
     */
    public void createSessionData(String sessionId, int dataSizeKB) {
        byte[] sessionData = new byte[dataSizeKB * 1024];
        random.nextBytes(sessionData);

        // LEAK: Sessions are never cleaned up!
        leakyCache.put("session-" + sessionId, new CachedData(
                sessionId,
                sessionData,
                Instant.now(),
                "Session data"));

        log.debug("Created session {} with {}KB data. Total sessions in cache: {}",
                sessionId, dataSizeKB,
                leakyCache.keySet().stream().filter(k -> k.startsWith("session-")).count());
    }

    /**
     * Background task that continuously leaks memory
     * Runs every 30 seconds
     */
    @Scheduled(fixedDelay = 30000)
    public void backgroundMemoryLeak() {
        // Silently leak 2MB every 30 seconds
        byte[] leak = new byte[2 * 1024 * 1024];
        random.nextBytes(leak);
        leakyList.add(leak);

        log.debug("Background leak: Added 2MB to leaky list. Total size: {} items",
                leakyList.size());
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        long totalBytes = leakyCache.values().stream()
                .mapToLong(CachedData::getSizeInBytes)
                .sum();

        long leakyListBytes = leakyList.stream()
                .mapToLong(arr -> arr.length)
                .sum();

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", leakyCache.size());
        stats.put("cacheSizeMB", totalBytes / (1024.0 * 1024.0));
        stats.put("leakyListSize", leakyList.size());
        stats.put("leakyListSizeMB", leakyListBytes / (1024.0 * 1024.0));
        stats.put("usedMemoryMB", usedMemory / (1024.0 * 1024.0));
        stats.put("maxMemoryMB", maxMemory / (1024.0 * 1024.0));
        stats.put("memoryUsagePercent", (usedMemory * 100.0) / maxMemory);

        return stats;
    }

    /**
     * DANGEROUS: Clear all leaks (for testing purposes)
     * In production, this would never be called!
     */
    public Map<String, Object> clearLeaks() {
        int cacheSize = leakyCache.size();
        int listSize = leakyList.size();

        leakyCache.clear();
        leakyList.clear();

        System.gc(); // Request garbage collection

        Map<String, Object> result = new HashMap<>();
        result.put("clearedCacheItems", cacheSize);
        result.put("clearedListItems", listSize);
        result.put("warning", "This is for demo purposes only - real leaks can't be cleared!");

        log.warn("CLEARED ALL LEAKS - Cache: {} items, List: {} items", cacheSize, listSize);

        return result;
    }
}
