package com.hackathon.codegen.service;

import com.hackathon.codegen.azuredevops.AzureDevOpsService;
import com.hackathon.codegen.generator.FileGeneratorService;
import com.hackathon.codegen.model.GeneratedArtifacts;
import com.hackathon.codegen.model.ResourceRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationService {

    private final FileGeneratorService fileGenerator;
    private final AzureDevOpsService azureDevOpsService;

    /**
     * Generate configuration files and optionally create Azure DevOps PR
     */
    public GeneratedArtifacts generateAndDeploy(ResourceRecommendation recommendation, boolean createPR) {
        log.info("Starting code generation for service: {}", recommendation.getServiceName());

        try {
            // Step 1: Generate all configuration files
            GeneratedArtifacts artifacts = fileGenerator.generateArtifacts(recommendation);

            // Step 2: Save files locally (for demo/testing)
            saveFilesLocally(artifacts);

            // Step 3: Create Azure DevOps branch and commit (if configured)
            if (createPR && azureDevOpsService.isConfigured()) {
                String branchName = azureDevOpsService.createBranchAndCommit(artifacts);
                artifacts.setAzureDevOpsBranch(branchName);

                // Step 4: Create Pull Request
                String prUrl = azureDevOpsService.createPullRequest(branchName, artifacts);
                artifacts.setPullRequestUrl(prUrl);

                log.info("Pull Request created: {}", prUrl);
            } else if (createPR) {
                log.warn("Azure DevOps not configured. Files generated locally only.");
                artifacts.setPullRequestUrl("Azure DevOps PAT not configured - files saved locally");
            }

            artifacts.setSuccess(true);
            log.info("Code generation completed successfully for {}", recommendation.getServiceName());

            return artifacts;

        } catch (Exception e) {
            log.error("Error during code generation: {}", e.getMessage(), e);

            return GeneratedArtifacts.builder()
                    .serviceName(recommendation.getServiceName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Save generated files to local filesystem (for testing/demo)
     */
    private void saveFilesLocally(GeneratedArtifacts artifacts) {
        log.info("Saving generated files locally for service: {}", artifacts.getServiceName());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String outputDir = String.format("./output/%s/%s", artifacts.getServiceName(), timestamp);

        try {
            Path basePath = Paths.get(outputDir);
            Files.createDirectories(basePath);

            for (GeneratedArtifacts.GeneratedFile file : artifacts.getFiles()) {
                Path filePath = basePath.resolve(file.getFileName());
                Files.writeString(filePath, file.getContent());

                log.debug("Saved file: {}", filePath);
            }

            log.info("All files saved to: {}", outputDir);

        } catch (IOException e) {
            log.error("Error saving files locally: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate files only (no Azure DevOps integration)
     */
    public GeneratedArtifacts generateOnly(ResourceRecommendation recommendation) {
        return generateAndDeploy(recommendation, false);
    }
}
