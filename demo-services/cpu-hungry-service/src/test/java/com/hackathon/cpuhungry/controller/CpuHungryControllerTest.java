package com.hackathon.cpuhungry.controller;

import com.hackathon.cpuhungry.service.CpuIntensiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CpuHungryController.class)
class CpuHungryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CpuIntensiveService cpuIntensiveService;

    @BeforeEach
    void setUp() {
        when(cpuIntensiveService.performHashing(anyString(), anyInt()))
                .thenReturn("abc123hash");
        when(cpuIntensiveService.calculatePrimes(anyInt()))
                .thenReturn(Arrays.asList(2L, 3L, 5L, 7L, 11L));
        when(cpuIntensiveService.calculateFibonacci(anyInt()))
                .thenReturn(55L);
        when(cpuIntensiveService.multiplyMatrices(anyInt()))
                .thenReturn(new double[10][10]);
        when(cpuIntensiveService.processText(anyString(), anyInt()))
                .thenReturn("processed-text");
    }

    @Test
    void health_shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("cpu-hungry-service"));
    }

    @Test
    void performHashing_shouldReturnHashResult() throws Exception {
        mockMvc.perform(post("/api/hash")
                        .param("input", "test")
                        .param("iterations", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("hashing"))
                .andExpect(jsonPath("$.iterations").value(100))
                .andExpect(jsonPath("$.durationMs").exists());

        verify(cpuIntensiveService, times(1)).performHashing("test", 100);
    }

    @Test
    void calculatePrimes_shouldReturnPrimeNumbers() throws Exception {
        mockMvc.perform(get("/api/primes")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("prime-calculation"))
                .andExpect(jsonPath("$.primesFound").value(5));

        verify(cpuIntensiveService, times(1)).calculatePrimes(100);
    }

    @Test
    void calculateFibonacci_shouldReturnFibonacciNumber() throws Exception {
        mockMvc.perform(get("/api/fibonacci")
                        .param("n", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("fibonacci"))
                .andExpect(jsonPath("$.n").value(10))
                .andExpect(jsonPath("$.result").value(55));

        verify(cpuIntensiveService, times(1)).calculateFibonacci(10);
    }

    @Test
    void multiplyMatrices_shouldReturnMatrixResult() throws Exception {
        mockMvc.perform(get("/api/matrix")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("matrix-multiplication"))
                .andExpect(jsonPath("$.matrixSize").value("50x50"));

        verify(cpuIntensiveService, times(1)).multiplyMatrices(50);
    }

    @Test
    void burnCpu_shouldExecuteAllOperations() throws Exception {
        mockMvc.perform(get("/api/burn-cpu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("combined-cpu-burn"))
                .andExpect(jsonPath("$.hashCompleted").value(true))
                .andExpect(jsonPath("$.primesFound").value(5));

        verify(cpuIntensiveService, times(1)).performHashing(anyString(), anyInt());
        verify(cpuIntensiveService, times(1)).calculatePrimes(anyInt());
        verify(cpuIntensiveService, times(1)).calculateFibonacci(anyInt());
        verify(cpuIntensiveService, times(1)).multiplyMatrices(anyInt());
    }
}
