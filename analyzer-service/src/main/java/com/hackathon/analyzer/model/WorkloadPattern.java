package com.hackathon.analyzer.model;

/**
 * Workload pattern types detected by ML classifier
 */
public enum WorkloadPattern {

    /**
     * Steady, consistent load with minimal variation
     * Recommendation: Use reserved instances, predictable scaling
     */
    STEADY_STATE("Steady State", "Consistent load with minimal variation"),

    /**
     * Sudden, unpredictable spikes in resource usage
     * Recommendation: Aggressive auto-scaling, spot instances
     */
    BURSTY("Bursty", "Unpredictable spikes and valleys"),

    /**
     * Predictable daily or weekly patterns
     * Recommendation: Scheduled scaling based on time patterns
     */
    PERIODIC("Periodic", "Predictable daily/weekly patterns"),

    /**
     * Linear growth trend over time
     * Recommendation: Gradual capacity planning, trend-based scaling
     */
    GROWING("Growing", "Continuous linear growth trend"),

    /**
     * Seasonal variations (monthly, quarterly)
     * Recommendation: Season-aware capacity planning
     */
    SEASONAL("Seasonal", "Monthly or quarterly seasonal patterns"),

    /**
     * Declining resource usage over time
     * Recommendation: Consider service consolidation or retirement
     */
    DECLINING("Declining", "Decreasing resource usage trend"),

    /**
     * Unpredictable, chaotic patterns
     * Recommendation: Conservative over-provisioning, high buffer
     */
    CHAOTIC("Chaotic", "No clear pattern detected");

    private final String displayName;
    private final String description;

    WorkloadPattern(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
