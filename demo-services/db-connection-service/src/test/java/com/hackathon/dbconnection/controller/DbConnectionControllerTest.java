package com.hackathon.dbconnection.controller;

import com.hackathon.dbconnection.model.Order;
import com.hackathon.dbconnection.model.User;
import com.hackathon.dbconnection.service.DatabaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DbConnectionController.class)
class DbConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        List<User> mockUsers = Arrays.asList(
                User.builder().id(1L).username("user1").email("user1@test.com").build(),
                User.builder().id(2L).username("user2").email("user2@test.com").build()
        );

        List<Order> mockOrders = Arrays.asList(
                Order.builder().id(1L).orderNumber("ORD-001").status("PENDING").build(),
                Order.builder().id(2L).orderNumber("ORD-002").status("COMPLETED").build()
        );

        when(databaseService.performSlowQuery()).thenReturn(mockUsers);
        when(databaseService.performComplexQuery(anyString())).thenReturn(mockOrders);

        Map<String, Object> multipleResult = new HashMap<>();
        multipleResult.put("queriesExecuted", 5);
        multipleResult.put("totalDurationMs", 1000L);
        when(databaseService.performMultipleQueries(anyInt())).thenReturn(multipleResult);

        Map<String, Object> transactionResult = new HashMap<>();
        transactionResult.put("operation", "long-transaction");
        when(databaseService.performLongTransaction(anyInt())).thenReturn(transactionResult);

        Map<String, Object> statsResult = new HashMap<>();
        statsResult.put("totalUsers", 100);
        statsResult.put("totalOrders", 500);
        when(databaseService.getDatabaseStats()).thenReturn(statsResult);

        Map<String, Object> initResult = new HashMap<>();
        initResult.put("usersCreated", 100);
        initResult.put("ordersCreated", 500);
        when(databaseService.createTestData(anyInt(), anyInt())).thenReturn(initResult);
    }

    @Test
    void health_shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("db-connection-service"));
    }

    @Test
    void slowQuery_shouldReturnUsers() throws Exception {
        mockMvc.perform(get("/api/users/slow-query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("slow-query"))
                .andExpect(jsonPath("$.usersFound").value(2));

        verify(databaseService, times(1)).performSlowQuery();
    }

    @Test
    void complexQuery_shouldReturnOrders() throws Exception {
        mockMvc.perform(get("/api/orders/complex-query")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("complex-query"))
                .andExpect(jsonPath("$.ordersFound").value(2));

        verify(databaseService, times(1)).performComplexQuery("PENDING");
    }

    @Test
    void multipleQueries_shouldExecuteMultiple() throws Exception {
        mockMvc.perform(get("/api/queries/multiple")
                        .param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queriesExecuted").value(5));

        verify(databaseService, times(1)).performMultipleQueries(10);
    }

    @Test
    void longTransaction_shouldExecuteTransaction() throws Exception {
        mockMvc.perform(post("/api/transaction/long")
                        .param("durationSeconds", "5"))
                .andExpect(status().isOk());

        verify(databaseService, times(1)).performLongTransaction(5);
    }

    @Test
    void connectionBomb_shouldExecuteAllOperations() throws Exception {
        mockMvc.perform(get("/api/connection-bomb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("connection-bomb"))
                .andExpect(jsonPath("$.usersQueried").value(2))
                .andExpect(jsonPath("$.ordersQueried").value(2));

        verify(databaseService, times(1)).performSlowQuery();
        verify(databaseService, times(1)).performComplexQuery("PENDING");
        verify(databaseService, times(1)).performMultipleQueries(5);
    }

    @Test
    void getStats_shouldReturnStatistics() throws Exception {
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.totalOrders").value(500));

        verify(databaseService, times(1)).getDatabaseStats();
    }

    @Test
    void initData_shouldCreateTestData() throws Exception {
        mockMvc.perform(post("/api/init-data")
                        .param("userCount", "100")
                        .param("ordersPerUser", "5"))
                .andExpect(status().isOk());

        verify(databaseService, times(1)).createTestData(100, 5);
    }
}
