package com.hackathon.analyzer.service.strategy;

import com.hackathon.analyzer.model.AnalysisResult;
import com.hackathon.analyzer.model.MetricsSnapshot;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for modular resource analysis.
 * Each implementation handles one dimension of resource analysis
 * (CPU, Memory, Connection Pool) independently.
 */
public interface ResourceAnalysisStrategy {

    /**
     * Detect issues specific to this resource dimension.
     *
     * @param snapshots raw metrics snapshots
     * @return map of issue name → description (empty if no issues)
     */
    Map<String, String> detectIssues(List<MetricsSnapshot> snapshots);

    /**
     * Apply dimension-specific recommendations to the AnalysisResult builder.
     *
     * @param builder   the analysis result builder to enrich
     * @param snapshots raw metrics snapshots
     * @param stats     pre-computed descriptive statistics for this dimension
     */
    void applyRecommendations(AnalysisResult.AnalysisResultBuilder builder,
            List<MetricsSnapshot> snapshots,
            DescriptiveStatistics stats);

    /**
     * Returns the metric type this strategy handles (for logging/diagnostics).
     */
    String getMetricType();
}
