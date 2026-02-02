package com.hackathon.cpuhungry.controller;

import com.hackathon.cpuhungry.service.CpuIntensiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CpuHungryController {

    private final CpuIntensiveService cpuIntensiveService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "cpu-hungry-service");
        return ResponseEntity.ok(response);
    }

    /**
     * CPU-intensive hashing endpoint
     * JMeter should hit this with high concurrency
     */
    @PostMapping("/hash")
    public ResponseEntity<Map<String, Object>> performHashing(
            @RequestParam(defaultValue = "sample-data-to-hash") String input,
            @RequestParam(defaultValue = "1000") int iterations) {

        long startTime = System.currentTimeMillis();

        String result = cpuIntensiveService.performHashing(input, iterations);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "hashing");
        response.put("iterations", iterations);
        response.put("durationMs", duration);
        response.put("result", result.substring(0, Math.min(32, result.length())) + "...");

        log.info("Hashing completed in {}ms with {} iterations", duration, iterations);
        return ResponseEntity.ok(response);
    }

    /**
     * CPU-intensive prime number calculation endpoint
     * JMeter should hit this with medium concurrency
     */
    @GetMapping("/primes")
    public ResponseEntity<Map<String, Object>> calculatePrimes(
            @RequestParam(defaultValue = "500") int limit) {

        long startTime = System.currentTimeMillis();

        List<Long> primes = cpuIntensiveService.calculatePrimes(limit);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "prime-calculation");
        response.put("limit", limit);
        response.put("primesFound", primes.size());
        response.put("durationMs", duration);
        response.put("firstFivePrimes", primes.subList(0, Math.min(5, primes.size())));

        log.info("Prime calculation completed in {}ms, found {} primes", duration, primes.size());
        return ResponseEntity.ok(response);
    }

    /**
     * CPU-intensive Fibonacci calculation endpoint
     * WARNING: This will BURN CPU intentionally!
     */
    @GetMapping("/fibonacci")
    public ResponseEntity<Map<String, Object>> calculateFibonacci(
            @RequestParam(defaultValue = "35") int n) {

        // Limit to prevent infinite hang
        if (n > 40) {
            n = 40;
        }

        long startTime = System.currentTimeMillis();

        long result = cpuIntensiveService.calculateFibonacci(n);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "fibonacci");
        response.put("n", n);
        response.put("result", result);
        response.put("durationMs", duration);
        response.put("warning", "Intentionally inefficient recursive algorithm for CPU burn");

        log.info("Fibonacci({}) = {} calculated in {}ms", n, result, duration);
        return ResponseEntity.ok(response);
    }

    /**
     * CPU-intensive matrix multiplication endpoint
     * JMeter should hit this for sustained CPU load
     */
    @GetMapping("/matrix")
    public ResponseEntity<Map<String, Object>> multiplyMatrices(
            @RequestParam(defaultValue = "100") int size) {

        // Limit matrix size to prevent memory issues
        if (size > 200) {
            size = 200;
        }

        long startTime = System.currentTimeMillis();

        double[][] result = cpuIntensiveService.multiplyMatrices(size);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "matrix-multiplication");
        response.put("matrixSize", size + "x" + size);
        response.put("durationMs", duration);
        response.put("complexity", "O(n³)");
        response.put("sampleResult", result[0][0]);

        log.info("Matrix multiplication ({}x{}) completed in {}ms", size, size, duration);
        return ResponseEntity.ok(response);
    }

    /**
     * CPU-intensive text processing endpoint
     * Combined workload for realistic scenario
     */
    @PostMapping("/process-text")
    public ResponseEntity<Map<String, Object>> processText(
            @RequestBody(required = false) String text,
            @RequestParam(defaultValue = "50") int complexity) {

        if (text == null || text.isEmpty()) {
            text = "Sample text for CPU-intensive processing. This will be hashed, reversed, and manipulated multiple times.";
        }

        long startTime = System.currentTimeMillis();

        String result = cpuIntensiveService.processText(text, complexity);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "text-processing");
        response.put("inputLength", text.length());
        response.put("complexity", complexity);
        response.put("durationMs", duration);
        response.put("result", result.substring(0, Math.min(64, result.length())) + "...");

        log.info("Text processing completed in {}ms with complexity {}", duration, complexity);
        return ResponseEntity.ok(response);
    }

    /**
     * Combined CPU burn endpoint - hits ALL CPU-intensive operations
     * This is the KILLER endpoint for JMeter stress testing
     */
    @GetMapping("/burn-cpu")
    public ResponseEntity<Map<String, Object>> burnCpu() {

        long startTime = System.currentTimeMillis();

        Map<String, Object> results = new HashMap<>();

        // Execute all CPU-intensive operations
        String hashResult = cpuIntensiveService.performHashing("burn-test", 500);
        List<Long> primes = cpuIntensiveService.calculatePrimes(300);
        long fibonacci = cpuIntensiveService.calculateFibonacci(30);
        double[][] matrix = cpuIntensiveService.multiplyMatrices(50);

        long duration = System.currentTimeMillis() - startTime;

        results.put("operation", "combined-cpu-burn");
        results.put("durationMs", duration);
        results.put("hashCompleted", true);
        results.put("primesFound", primes.size());
        results.put("fibonacciResult", fibonacci);
        results.put("matrixMultiplied", true);
        results.put("warning", "This endpoint intentionally consumes maximum CPU!");

        log.warn("CPU BURN completed in {}ms - this is intentionally resource-intensive!", duration);
        return ResponseEntity.ok(results);
    }
}
