package com.hackathon.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI-powered optimizasyon önerileri üretir.
 * Anthropic SDK uyumlu proxy API kullanır (Antigravity).
 */
@Slf4j
@Service
public class GeminiInsightService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiInsightService(
            WebClient.Builder webClientBuilder,
            @Value("${ai.api-key:sk-4a2814af1df6436aa2bf93b1d800dc26}") String apiKey,
            @Value("${ai.base-url:http://127.0.0.1:8045}") String baseUrl,
            @Value("${ai.model:gemini-3-pro-high}") String model,
            @Value("${ai.enabled:true}") boolean enabled) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        log.info("AI Insight Service initialized - baseUrl: {}, model: {}, enabled: {}", baseUrl, model, enabled);
    }

    /**
     * API'nin yapılandırılıp yapılandırılmadığını kontrol eder.
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Kullanılan model bilgisini döndürür.
     */
    public String getModel() {
        return model;
    }

    /**
     * Servis metrikleri hakkında AI'dan optimizasyon raporu iste.
     */
    public String generateOptimizationInsight(String serviceName,
                                               double cpuUsagePercent,
                                               double memoryUsageMb,
                                               double monthlyCost,
                                               double estimatedSavings,
                                               double confidenceScore,
                                               Map<String, String> detectedIssues) {
        if (!isConfigured()) {
            log.warn("AI API key not configured — skipping AI insight");
            return null;
        }

        String prompt = buildOptimizationPrompt(serviceName, cpuUsagePercent, memoryUsageMb,
                monthlyCost, estimatedSavings, confidenceScore, detectedIssues);

        return callAnthropicAPI(prompt);
    }

    /**
     * Genel bir soru için AI'dan yanıt al.
     */
    public String generateInsight(String prompt) {
        if (!isConfigured()) {
            log.warn("AI API key not configured — skipping AI insight");
            return null;
        }
        return callAnthropicAPI(prompt);
    }

    private String callAnthropicAPI(String prompt) {
        try {
            log.info("Calling AI API with model: {}", model);

            // Anthropic Messages API format
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 1024,
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", prompt
                    )
                )
            );

            String response = webClient.post()
                .uri("/v1/messages")
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .onErrorResume(e -> {
                    log.error("AI API call failed: {}", e.getMessage());
                    return Mono.just("{\"error\":\"" + e.getMessage() + "\"}");
                })
                .block();

            log.debug("AI API response: {}", response);
            return extractTextFromAnthropicResponse(response);

        } catch (Exception e) {
            log.error("Error calling AI API: {}", e.getMessage(), e);
            return "AI insight temporarily unavailable: " + e.getMessage();
        }
    }

    private String buildOptimizationPrompt(String serviceName, double cpu,
                                            double memory, double cost,
                                            double savings, double confidence,
                                            Map<String, String> issues) {
        StringBuilder issuesText = new StringBuilder();
        if (issues != null && !issues.isEmpty()) {
            issues.forEach((key, value) ->
                issuesText.append("- ").append(key).append(": ").append(value).append("\n"));
        } else {
            issuesText.append("- No critical issues detected\n");
        }

        return String.format("""
            You are a Kubernetes resource optimization and cloud cost management expert.
            Analyze the following microservice metrics and write a concise optimization report in English.

            ## Service Information
            - Service Name: %s
            - CPU Usage: %.1f%%
            - Memory Usage: %.1f MB
            - Monthly Cost: $%.2f
            - Estimated Savings: $%.2f
            - Analysis Confidence Score: %.0f%%

            ## Detected Issues
            %s

            ## Report Format (maximum 5 points, short and concise):
            1. **Current Status:** (1 sentence summary)
            2. **Main Issue:** (if any, otherwise "No critical issues")
            3. **Recommendations:** (with specific CPU/Memory values, e.g., "Increase CPU limit from 200m to 350m")
            4. **Estimated Savings:** (monthly and yearly)
            5. **Priority:** (Low/Medium/High/Critical)

            Provide the response in markdown format.
            """, serviceName, cpu, memory, cost, savings, confidence * 100, issuesText);
    }

    private String extractTextFromAnthropicResponse(String jsonResponse) {
        try {
            // Check for error
            if (jsonResponse.contains("\"error\"")) {
                log.error("AI API returned error: {}", jsonResponse);
                return "AI API error: " + jsonResponse;
            }

            // Anthropic format: content[0].text
            // Simple JSON parsing without library
            int contentStart = jsonResponse.indexOf("\"content\":");
            if (contentStart == -1) {
                log.warn("No content field in response: {}", jsonResponse);
                return jsonResponse;
            }

            // Find "text": within content array
            int textStart = jsonResponse.indexOf("\"text\":", contentStart);
            if (textStart == -1) {
                // Try alternate format where text might be directly in content
                textStart = jsonResponse.indexOf("\"text\":");
            }

            if (textStart == -1) {
                log.warn("No text field in response: {}", jsonResponse);
                return jsonResponse;
            }

            // Find the opening quote of the text value
            int valueStart = jsonResponse.indexOf("\"", textStart + 7) + 1;

            // Find the closing quote, handling escaped quotes
            int valueEnd = valueStart;
            while (valueEnd < jsonResponse.length()) {
                valueEnd = jsonResponse.indexOf("\"", valueEnd);
                if (valueEnd == -1) break;

                // Count backslashes before the quote
                int backslashCount = 0;
                int checkPos = valueEnd - 1;
                while (checkPos >= valueStart && jsonResponse.charAt(checkPos) == '\\') {
                    backslashCount++;
                    checkPos--;
                }

                // If even number of backslashes, this is the real end quote
                if (backslashCount % 2 == 0) {
                    break;
                }
                valueEnd++;
            }

            if (valueEnd <= valueStart) {
                return jsonResponse;
            }

            String extracted = jsonResponse.substring(valueStart, valueEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\t", "\t")
                    .replace("\\\\", "\\");

            log.info("Successfully extracted AI response ({} chars)", extracted.length());
            return extracted;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            return jsonResponse;
        }
    }
}
