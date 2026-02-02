package com.hackathon.codegen.controller;

import com.hackathon.codegen.azuredevops.AzureDevOpsService;
import com.hackathon.codegen.model.GeneratedArtifacts;
import com.hackathon.codegen.model.ResourceRecommendation;
import com.hackathon.codegen.service.CodeGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CodeGeneratorController {

    private final CodeGenerationService codeGenerationService;
    private final AzureDevOpsService azureDevOpsService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "code-generator-service");
        response.put("azureDevOpsConfigured", String.valueOf(azureDevOpsService.isConfigured()));
        return ResponseEntity.ok(response);
    }

    /**
     * Generate configuration files only (no PR)
     */
    @PostMapping("/generate")
    public ResponseEntity<GeneratedArtifacts> generateFiles(
            @RequestBody ResourceRecommendation recommendation) {

        log.info("Received code generation request for service: {}", recommendation.getServiceName());

        GeneratedArtifacts artifacts = codeGenerationService.generateOnly(recommendation);

        if (artifacts.getSuccess()) {
            log.info("Code generation successful for {}", recommendation.getServiceName());
            return ResponseEntity.ok(artifacts);
        } else {
            log.error("Code generation failed: {}", artifacts.getErrorMessage());
            return ResponseEntity.internalServerError().body(artifacts);
        }
    }

    /**
     * Generate files AND create Azure DevOps Pull Request
     */
    @PostMapping("/generate-and-pr")
    public ResponseEntity<GeneratedArtifacts> generateAndCreatePR(
            @RequestBody ResourceRecommendation recommendation) {

        log.info("Received generate-and-PR request for service: {}", recommendation.getServiceName());

        GeneratedArtifacts artifacts = codeGenerationService.generateAndDeploy(recommendation, true);

        if (artifacts.getSuccess()) {
            log.info("Code generation and PR creation successful for {}", recommendation.getServiceName());
            return ResponseEntity.ok(artifacts);
        } else {
            log.error("Code generation/PR failed: {}", artifacts.getErrorMessage());
            return ResponseEntity.internalServerError().body(artifacts);
        }
    }

    /**
     * Get Azure DevOps configuration status
     */
    @GetMapping("/azure-devops/status")
    public ResponseEntity<Map<String, Object>> getAzureDevOpsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", azureDevOpsService.isConfigured());
        status.put("repositoryUrl", azureDevOpsService.getRepositoryUrl());

        return ResponseEntity.ok(status);
    }

    /**
     * Preview generated files without creating PR
     */
    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewGeneration(
            @RequestBody ResourceRecommendation recommendation) {

        log.info("Generating preview for service: {}", recommendation.getServiceName());

        GeneratedArtifacts artifacts = codeGenerationService.generateOnly(recommendation);

        Map<String, Object> preview = new HashMap<>();
        preview.put("serviceName", artifacts.getServiceName());
        preview.put("fileCount", artifacts.getFiles().size());
        preview.put("files", artifacts.getFiles());

        return ResponseEntity.ok(preview);
    }
}
