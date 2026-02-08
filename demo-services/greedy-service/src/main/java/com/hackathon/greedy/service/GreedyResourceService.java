package com.hackathon.greedy.service;

import com.hackathon.greedy.model.GreedyRecord;
import com.hackathon.greedy.repository.GreedyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The Greedy Service: intentionally over-consumes ALL resource dimensions.
 *
 * 🔥 CPU: Expensive hashing, Fibonacci, matrix multiplication
 * 💾 Memory: Growing in-memory cache that never evicts (leak pattern)
 * 🔌 Connection Pool: Long-running DB queries that hold connections
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GreedyResourceService {

    private final GreedyRecordRepository repository;

    // ═══════════════════════════════════════════
    // 💾 MEMORY LEAK: never-evicted growing cache
    // ═══════════════════════════════════════════
    private final List<byte[]> memoryBlackHole = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentLinkedQueue<Map<String, Object>> eventLog = new ConcurrentLinkedQueue<>();

    /**
     * Allocate memory that is never freed — simulates a memory leak.
     */
    public Map<String, Object> leakMemory(int sizeMb) {
        int chunks = Math.min(sizeMb, 50); // cap at 50MB per request
        for (int i = 0; i < chunks; i++) {
            byte[] leak = new byte[1024 * 1024]; // 1MB chunks
            Arrays.fill(leak, (byte) 0xFF);
            memoryBlackHole.add(leak);
        }

        // Also grow the event log with large objects
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", System.currentTimeMillis());
        event.put("action", "memory_allocation");
        event.put("allocatedMb", chunks);
        event.put("totalLeakedMb", memoryBlackHole.size());
        event.put("largePayload", "X".repeat(8192)); // 8KB per event
        eventLog.add(event);

        log.warn("💾 MEMORY LEAK: Allocated {}MB, total leaked: {}MB", chunks, memoryBlackHole.size());
        return event;
    }

    /**
     * Get current memory leak status.
     */
    public Map<String, Object> getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> status = new HashMap<>();
        status.put("leakedChunksMb", memoryBlackHole.size());
        status.put("eventLogSize", eventLog.size());
        status.put("heapUsedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        status.put("heapMaxMb", runtime.maxMemory() / (1024 * 1024));
        status.put("heapUsagePercent",
                ((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100);
        return status;
    }

    // ═══════════════════════════════════════════
    // 🔥 CPU BURN: expensive computations
    // ═══════════════════════════════════════════

    /**
     * CPU-intensive hashing with many iterations.
     */
    public String burnCpu(int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            String result = "greedy-burn-" + System.nanoTime();

            for (int i = 0; i < iterations; i++) {
                byte[] hash = md.digest(result.getBytes());
                result = bytesToHex(hash);
            }

            log.info("🔥 CPU BURN: {} SHA-512 iterations completed", iterations);
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Fibonacci — intentionally O(2^n) recursive.
     */
    public long fibonacci(int n) {
        if (n <= 1)
            return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    /**
     * Matrix multiplication — O(n³).
     */
    public double[][] matrixMultiply(int size) {
        Random rnd = new Random();
        double[][] a = new double[size][size];
        double[][] b = new double[size][size];
        double[][] result = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                a[i][j] = rnd.nextDouble() * 100;
                b[i][j] = rnd.nextDouble() * 100;
            }
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        log.info("🔥 CPU BURN: {}x{} matrix multiplication done", size, size);
        return result;
    }

    // ═══════════════════════════════════════════
    // 🔌 CONNECTION POOL EXHAUSTION: hold connections
    // ═══════════════════════════════════════════

    /**
     * Perform a heavy DB operation that holds the connection for a long time.
     * Combines CPU burn + memory allocation + DB write in a single call.
     */
    public Map<String, Object> greedyDbOperation(int complexity) {
        long start = System.currentTimeMillis();

        // 1. CPU burn while holding a DB transaction
        String hash = burnCpu(Math.max(500, complexity * 100));

        // 2. Create large records to fill DB
        List<GreedyRecord> records = new ArrayList<>();
        for (int i = 0; i < Math.min(complexity, 20); i++) {
            records.add(GreedyRecord.builder()
                    .payload("GREEDY-" + UUID.randomUUID() + "-" + "X".repeat(2048))
                    .hashResult(hash.substring(0, 64))
                    .computationTimeMs(System.currentTimeMillis() - start)
                    .createdAt(System.currentTimeMillis())
                    .build());
        }
        repository.saveAll(records);

        // 3. Run a heavy read query (full table scan)
        long totalRecords = repository.count();

        long duration = System.currentTimeMillis() - start;

        Map<String, Object> result = new HashMap<>();
        result.put("recordsInserted", records.size());
        result.put("totalRecords", totalRecords);
        result.put("hashComputed", hash.substring(0, 32) + "...");
        result.put("durationMs", duration);
        result.put("connectionHeldMs", duration);

        log.warn("🔌 DB EXHAUSTION: Held connection for {}ms, {} total records", duration, totalRecords);
        return result;
    }

    // ═══════════════════════════════════════════
    // 💀 COMBINED: burn everything at once
    // ═══════════════════════════════════════════

    /**
     * The ULTIMATE greedy endpoint — burns CPU, leaks memory, and exhausts
     * connections simultaneously.
     */
    public Map<String, Object> burnEverything() {
        long start = System.currentTimeMillis();
        Map<String, Object> results = new HashMap<>();

        // 1. Leak 5MB of memory
        Map<String, Object> memResult = leakMemory(5);
        results.put("memoryLeak", memResult);

        // 2. Burn CPU (fibonacci + hashing + matrix)
        long fib = fibonacci(33);
        String hash = burnCpu(3000);
        matrixMultiply(120);
        results.put("fibonacciResult", fib);
        results.put("cpuHashIterations", 3000);
        results.put("matrixSize", "120x120");

        // 3. Exhaust DB connections
        Map<String, Object> dbResult = greedyDbOperation(10);
        results.put("dbOperation", dbResult);

        long duration = System.currentTimeMillis() - start;
        results.put("totalDurationMs", duration);
        results.put("warning", "💀 This endpoint intentionally over-consumes ALL resources!");

        log.error("💀 BURN EVERYTHING completed in {}ms — CPU + Memory + DB all consumed!", duration);
        return results;
    }

    // ═══════════════════════════════════════════
    // ⏰ BACKGROUND TASKS: continuous resource drain
    // ═══════════════════════════════════════════

    /**
     * Background CPU burn — runs every 8 seconds.
     */
    @Scheduled(fixedDelay = 8000)
    public void backgroundCpuBurn() {
        fibonacci(34);
        burnCpu(2000);
        matrixMultiply(90);
        log.debug("Background CPU burn completed");
    }

    /**
     * Background memory leak — grows 1MB every 15 seconds.
     */
    @Scheduled(fixedDelay = 15000, initialDelay = 3000)
    public void backgroundMemoryLeak() {
        byte[] leak = new byte[1024 * 1024];
        Arrays.fill(leak, (byte) 0xAB);
        memoryBlackHole.add(leak);

        eventLog.add(Map.of(
                "timestamp", System.currentTimeMillis(),
                "action", "background_leak",
                "totalMb", memoryBlackHole.size()));

        log.debug("Background memory leak: {}MB total", memoryBlackHole.size());
    }

    /**
     * Background DB writes — inserts records every 12 seconds.
     */
    @Scheduled(fixedDelay = 12000, initialDelay = 5000)
    public void backgroundDbWrites() {
        try {
            String hash = burnCpu(200);
            repository.save(GreedyRecord.builder()
                    .payload("BG-GREEDY-" + UUID.randomUUID() + "-" + "Y".repeat(1024))
                    .hashResult(hash.substring(0, 64))
                    .computationTimeMs(0L)
                    .createdAt(System.currentTimeMillis())
                    .build());
            log.debug("Background DB write completed, total records: {}", repository.count());
        } catch (Exception e) {
            log.warn("Background DB write failed: {}", e.getMessage());
        }
    }

    // ─── Helpers ────────────────────────────────
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
