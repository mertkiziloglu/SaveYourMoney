package com.hackathon.analyzer.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo {
    private String name;
    private String url;
    private int port;
    private boolean healthy;
    private Instant discoveredAt;
    private Instant lastSeenAt;
    private String discoveryMethod; // "SCAN" or "REGISTER"
    private String applicationName; // from actuator/info
}
