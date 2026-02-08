package com.hackathon.analyzer;

import com.hackathon.analyzer.controller.AnalyzerController;
import com.hackathon.analyzer.controller.AuthController;
import com.hackathon.analyzer.security.JwtTokenProvider;
import com.hackathon.analyzer.service.ResourceAnalyzerService;
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
 * Integration tests that boot the full Spring context and verify
 * endpoint availability, security wiring, and service orchestration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class AnalyzerServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AnalyzerController analyzerController;

    @Autowired
    private AuthController authController;

    @Autowired
    private ResourceAnalyzerService analyzerService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void contextLoads() {
        assertThat(analyzerController).isNotNull();
        assertThat(authController).isNotNull();
        assertThat(analyzerService).isNotNull();
        assertThat(jwtTokenProvider).isNotNull();
    }

    @Test
    void healthEndpoint_shouldReturnOk() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("service", "analyzer-service");
    }

    @Test
    void authTokenEndpoint_shouldIssueToken() {
        Map<String, String> credentials = Map.of("username", "demo", "password", "demo");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/token", credentials, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat(response.getBody()).containsEntry("type", "Bearer");
        assertThat(response.getBody()).containsEntry("username", "demo");
    }

    @Test
    void dashboardEndpoint_shouldReturnSummary() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/dashboard", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("servicesAnalyzed");
        assertThat(response.getBody()).containsKey("totalMonthlySavings");
    }

    @Test
    void analyzeEndpoint_shouldReturnRecommendation() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/analyze/cpu-hungry-service", null, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("serviceName");
        assertThat(response.getBody()).containsKey("confidenceScore");
    }

    @Test
    void jwtTokenProvider_shouldGenerateValidTokens() {
        String token = jwtTokenProvider.generateToken("testuser");
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
    }
}
