package com.hackathon.analyzer.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceRecommendationTest {

    @Test
    void builder_shouldCreateResourceRecommendation() {
        ResourceRecommendation.KubernetesResources k8s = ResourceRecommendation.KubernetesResources.builder()
                .cpuRequest("200m")
                .cpuLimit("400m")
                .memoryRequest("512Mi")
                .memoryLimit("1Gi")
                .build();

        ResourceRecommendation.JvmConfiguration jvm = ResourceRecommendation.JvmConfiguration.builder()
                .xms("384m")
                .xmx("512m")
                .gcType("G1GC")
                .build();

        ResourceRecommendation.CostAnalysis cost = ResourceRecommendation.CostAnalysis.builder()
                .currentMonthlyCost(100.0)
                .recommendedMonthlyCost(60.0)
                .monthlySavings(40.0)
                .annualSavings(480.0)
                .savingsPercentage(40)
                .build();

        Map<String, String> issues = new HashMap<>();
        issues.put("CPU Throttling", "High CPU usage detected");

        ResourceRecommendation recommendation = ResourceRecommendation.builder()
                .serviceName("test-service")
                .kubernetes(k8s)
                .jvm(jvm)
                .costAnalysis(cost)
                .confidenceScore(0.85)
                .rationale("Based on analysis")
                .detectedIssues(issues)
                .build();

        assertThat(recommendation.getServiceName()).isEqualTo("test-service");
        assertThat(recommendation.getKubernetes().getCpuRequest()).isEqualTo("200m");
        assertThat(recommendation.getJvm().getXms()).isEqualTo("384m");
        assertThat(recommendation.getCostAnalysis().getMonthlySavings()).isEqualTo(40.0);
        assertThat(recommendation.getConfidenceScore()).isEqualTo(0.85);
        assertThat(recommendation.getDetectedIssues()).containsKey("CPU Throttling");
    }

    @Test
    void kubernetesResources_shouldBuildCorrectly() {
        ResourceRecommendation.KubernetesResources k8s = ResourceRecommendation.KubernetesResources.builder()
                .cpuRequest("100m")
                .cpuLimit("200m")
                .memoryRequest("256Mi")
                .memoryLimit("512Mi")
                .build();

        assertThat(k8s.getCpuRequest()).isEqualTo("100m");
        assertThat(k8s.getCpuLimit()).isEqualTo("200m");
        assertThat(k8s.getMemoryRequest()).isEqualTo("256Mi");
        assertThat(k8s.getMemoryLimit()).isEqualTo("512Mi");
    }

    @Test
    void costAnalysis_shouldCalculateCorrectly() {
        ResourceRecommendation.CostAnalysis cost = ResourceRecommendation.CostAnalysis.builder()
                .currentMonthlyCost(150.0)
                .recommendedMonthlyCost(90.0)
                .monthlySavings(60.0)
                .annualSavings(720.0)
                .savingsPercentage(40)
                .build();

        assertThat(cost.getCurrentMonthlyCost()).isEqualTo(150.0);
        assertThat(cost.getRecommendedMonthlyCost()).isEqualTo(90.0);
        assertThat(cost.getMonthlySavings()).isEqualTo(60.0);
        assertThat(cost.getAnnualSavings()).isEqualTo(720.0);
        assertThat(cost.getSavingsPercentage()).isEqualTo(40);
    }
}
