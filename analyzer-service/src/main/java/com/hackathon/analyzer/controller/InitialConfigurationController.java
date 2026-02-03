package com.hackathon.analyzer.controller;

import com.hackathon.analyzer.model.ResourceRecommendation;
import com.hackathon.analyzer.service.InitialConfigurationService;
import com.hackathon.analyzer.service.InitialConfigurationService.ServiceType;
import com.hackathon.analyzer.service.InitialConfigurationService.LoadLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for generating initial configurations for new projects
 */
@RestController
@RequestMapping("/api/initial-config")
@RequiredArgsConstructor
@Tag(name = "Initial Configuration", description = "Generate baseline configurations for new projects without metrics")
public class InitialConfigurationController {

    private final InitialConfigurationService initialConfigurationService;

    /**
     * Generate initial configuration for a new service
     *
     * @param serviceName Name of the new service
     * @param serviceType Type of service (WEB_API, BACKGROUND_JOB, etc.)
     * @param expectedLoad Expected load level (LOW, MEDIUM, HIGH, VERY_HIGH)
     * @return ResourceRecommendation with baseline configuration
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate initial configuration",
               description = "Creates baseline resource configuration for new projects without existing metrics")
    public ResponseEntity<ResourceRecommendation> generateInitialConfiguration(
            @RequestParam String serviceName,
            @RequestParam(defaultValue = "MICROSERVICE") ServiceType serviceType,
            @RequestParam(defaultValue = "MEDIUM") LoadLevel expectedLoad) {

        ResourceRecommendation recommendation = initialConfigurationService
                .generateInitialConfiguration(serviceName, serviceType, expectedLoad);

        return ResponseEntity.ok(recommendation);
    }

    /**
     * Get available service types
     */
    @GetMapping("/service-types")
    @Operation(summary = "List service types", description = "Get all available service types for configuration")
    public ResponseEntity<ServiceType[]> getServiceTypes() {
        return ResponseEntity.ok(ServiceType.values());
    }

    /**
     * Get available load levels
     */
    @GetMapping("/load-levels")
    @Operation(summary = "List load levels", description = "Get all available load levels")
    public ResponseEntity<LoadLevel[]> getLoadLevels() {
        return ResponseEntity.ok(LoadLevel.values());
    }

    /**
     * Generate configuration with detailed parameters
     */
    @PostMapping("/generate/detailed")
    @Operation(summary = "Generate detailed configuration",
               description = "Generate configuration with custom parameters")
    public ResponseEntity<ConfigurationGuideResponse> generateDetailedConfiguration(
            @RequestBody ConfigurationRequest request) {

        ResourceRecommendation recommendation = initialConfigurationService
                .generateInitialConfiguration(
                        request.getServiceName(),
                        request.getServiceType(),
                        request.getExpectedLoad());

        ConfigurationGuideResponse response = new ConfigurationGuideResponse();
        response.setRecommendation(recommendation);
        response.setNextSteps(getNextSteps(request.getServiceType()));
        response.setMonitoringGuidelines(getMonitoringGuidelines());

        return ResponseEntity.ok(response);
    }

    private String[] getNextSteps(ServiceType type) {
        return new String[]{
                "1. Apply the generated configuration to your project",
                "2. Deploy to a test environment",
                "3. Run load tests to generate real metrics",
                "4. Monitor for 24-48 hours to collect baseline data",
                "5. Use /api/analyze endpoint to get optimized recommendations based on real metrics",
                "6. Iteratively tune based on actual performance data"
        };
    }

    private String[] getMonitoringGuidelines() {
        return new String[]{
                "Monitor CPU usage - should stay below 70% under normal load",
                "Monitor memory usage - watch for steady growth (memory leaks)",
                "Monitor response times - P95 should be within SLA",
                "Monitor error rates - should be < 1%",
                "Monitor database connection pool - active connections should not max out",
                "Set up alerts for resource exhaustion scenarios"
        };
    }

    // Request/Response DTOs
    @lombok.Data
    public static class ConfigurationRequest {
        private String serviceName;
        private ServiceType serviceType = ServiceType.MICROSERVICE;
        private LoadLevel expectedLoad = LoadLevel.MEDIUM;
        private boolean includeMonitoringSetup = true;
    }

    @lombok.Data
    public static class ConfigurationGuideResponse {
        private ResourceRecommendation recommendation;
        private String[] nextSteps;
        private String[] monitoringGuidelines;
    }
}
