package com.hackathon.dbconnection.controller;

import com.hackathon.dbconnection.model.Order;
import com.hackathon.dbconnection.model.User;
import com.hackathon.dbconnection.service.DatabaseService;
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
public class DbConnectionController {

    private final DatabaseService databaseService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "db-connection-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Slow query endpoint
     * JMeter should hit this with high concurrency to exhaust connection pool
     */
    @GetMapping("/users/slow-query")
    public ResponseEntity<Map<String, Object>> slowQuery() {
        long startTime = System.currentTimeMillis();

        List<User> users = databaseService.performSlowQuery();

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "slow-query");
        response.put("usersFound", users.size());
        response.put("durationMs", duration);
        response.put("warning", "This query holds DB connection for extended time!");

        log.info("Slow query completed in {}ms", duration);
        return ResponseEntity.ok(response);
    }

    /**
     * Complex join query
     */
    @GetMapping("/orders/complex-query")
    public ResponseEntity<Map<String, Object>> complexQuery(
            @RequestParam(defaultValue = "PENDING") String status) {

        long startTime = System.currentTimeMillis();

        List<Order> orders = databaseService.performComplexQuery(status);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "complex-query");
        response.put("status", status);
        response.put("ordersFound", orders.size());
        response.put("durationMs", duration);

        log.info("Complex query for status {} completed in {}ms", status, duration);
        return ResponseEntity.ok(response);
    }

    /**
     * Multiple sequential queries (N+1 problem)
     * This will quickly exhaust connection pool!
     */
    @GetMapping("/queries/multiple")
    public ResponseEntity<Map<String, Object>> multipleQueries(
            @RequestParam(defaultValue = "10") int count) {

        if (count > 50) {
            count = 50; // Limit to prevent complete lockup
        }

        Map<String, Object> result = databaseService.performMultipleQueries(count);

        log.info("Multiple queries ({}) completed", count);
        return ResponseEntity.ok(result);
    }

    /**
     * Long-running transaction
     * Holds connection for extended period
     */
    @PostMapping("/transaction/long")
    public ResponseEntity<Map<String, Object>> longTransaction(
            @RequestParam(defaultValue = "5") int durationSeconds) {

        if (durationSeconds > 30) {
            durationSeconds = 30;
        }

        Map<String, Object> result = databaseService.performLongTransaction(durationSeconds);

        log.warn("Long transaction held connection for {}s", durationSeconds);
        return ResponseEntity.ok(result);
    }

    /**
     * Connection pool killer - hits all inefficient patterns
     * This is the KILLER endpoint for JMeter
     */
    @GetMapping("/connection-bomb")
    public ResponseEntity<Map<String, Object>> connectionBomb() {
        long startTime = System.currentTimeMillis();

        // Execute multiple inefficient operations
        List<User> users = databaseService.performSlowQuery();
        List<Order> orders = databaseService.performComplexQuery("PENDING");
        Map<String, Object> multipleResults = databaseService.performMultipleQueries(5);

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("operation", "connection-bomb");
        response.put("usersQueried", users.size());
        response.put("ordersQueried", orders.size());
        response.put("additionalQueries", multipleResults.get("queriesExecuted"));
        response.put("durationMs", duration);
        response.put("warning", "This endpoint exhausts connection pool rapidly!");

        log.warn("CONNECTION BOMB executed in {}ms - pool exhaustion likely!", duration);
        return ResponseEntity.ok(response);
    }

    /**
     * Initialize test data
     */
    @PostMapping("/init-data")
    public ResponseEntity<Map<String, Object>> initData(
            @RequestParam(defaultValue = "100") int userCount,
            @RequestParam(defaultValue = "5") int ordersPerUser) {

        Map<String, Object> result = databaseService.createTestData(userCount, ordersPerUser);

        log.info("Test data initialized");
        return ResponseEntity.ok(result);
    }

    /**
     * Get database statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = databaseService.getDatabaseStats();

        log.info("Database stats retrieved");
        return ResponseEntity.ok(stats);
    }
}
