package com.hackathon.analyzer.service;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.ResourceRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

/**
 * Isolated cost calculation service.
 * Converts resource units (millicores, Mi) to dollar estimates
 * using standard GCP/AWS pricing baselines.
 */
@Slf4j
@Service
public class CostCalculationService {

    static final double COST_PER_CPU_CORE_MONTH = 30.0;
    static final double COST_PER_GB_MEMORY_MONTH = 5.0;
    private static final double SAFETY_MARGIN = 0.20;

    /**
     * Build a full CostAnalysis comparing current vs recommended allocations.
     */
    public ResourceRecommendation.CostAnalysis calculateCostAnalysis(AnalysisResult analysis) {
        double currentCpuCores = parseCpuToCore(analysis.getCurrentCpuRequest());
        double currentMemoryGb = parseMemoryToGb(analysis.getCurrentMemoryRequest());
        double currentMonthlyCost = (currentCpuCores * COST_PER_CPU_CORE_MONTH)
                + (currentMemoryGb * COST_PER_GB_MEMORY_MONTH);

        double recommendedCpuCores = parseCpuToCore(analysis.getRecommendedCpuRequest());
        double recommendedMemoryGb = parseMemoryToGb(analysis.getRecommendedMemoryRequest());
        double recommendedMonthlyCost = (recommendedCpuCores * COST_PER_CPU_CORE_MONTH)
                + (recommendedMemoryGb * COST_PER_GB_MEMORY_MONTH);

        double monthlySavings = currentMonthlyCost - recommendedMonthlyCost;
        double annualSavings = monthlySavings * 12;
        int savingsPercentage = currentMonthlyCost > 0
                ? (int) ((monthlySavings / currentMonthlyCost) * 100)
                : 0;

        return ResourceRecommendation.CostAnalysis.builder()
                .currentMonthlyCost(round2(currentMonthlyCost))
                .recommendedMonthlyCost(round2(recommendedMonthlyCost))
                .monthlySavings(round2(monthlySavings))
                .annualSavings(round2(annualSavings))
                .savingsPercentage(savingsPercentage)
                .build();
    }

    /**
     * Quick savings estimate from raw statistics (used for AnalysisResult
     * snapshot).
     */
    public double estimateSavings(DescriptiveStatistics cpuStats, DescriptiveStatistics memoryStats) {
        double currentCpuCores = 1.0;
        double currentMemoryGb = 2.0;

        double p95Cpu = cpuStats.getPercentile(95);
        double p95Memory = memoryStats.getPercentile(95);

        double recommendedCpuCores = Math.ceil((p95Cpu / 100.0) * (1 + SAFETY_MARGIN) * 10) / 10.0;
        double recommendedMemoryGb = Math.ceil((p95Memory / 100.0) * (1 + SAFETY_MARGIN) * 10) / 10.0;

        double currentCost = (currentCpuCores * COST_PER_CPU_CORE_MONTH)
                + (currentMemoryGb * COST_PER_GB_MEMORY_MONTH);
        double recommendedCost = (recommendedCpuCores * COST_PER_CPU_CORE_MONTH)
                + (recommendedMemoryGb * COST_PER_GB_MEMORY_MONTH);

        return Math.max(0.0, round2(currentCost - recommendedCost));
    }

    double parseCpuToCore(String cpu) {
        if (cpu == null)
            return 0.0;
        if (cpu.endsWith("m")) {
            return Double.parseDouble(cpu.replace("m", "")) / 1000.0;
        }
        return Double.parseDouble(cpu);
    }

    double parseMemoryToGb(String memory) {
        if (memory == null)
            return 0.0;
        if (memory.endsWith("Mi")) {
            return Double.parseDouble(memory.replace("Mi", "")) / 1024.0;
        } else if (memory.endsWith("Gi")) {
            return Double.parseDouble(memory.replace("Gi", ""));
        }
        return Double.parseDouble(memory) / (1024.0 * 1024.0 * 1024.0);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
