package com.hackathon.greedy.controller;

import com.hackathon.greedy.service.GreedyResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GreedyController {

    private final GreedyResourceService greedyService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "greedy-service");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔥 CPU Burn endpoint — expensive SHA-512 hashing
     */
    @PostMapping("/burn-cpu")
    public ResponseEntity<Map<String, Object>> burnCpu(
            @RequestParam(defaultValue = "2000") int iterations) {

        long start = System.currentTimeMillis();
        String result = greedyService.burnCpu(iterations);
        long duration = System.currentTimeMillis() - start;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "cpu-burn");
        response.put("iterations", iterations);
        response.put("algorithm", "SHA-512");
        response.put("durationMs", duration);
        response.put("result", result.substring(0, 32) + "...");

        return ResponseEntity.ok(response);
    }

    /**
     * 🔥 Fibonacci endpoint — O(2^n) recursive
     */
    @GetMapping("/fibonacci")
    public ResponseEntity<Map<String, Object>> fibonacci(
            @RequestParam(defaultValue = "37") int n) {

        n = Math.min(n, 42); // safety cap

        long start = System.currentTimeMillis();
        long result = greedyService.fibonacci(n);
        long duration = System.currentTimeMillis() - start;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "fibonacci");
        response.put("n", n);
        response.put("result", result);
        response.put("durationMs", duration);

        return ResponseEntity.ok(response);
    }

    /**
     * 💾 Memory Leak endpoint — allocate memory that's never freed
     */
    @PostMapping("/leak-memory")
    public ResponseEntity<Map<String, Object>> leakMemory(
            @RequestParam(defaultValue = "10") int sizeMb) {

        Map<String, Object> result = greedyService.leakMemory(sizeMb);
        return ResponseEntity.ok(result);
    }

    /**
     * 💾 Memory Status
     */
    @GetMapping("/memory-status")
    public ResponseEntity<Map<String, Object>> memoryStatus() {
        return ResponseEntity.ok(greedyService.getMemoryStatus());
    }

    /**
     * 🔌 DB Exhaustion — hold connections with heavy operations
     */
    @PostMapping("/exhaust-db")
    public ResponseEntity<Map<String, Object>> exhaustDb(
            @RequestParam(defaultValue = "5") int complexity) {

        Map<String, Object> result = greedyService.greedyDbOperation(complexity);
        return ResponseEntity.ok(result);
    }

    /**
     * 💀 BURN EVERYTHING — CPU + Memory + DB all at once
     */
    @GetMapping("/burn-everything")
    public ResponseEntity<Map<String, Object>> burnEverything() {
        Map<String, Object> result = greedyService.burnEverything();
        return ResponseEntity.ok(result);
    }
}
