package com.hackathon.dbconnection.service;

import com.hackathon.dbconnection.model.Order;
import com.hackathon.dbconnection.model.User;
import com.hackathon.dbconnection.repository.OrderRepository;
import com.hackathon.dbconnection.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final Random random = new Random();

    /**
     * Slow query that holds DB connection for a long time
     * Simulates inefficient database access
     */
    @Transactional(readOnly = true)
    public List<User> performSlowQuery() {
        log.debug("Executing slow query...");

        // Fetch all users
        List<User> users = userRepository.findAll();

        // Simulate slow processing while holding connection
        try {
            Thread.sleep(100 + random.nextInt(200)); // 100-300ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Additional queries while in transaction
        for (User user : users) {
            orderRepository.findByUser(user);
        }

        log.debug("Slow query completed, returned {} users", users.size());
        return users;
    }

    /**
     * Complex join query that holds connection
     */
    @Transactional(readOnly = true)
    public List<Order> performComplexQuery(String status) {
        log.debug("Executing complex query for status: {}", status);

        // This query uses JOIN FETCH and can be slow
        List<Order> orders = orderRepository.findActiveUserOrders(status);

        // Simulate processing delay
        try {
            Thread.sleep(50 + random.nextInt(150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.debug("Complex query completed, returned {} orders", orders.size());
        return orders;
    }

    /**
     * Multiple sequential queries - inefficient N+1 pattern
     */
    @Transactional(readOnly = true)
    public Map<String, Object> performMultipleQueries(int count) {
        log.debug("Executing {} sequential queries...", count);

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // Each iteration hits DB separately
            List<User> activeUsers = userRepository.findByActive(true);
            List<Order> pendingOrders = orderRepository.findByStatus("PENDING");

            Map<String, Object> result = new HashMap<>();
            result.put("activeUsers", activeUsers.size());
            result.put("pendingOrders", pendingOrders.size());
            results.add(result);

            // Small delay between queries
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("queriesExecuted", count);
        response.put("durationMs", duration);
        response.put("warning", "Inefficient N+1 query pattern holding connections!");

        log.debug("Multiple queries completed in {}ms", duration);
        return response;
    }

    /**
     * Long-running transaction
     */
    @Transactional
    public Map<String, Object> performLongTransaction(int durationSeconds) {
        log.debug("Starting long transaction for {}s...", durationSeconds);

        long startTime = System.currentTimeMillis();

        // Fetch some data
        List<User> users = userRepository.findAll();

        // Hold transaction open for specified duration
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Update some data at the end
        if (!users.isEmpty()) {
            User user = users.get(0);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        }

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("durationMs", duration);
        response.put("usersProcessed", users.size());
        response.put("warning", "Long transaction held DB connection for " + durationSeconds + " seconds!");

        log.warn("Long transaction completed in {}ms", duration);
        return response;
    }

    /**
     * Create test data
     */
    @Transactional
    public Map<String, Object> createTestData(int userCount, int ordersPerUser) {
        log.info("Creating test data: {} users, {} orders each", userCount, ordersPerUser);

        long startTime = System.currentTimeMillis();

        List<User> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            User user = User.builder()
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .firstName("First" + i)
                    .lastName("Last" + i)
                    .createdAt(Instant.now().minus(random.nextInt(365), ChronoUnit.DAYS))
                    .lastLoginAt(Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS))
                    .active(random.nextBoolean())
                    .metadata("Test user metadata " + i)
                    .build();

            users.add(userRepository.save(user));
        }

        int totalOrders = 0;
        for (User user : users) {
            for (int j = 0; j < ordersPerUser; j++) {
                Order order = Order.builder()
                        .user(user)
                        .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8))
                        .totalAmount(random.nextDouble() * 1000)
                        .status(random.nextBoolean() ? "COMPLETED" : "PENDING")
                        .orderDate(Instant.now().minus(random.nextInt(180), ChronoUnit.DAYS))
                        .orderDetails("Test order details for order " + j)
                        .build();

                orderRepository.save(order);
                totalOrders++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("usersCreated", users.size());
        response.put("ordersCreated", totalOrders);
        response.put("durationMs", duration);

        log.info("Test data created in {}ms", duration);
        return response;
    }

    /**
     * Get database statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDatabaseStats() {
        long userCount = userRepository.count();
        long orderCount = orderRepository.count();
        long activeUsers = userRepository.findByActive(true).size();
        long pendingOrders = orderRepository.findByStatus("PENDING").size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userCount);
        stats.put("totalOrders", orderCount);
        stats.put("activeUsers", activeUsers);
        stats.put("pendingOrders", pendingOrders);

        return stats;
    }

    /**
     * Background task that continuously pressures connection pool
     * Runs every 15 seconds, holding connections open to keep pool utilization high
     */
    @Scheduled(fixedDelay = 15000)
    public void backgroundConnectionPressure() {
        try {
            // Execute slow query (holds connection for 100-300ms)
            performSlowQuery();
            // Execute multiple sequential queries (holds connection longer)
            performMultipleQueries(5);
            log.debug("Background connection pressure applied - pool should be under load");
        } catch (Exception e) {
            log.debug("Background connection pressure task encountered: {}", e.getMessage());
        }
    }
}
