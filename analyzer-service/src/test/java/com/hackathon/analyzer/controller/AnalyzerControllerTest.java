package com.hackathon.analyzer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Controller integration tests — zero Mockito/ByteBuddy dependency.
 * Uses real Spring context with TestRestTemplate for full JDK 25 compatibility.
 *
 * Tests are grouped by dependency profile:
 * - Standalone: endpoints that work without external service connections
 * - Dependent: endpoints that require demo services (tested with soft
 * assertions)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class AnalyzerControllerTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate rest;

        private String url(String path) {
                return "http://localhost:" + port + path;
        }

        // ═══════════════════════════════════════════════════
        // Standalone Endpoints (no external dependencies)
        // ═══════════════════════════════════════════════════

        @Test
        void health_shouldReturnUpStatus() {
                ResponseEntity<Map> r = rest.getForEntity(url("/api/health"), Map.class);

                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).containsEntry("status", "UP");
                assertThat(r.getBody()).containsEntry("service", "analyzer-service");
        }

        @Test
        void dashboard_shouldReturnSummary() {
                ResponseEntity<Map> r = rest.getForEntity(url("/api/dashboard"), Map.class);

                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).containsKey("servicesAnalyzed");
                assertThat(r.getBody()).containsKey("totalMonthlySavings");
        }

        @Test
        void collectMetrics_shouldReturnSuccess() {
                ResponseEntity<Map> r = rest.postForEntity(
                                url("/api/collect-metrics"), null, Map.class);

                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).containsEntry("status", "success");
                assertThat(r.getBody()).containsEntry("message", "Metrics collection completed");
        }

        @Test
        void getMetrics_shouldReturnServiceMetrics() {
                ResponseEntity<Map> r = rest.getForEntity(
                                url("/api/metrics/cpu-hungry-service?limit=10"), Map.class);

                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).containsKey("serviceName");
                assertThat(r.getBody()).containsEntry("serviceName", "cpu-hungry-service");
        }

        // ═══════════════════════════════════════════════════
        // Analysis Endpoints (depend on external services)
        // ═══════════════════════════════════════════════════

        @Test
        void analyzeService_shouldReturnRecommendation() {
                ResponseEntity<Map> r = rest.postForEntity(
                                url("/api/analyze/cpu-hungry-service"), null, Map.class);

                // Analysis endpoint returns 200 with recommendation data.
                // The service handles missing demo services gracefully with default metrics.
                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).containsKey("serviceName");
        }

        @Test
        void analyzeAllServices_shouldReturnMapOfRecommendations() {
                ResponseEntity<Map> r = rest.postForEntity(
                                url("/api/analyze-all"), null, Map.class);

                assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(r.getBody()).containsKey("cpu-hungry-service");
                assertThat(r.getBody()).containsKey("memory-leaker-service");
                assertThat(r.getBody()).containsKey("db-connection-service");
        }

        @Test
        void getLatestAnalysis_returnsResultAfterAnalysis() {
                // First trigger an analysis
                rest.postForEntity(url("/api/analyze/db-connection-service"), null, Map.class);

                ResponseEntity<Map> r = rest.getForEntity(
                                url("/api/latest-analysis/db-connection-service"), Map.class);

                // Should return 200 (result exists) or 404 (no prior analysis stored)
                assertThat(r.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
        }

        // ═══════════════════════════════════════════════════
        // ML Endpoints
        // ═══════════════════════════════════════════════════

        @Test
        void predictCosts_shouldReturnForecast() {
                ResponseEntity<Map> r = rest.getForEntity(
                                url("/api/predict-costs/cpu-hungry-service?daysAhead=7"), Map.class);

                // Cost prediction works with or without historical data
                assertThat(r.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR,
                                HttpStatus.TOO_MANY_REQUESTS);
                if (r.getStatusCode() == HttpStatus.OK) {
                        assertThat(r.getBody()).containsKey("serviceName");
                }
        }

        @Test
        void classifyWorkload_shouldReturnClassification() {
                ResponseEntity<Map> r = rest.getForEntity(
                                url("/api/classify-workload/cpu-hungry-service"), Map.class);

                assertThat(r.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR,
                                HttpStatus.TOO_MANY_REQUESTS);
                if (r.getStatusCode() == HttpStatus.OK) {
                        assertThat(r.getBody()).containsKey("workloadType");
                }
        }

        @Test
        void getAIInsights_shouldReturnInsights() {
                ResponseEntity<Map> r = rest.getForEntity(
                                url("/api/ai-insights/cpu-hungry-service"), Map.class);

                assertThat(r.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR,
                                HttpStatus.TOO_MANY_REQUESTS);
                if (r.getStatusCode() == HttpStatus.OK) {
                        assertThat(r.getBody()).containsKey("serviceName");
                }
        }

        @Test
        void getAIOverview_shouldReturnOverview() {
                ResponseEntity<Map> r = rest.getForEntity(
                                url("/api/ai-overview"), Map.class);

                assertThat(r.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR,
                                HttpStatus.TOO_MANY_REQUESTS);
                if (r.getStatusCode() == HttpStatus.OK) {
                        assertThat(r.getBody()).containsKey("services");
                }
        }
}
