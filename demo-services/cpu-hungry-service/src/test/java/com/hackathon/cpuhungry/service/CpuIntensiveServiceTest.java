package com.hackathon.cpuhungry.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CpuIntensiveServiceTest {

    private CpuIntensiveService cpuIntensiveService;

    @BeforeEach
    void setUp() {
        cpuIntensiveService = new CpuIntensiveService();
    }

    @Test
    void performHashing_shouldReturnHashedString() {
        String result = cpuIntensiveService.performHashing("test", 10);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(64); // SHA-256 hex string length
    }

    @Test
    void calculatePrimes_shouldReturnPrimeNumbers() {
        List<Long> primes = cpuIntensiveService.calculatePrimes(100);

        assertThat(primes).isNotEmpty();
        assertThat(primes).allMatch(p -> p > 0);
    }

    @Test
    void calculateFibonacci_shouldReturnCorrectValue() {
        assertThat(cpuIntensiveService.calculateFibonacci(0)).isEqualTo(0);
        assertThat(cpuIntensiveService.calculateFibonacci(1)).isEqualTo(1);
        assertThat(cpuIntensiveService.calculateFibonacci(10)).isEqualTo(55);
    }

    @Test
    void multiplyMatrices_shouldReturnMatrix() {
        double[][] result = cpuIntensiveService.multiplyMatrices(10);

        assertThat(result).hasDimensions(10, 10);
    }

    @Test
    void processText_shouldReturnProcessedString() {
        String result = cpuIntensiveService.processText("test", 5);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }
}
