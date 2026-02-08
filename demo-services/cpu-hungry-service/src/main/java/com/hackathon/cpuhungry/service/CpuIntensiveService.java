package com.hackathon.cpuhungry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class CpuIntensiveService {

    private final Random random = new Random();

    /**
     * CPU-intensive hashing operation
     * Simulates heavy cryptographic work
     */
    public String performHashing(String input, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String result = input;

            // Perform multiple hashing iterations to consume CPU
            for (int i = 0; i < iterations; i++) {
                byte[] hash = md.digest(result.getBytes());
                result = bytesToHex(hash);
            }

            log.debug("Completed {} hashing iterations", iterations);
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * CPU-intensive prime number calculation
     * Simulates heavy mathematical computation
     */
    public List<Long> calculatePrimes(int limit) {
        List<Long> primes = new ArrayList<>();
        long startRange = random.nextInt(1000) + 2;
        long endRange = startRange + limit;

        for (long num = startRange; num <= endRange; num++) {
            if (isPrime(num)) {
                primes.add(num);
            }
        }

        log.debug("Found {} prime numbers between {} and {}", primes.size(), startRange, endRange);
        return primes;
    }

    /**
     * CPU-intensive Fibonacci calculation
     * Uses inefficient recursive algorithm intentionally
     */
    public long calculateFibonacci(int n) {
        if (n <= 1)
            return n;

        // Intentionally inefficient recursive implementation for CPU burn
        return calculateFibonacci(n - 1) + calculateFibonacci(n - 2);
    }

    /**
     * CPU-intensive matrix multiplication
     * Simulates heavy numerical computation
     */
    public double[][] multiplyMatrices(int size) {
        double[][] matrix1 = generateRandomMatrix(size);
        double[][] matrix2 = generateRandomMatrix(size);
        double[][] result = new double[size][size];

        // Matrix multiplication - O(n³) complexity
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    result[i][j] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }

        log.debug("Completed {}x{} matrix multiplication", size, size);
        return result;
    }

    /**
     * CPU-intensive string processing
     * Simulates heavy text processing workload
     */
    public String processText(String text, int complexity) {
        StringBuilder result = new StringBuilder(text);

        for (int i = 0; i < complexity; i++) {
            // Reverse, hash, and manipulate string multiple times
            result.reverse();
            String hashed = performHashing(result.toString(), 10);
            result = new StringBuilder(hashed);
        }

        return result.toString();
    }

    // Helper methods

    private boolean isPrime(long num) {
        if (num <= 1)
            return false;
        if (num <= 3)
            return true;
        if (num % 2 == 0 || num % 3 == 0)
            return false;

        for (long i = 5; i * i <= num; i += 6) {
            if (num % i == 0 || num % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private double[][] generateRandomMatrix(int size) {
        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = random.nextDouble() * 100;
            }
        }
        return matrix;
    }

    /**
     * Background task that continuously burns CPU
     * Runs every 10 seconds to keep process_cpu_usage high
     */
    @Scheduled(fixedDelay = 10000)
    public void backgroundCpuBurn() {
        // Perform fibonacci(35) - takes significant CPU
        calculateFibonacci(35);
        // Perform hashing iterations
        performHashing("background-burn-" + System.currentTimeMillis(), 2000);
        // Matrix multiplication
        multiplyMatrices(80);
        log.debug("Background CPU burn completed");
    }
}
