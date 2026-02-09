package com.hackathon.analyzer.discovery;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRegistrationRequest {
    private String name;
    private String url;
}
