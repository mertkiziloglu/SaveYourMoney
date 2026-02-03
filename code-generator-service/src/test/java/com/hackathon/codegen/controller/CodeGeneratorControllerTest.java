package com.hackathon.codegen.controller;

import com.hackathon.codegen.azuredevops.AzureDevOpsService;
import com.hackathon.codegen.model.GeneratedArtifacts;
import com.hackathon.codegen.model.ResourceRecommendation;
import com.hackathon.codegen.service.CodeGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CodeGeneratorController.class)
class CodeGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CodeGenerationService codeGenerationService;

    @MockBean
    private AzureDevOpsService azureDevOpsService;

    private ResourceRecommendation mockRecommendation;
    private GeneratedArtifacts mockArtifacts;

    @BeforeEach
    void setUp() {
        mockRecommendation = ResourceRecommendation.builder()
                .serviceName("test-service")
                .build();

        mockArtifacts = GeneratedArtifacts.builder()
                .serviceName("test-service")
                .success(true)
                .files(new ArrayList<>())
                .build();
    }

    @Test
    void health_shouldReturnHealthStatus() throws Exception {
        when(azureDevOpsService.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("code-generator-service"))
                .andExpect(jsonPath("$.azureDevOpsConfigured").value("true"));
    }

    @Test
    void generateFiles_shouldReturnArtifacts() throws Exception {
        when(codeGenerationService.generateOnly(any(ResourceRecommendation.class)))
                .thenReturn(mockArtifacts);

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"test-service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.success").value(true));

        verify(codeGenerationService, times(1)).generateOnly(any(ResourceRecommendation.class));
    }

    @Test
    void generateFiles_whenFailed_shouldReturnError() throws Exception {
        GeneratedArtifacts failedArtifacts = GeneratedArtifacts.builder()
                .serviceName("test-service")
                .success(false)
                .errorMessage("Generation failed")
                .build();

        when(codeGenerationService.generateOnly(any(ResourceRecommendation.class)))
                .thenReturn(failedArtifacts);

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"test-service\"}"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("Generation failed"));
    }

    @Test
    void generateAndCreatePR_shouldReturnArtifactsWithPR() throws Exception {
        mockArtifacts.setPullRequestUrl("https://dev.azure.com/pr/123");
        when(codeGenerationService.generateAndDeploy(any(ResourceRecommendation.class), anyBoolean()))
                .thenReturn(mockArtifacts);

        mockMvc.perform(post("/api/generate-and-pr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"test-service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.success").value(true));

        verify(codeGenerationService, times(1))
                .generateAndDeploy(any(ResourceRecommendation.class), eq(true));
    }

    @Test
    void getAzureDevOpsStatus_shouldReturnStatus() throws Exception {
        when(azureDevOpsService.isConfigured()).thenReturn(true);
        when(azureDevOpsService.getRepositoryUrl()).thenReturn("https://dev.azure.com/repo");

        mockMvc.perform(get("/api/azure-devops/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.repositoryUrl").value("https://dev.azure.com/repo"));
    }

    @Test
    void previewGeneration_shouldReturnPreview() throws Exception {
        when(codeGenerationService.generateOnly(any(ResourceRecommendation.class)))
                .thenReturn(mockArtifacts);

        mockMvc.perform(post("/api/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"test-service\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.fileCount").exists());
    }
}
