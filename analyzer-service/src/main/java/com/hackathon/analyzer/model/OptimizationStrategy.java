package com.hackathon.analyzer.model;

/**
 * Optimization strategies based on workload patterns
 */
public enum OptimizationStrategy {

    RESERVED_CAPACITY("Reserved Capacity",
        "Use reserved instances for predictable steady load"),

    AGGRESSIVE_AUTOSCALING("Aggressive Auto-Scaling",
        "Fast scale-up/down for bursty workloads"),

    SCHEDULED_SCALING("Scheduled Scaling",
        "Pre-scheduled scaling based on time patterns"),

    PREDICTIVE_SCALING("Predictive Scaling",
        "ML-based predictive auto-scaling"),

    SPOT_INSTANCES("Spot Instances",
        "Use spot/preemptible instances for cost savings"),

    RIGHT_SIZING("Right-Sizing",
        "Adjust baseline capacity to match average load"),

    SERVICE_CONSOLIDATION("Service Consolidation",
        "Consider consolidating with other services"),

    CONSERVATIVE_BUFFER("Conservative Buffer",
        "Maintain high buffer for unpredictable patterns");

    private final String displayName;
    private final String description;

    OptimizationStrategy(String displayName, String description) {
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
