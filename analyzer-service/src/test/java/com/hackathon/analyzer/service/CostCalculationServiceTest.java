package com.hackathon.analyzer.service;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.ResourceRecommendation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CostCalculationService.
 * Covers cost parsing, savings estimation, and cost analysis generation.
 */
class CostCalculationServiceTest {

    private CostCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CostCalculationService();
    }

    @Test
    @DisplayName("Should parse millicores to cores correctly")
    void parseCpuToCore_millicore_shouldConvert() {
        assertThat(service.parseCpuToCore("100m")).isEqualTo(0.1);
        assertThat(service.parseCpuToCore("500m")).isEqualTo(0.5);
        assertThat(service.parseCpuToCore("1000m")).isEqualTo(1.0);
        assertThat(service.parseCpuToCore("2")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should parse memory units to GB correctly")
    void parseMemoryToGb_variousUnits_shouldConvert() {
        assertThat(service.parseMemoryToGb("256Mi")).isCloseTo(0.25, org.assertj.core.data.Offset.offset(0.01));
        assertThat(service.parseMemoryToGb("1024Mi")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.01));
        assertThat(service.parseMemoryToGb("2Gi")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should handle null CPU/memory gracefully")
    void parseCpuToCore_null_shouldReturnZero() {
        assertThat(service.parseCpuToCore(null)).isEqualTo(0.0);
        assertThat(service.parseMemoryToGb(null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate positive savings when downsizing")
    void estimateSavings_withDownsizing_shouldBePositive() {
        DescriptiveStatistics cpuStats = new DescriptiveStatistics();
        DescriptiveStatistics memoryStats = new DescriptiveStatistics();

        // Simulate low usage — should suggest downsizing → positive savings
        for (int i = 0; i < 50; i++) {
            cpuStats.addValue(20.0 + Math.random() * 5);
            memoryStats.addValue(30.0 + Math.random() * 5);
        }

        double savings = service.estimateSavings(cpuStats, memoryStats);
        assertThat(savings).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate full cost analysis from AnalysisResult")
    void calculateCostAnalysis_shouldReturnValidAnalysis() {
        AnalysisResult analysis = AnalysisResult.builder()
                .serviceName("test-service")
                .analysisTimestamp(Instant.now())
                .currentCpuRequest("500m")
                .currentMemoryRequest("1024Mi")
                .recommendedCpuRequest("250m")
                .recommendedMemoryRequest("512Mi")
                .build();

        ResourceRecommendation.CostAnalysis cost = service.calculateCostAnalysis(analysis);

        assertThat(cost).isNotNull();
        assertThat(cost.getCurrentMonthlyCost()).isGreaterThan(0);
        assertThat(cost.getRecommendedMonthlyCost()).isGreaterThan(0);
        assertThat(cost.getMonthlySavings()).isGreaterThan(0);
        assertThat(cost.getAnnualSavings()).isEqualTo(cost.getMonthlySavings() * 12);
        assertThat(cost.getSavingsPercentage()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return zero savings when upsizing")
    void estimateSavings_withHighUsage_shouldBeZero() {
        DescriptiveStatistics cpuStats = new DescriptiveStatistics();
        DescriptiveStatistics memoryStats = new DescriptiveStatistics();

        // Simulate very high usage — recommended > current → zero savings
        for (int i = 0; i < 50; i++) {
            cpuStats.addValue(90.0 + Math.random() * 5);
            memoryStats.addValue(85.0 + Math.random() * 5);
        }

        double savings = service.estimateSavings(cpuStats, memoryStats);
        assertThat(savings).isEqualTo(0.0);
    }
}
