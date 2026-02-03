package com.hackathon.codegen.service;

import com.hackathon.codegen.azuredevops.AzureDevOpsService;
import com.hackathon.codegen.generator.FileGeneratorService;
import com.hackathon.codegen.model.GeneratedArtifacts;
import com.hackathon.codegen.model.ResourceRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeGenerationServiceTest {

    @Mock
    private FileGeneratorService fileGenerator;

    @Mock
    private AzureDevOpsService azureDevOpsService;

    @InjectMocks
    private CodeGenerationService codeGenerationService;

    private ResourceRecommendation mockRecommendation;
    private GeneratedArtifacts mockArtifacts;

    @BeforeEach
    void setUp() {
        mockRecommendation = ResourceRecommendation.builder()
                .serviceName("test-service")
                .build();

        mockArtifacts = GeneratedArtifacts.builder()
                .serviceName("test-service")
                .files(new ArrayList<>())
                .build();
    }

    @Test
    void generateOnly_shouldGenerateFilesWithoutPR() {
        when(fileGenerator.generateArtifacts(any(ResourceRecommendation.class)))
                .thenReturn(mockArtifacts);

        GeneratedArtifacts result = codeGenerationService.generateOnly(mockRecommendation);

        assertThat(result).isNotNull();
        assertThat(result.getServiceName()).isEqualTo("test-service");
        assertThat(result.getSuccess()).isTrue();

        verify(fileGenerator, times(1)).generateArtifacts(any(ResourceRecommendation.class));
        verify(azureDevOpsService, never()).createBranchAndCommit(any());
        verify(azureDevOpsService, never()).createPullRequest(anyString(), any());
    }

    @Test
    void generateAndDeploy_withPR_shouldCreatePullRequest() {
        when(fileGenerator.generateArtifacts(any(ResourceRecommendation.class)))
                .thenReturn(mockArtifacts);
        when(azureDevOpsService.isConfigured()).thenReturn(true);
        when(azureDevOpsService.createBranchAndCommit(any(GeneratedArtifacts.class)))
                .thenReturn("feature/optimize-test-service");
        when(azureDevOpsService.createPullRequest(anyString(), any(GeneratedArtifacts.class)))
                .thenReturn("https://dev.azure.com/pr/123");

        GeneratedArtifacts result = codeGenerationService.generateAndDeploy(mockRecommendation, true);

        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getAzureDevOpsBranch()).isEqualTo("feature/optimize-test-service");
        assertThat(result.getPullRequestUrl()).isEqualTo("https://dev.azure.com/pr/123");

        verify(fileGenerator, times(1)).generateArtifacts(any(ResourceRecommendation.class));
        verify(azureDevOpsService, times(1)).createBranchAndCommit(any());
        verify(azureDevOpsService, times(1)).createPullRequest(anyString(), any());
    }

    @Test
    void generateAndDeploy_withPRButNotConfigured_shouldGenerateOnly() {
        when(fileGenerator.generateArtifacts(any(ResourceRecommendation.class)))
                .thenReturn(mockArtifacts);
        when(azureDevOpsService.isConfigured()).thenReturn(false);

        GeneratedArtifacts result = codeGenerationService.generateAndDeploy(mockRecommendation, true);

        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getPullRequestUrl()).contains("not configured");

        verify(fileGenerator, times(1)).generateArtifacts(any(ResourceRecommendation.class));
        verify(azureDevOpsService, never()).createBranchAndCommit(any());
    }

    @Test
    void generateAndDeploy_whenException_shouldReturnError() {
        when(fileGenerator.generateArtifacts(any(ResourceRecommendation.class)))
                .thenThrow(new RuntimeException("Test exception"));

        GeneratedArtifacts result = codeGenerationService.generateAndDeploy(mockRecommendation, false);

        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Test exception");

        verify(fileGenerator, times(1)).generateArtifacts(any(ResourceRecommendation.class));
    }
}
