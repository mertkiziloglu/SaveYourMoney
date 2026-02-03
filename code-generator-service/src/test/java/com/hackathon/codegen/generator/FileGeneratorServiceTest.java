package com.hackathon.codegen.generator;

import com.hackathon.codegen.model.GeneratedArtifacts;
import com.hackathon.codegen.model.ResourceRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileGeneratorServiceTest {

    private FileGeneratorService fileGeneratorService;

    @BeforeEach
    void setUp() {
        fileGeneratorService = new FileGeneratorService();
    }

    @Test
    void generateArtifacts_shouldGenerateKubernetesFiles() {
        ResourceRecommendation recommendation = ResourceRecommendation.builder()
                .serviceName("test-service")
                .kubernetes(ResourceRecommendation.KubernetesResources.builder()
                        .cpuRequest("200m")
                        .cpuLimit("400m")
                        .memoryRequest("512Mi")
                        .memoryLimit("1Gi")
                        .build())
                .jvm(ResourceRecommendation.JvmConfiguration.builder()
                        .xms("384m")
                        .xmx("512m")
                        .gcType("G1GC")
                        .build())
                .build();

        GeneratedArtifacts artifacts = fileGeneratorService.generateArtifacts(recommendation);

        assertThat(artifacts).isNotNull();
        assertThat(artifacts.getServiceName()).isEqualTo("test-service");
        assertThat(artifacts.getFiles()).isNotEmpty();
        assertThat(artifacts.getFiles().stream()
                .anyMatch(f -> f.getFileName().contains("deployment.yaml"))).isTrue();
    }

    @Test
    void generateArtifacts_shouldHandleNullKubernetesConfig() {
        ResourceRecommendation recommendation = ResourceRecommendation.builder()
                .serviceName("test-service")
                .build();

        GeneratedArtifacts artifacts = fileGeneratorService.generateArtifacts(recommendation);

        assertThat(artifacts).isNotNull();
        assertThat(artifacts.getServiceName()).isEqualTo("test-service");
    }
}
