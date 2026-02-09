package com.hackathon.analyzer.discovery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Service Discovery", description = "Dynamic service registration and discovery endpoints")
public class ServiceDiscoveryController {

    private final ServiceDiscoveryService discoveryService;

    @Operation(summary = "List All Services",
               description = "Get all registered services including auto-discovered and manually registered ones")
    @ApiResponse(responseCode = "200", description = "List of all services")
    @GetMapping
    public ResponseEntity<Collection<ServiceInfo>> listServices() {
        return ResponseEntity.ok(discoveryService.getAllServices());
    }

    @Operation(summary = "Get Service Details",
               description = "Get details of a specific service by name")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service found"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @GetMapping("/{serviceName}")
    public ResponseEntity<ServiceInfo> getService(
            @Parameter(description = "Service name") @PathVariable String serviceName) {
        return discoveryService.getService(serviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Register Service",
               description = "Manually register a new service for monitoring. " +
                           "The service must have actuator/prometheus endpoint enabled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerService(@RequestBody ServiceRegistrationRequest request) {
        log.info("Service registration request received: {} at {}", request.getName(), request.getUrl());

        // Validate request
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Service name is required"
            ));
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Service URL is required"
            ));
        }

        // Normalize URL
        String url = request.getUrl().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        ServiceInfo registered = discoveryService.registerService(
                request.getName().trim(),
                url,
                "REGISTER"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Service registered successfully");
        response.put("service", registered);

        log.info("Service registered successfully: {}", registered.getName());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Unregister Service",
               description = "Remove a service from monitoring")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service unregistered"),
        @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @DeleteMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> unregisterService(
            @Parameter(description = "Service name to unregister") @PathVariable String serviceName) {

        boolean removed = discoveryService.unregisterService(serviceName);

        if (removed) {
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Service unregistered: " + serviceName
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Trigger Port Scan",
               description = "Manually trigger a port scan to discover new services")
    @ApiResponse(responseCode = "200", description = "Scan triggered")
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan() {
        log.info("Manual port scan triggered");

        int beforeCount = discoveryService.getAllServices().size();
        discoveryService.scanForServices();
        int afterCount = discoveryService.getAllServices().size();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Port scan completed",
            "servicesDiscovered", afterCount - beforeCount,
            "totalServices", afterCount
        ));
    }

    @Operation(summary = "Get Healthy Services",
               description = "Get only services that are currently healthy and responding")
    @ApiResponse(responseCode = "200", description = "List of healthy services")
    @GetMapping("/healthy")
    public ResponseEntity<Collection<ServiceInfo>> getHealthyServices() {
        return ResponseEntity.ok(discoveryService.getHealthyServices());
    }
}
