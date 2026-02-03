package com.hackathon.analyzer.model.scaling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingPrediction {
    private String serviceName;
    private LocalDateTime predictionTime;
    private LocalDateTime forecastFor;

    // Predicted metrics
    private Double predictedCPUUsage;
    private Double predictedMemoryUsage;
    private Double predictedRequestRate;

    // Scaling recommendation
    private Integer currentReplicas;
    private Integer recommendedReplicas;

    // Confidence and reasoning
    private Double confidence;
    private String reason;
    private TimeSeriesPattern detectedPattern;

    // Upcoming events
    private List<ScalingEvent> upcomingEvents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScalingEvent {
        private LocalDateTime eventTime;
        private String eventType; // "SCALE_UP", "SCALE_DOWN", "PEAK_LOAD", "LOW_ACTIVITY"
        private Integer recommendedReplicas;
        private String reason;
        private Double confidence;
    }
}
