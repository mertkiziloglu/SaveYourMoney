package com.hackathon.analyzer.service;

import com.hackathon.analyzer.model.ResourceRecommendation;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;

/**
 * Initial Configuration Generator for new projects without metrics
 *
 * This service provides baseline resource configurations for new projects
 * based on service type, expected load, and industry best practices.
 */
@Service
@Slf4j
public class InitialConfigurationService {

    /**
     * Service types based on resource consumption patterns
     */
    public enum ServiceType {
        WEB_API,           // REST APIs, web services
        BACKGROUND_JOB,    // Batch processing, scheduled tasks
        DATABASE_HEAVY,    // Services with many DB queries
        CPU_INTENSIVE,     // Computation-heavy services
        MEMORY_INTENSIVE,  // Large data processing, caching
        EVENT_DRIVEN,      // Kafka/RabbitMQ consumers
        MICROSERVICE       // Standard Spring Boot microservice
    }

    /**
     * Expected load levels
     */
    public enum LoadLevel {
        LOW,      // < 100 requests/min
        MEDIUM,   // 100-1000 requests/min
        HIGH,     // 1000-10000 requests/min
        VERY_HIGH // > 10000 requests/min
    }

    /**
     * Generate initial configuration for a new project
     *
     * @param serviceName Name of the service
     * @param serviceType Type of service (web, batch, etc.)
     * @param expectedLoad Expected load level
     * @return ResourceRecommendation with baseline configuration
     */
    public ResourceRecommendation generateInitialConfiguration(
            String serviceName,
            ServiceType serviceType,
            LoadLevel expectedLoad) {

        log.info("Generating initial configuration for new service: {} (type: {}, load: {})",
                serviceName, serviceType, expectedLoad);

        ResourceRecommendation recommendation = new ResourceRecommendation();
        recommendation.setServiceName(serviceName);
        recommendation.setConfidenceScore(0.6); // Lower confidence for estimations
        recommendation.setDetectedIssues(new HashMap<>());
        recommendation.getDetectedIssues().put("Note", "Baseline configuration - adjust after monitoring real metrics");

        // Get baseline resources
        BaselineResources baseline = getBaselineResources(serviceType, expectedLoad);

        // Kubernetes resources
        ResourceRecommendation.KubernetesResources k8s = ResourceRecommendation.KubernetesResources.builder()
                .cpuRequest(baseline.cpuRequestMillis + "m")
                .cpuLimit(baseline.cpuLimitMillis + "m")
                .memoryRequest(baseline.memoryRequestMb + "Mi")
                .memoryLimit(baseline.memoryLimitMb + "Mi")
                .build();
        recommendation.setKubernetes(k8s);

        // JVM configuration
        ResourceRecommendation.JvmConfiguration jvm = ResourceRecommendation.JvmConfiguration.builder()
                .xms(baseline.jvmHeapMinMb + "m")
                .xmx(baseline.jvmHeapMaxMb + "m")
                .gcType(baseline.gcAlgorithm)
                .build();
        recommendation.setJvm(jvm);

        // Connection pool
        ResourceRecommendation.ConnectionPoolConfig pool = ResourceRecommendation.ConnectionPoolConfig.builder()
                .minimumIdle(baseline.poolMinIdle)
                .maximumPoolSize(baseline.poolMaxSize)
                .connectionTimeout(30000L)
                .idleTimeout(600000L)
                .build();
        recommendation.setConnectionPool(pool);

        // Thread pool
        ResourceRecommendation.ThreadPoolConfig threadPool = ResourceRecommendation.ThreadPoolConfig.builder()
                .minSpareThreads(baseline.threadPoolCore)
                .maxThreads(baseline.threadPoolMax)
                .build();
        recommendation.setThreadPool(threadPool);

        // Cost analysis (estimated)
        ResourceRecommendation.CostAnalysis cost = ResourceRecommendation.CostAnalysis.builder()
                .currentMonthlyCost(0.0) // No current cost for new service
                .recommendedMonthlyCost(calculateMonthlyCost(baseline.cpuRequestMillis, baseline.memoryRequestMb))
                .monthlySavings(0.0)
                .annualSavings(0.0)
                .build();
        recommendation.setCostAnalysis(cost);

        log.info("Generated initial configuration: CPU={}m, Memory={}Mi, Replicas={}",
                baseline.cpuRequestMillis, baseline.memoryRequestMb, baseline.replicas);

        return recommendation;
    }

    /**
     * Get baseline resources based on service type and load
     */
    private BaselineResources getBaselineResources(ServiceType type, LoadLevel load) {
        BaselineResources baseline = new BaselineResources();

        // Base configuration for microservice
        switch (type) {
            case WEB_API:
                baseline.cpuRequestMillis = 250;
                baseline.cpuLimitMillis = 1000;
                baseline.memoryRequestMb = 512;
                baseline.memoryLimitMb = 1024;
                baseline.poolMinIdle = 5;
                baseline.poolMaxSize = 20;
                baseline.threadPoolCore = 10;
                baseline.threadPoolMax = 50;
                baseline.threadPoolQueue = 100;
                baseline.gcAlgorithm = "G1GC";
                baseline.gcThreads = 2;
                break;

            case BACKGROUND_JOB:
                baseline.cpuRequestMillis = 500;
                baseline.cpuLimitMillis = 2000;
                baseline.memoryRequestMb = 1024;
                baseline.memoryLimitMb = 2048;
                baseline.poolMinIdle = 2;
                baseline.poolMaxSize = 10;
                baseline.threadPoolCore = 5;
                baseline.threadPoolMax = 20;
                baseline.threadPoolQueue = 500;
                baseline.gcAlgorithm = "G1GC";
                baseline.gcThreads = 2;
                break;

            case DATABASE_HEAVY:
                baseline.cpuRequestMillis = 500;
                baseline.cpuLimitMillis = 1500;
                baseline.memoryRequestMb = 768;
                baseline.memoryLimitMb = 1536;
                baseline.poolMinIdle = 10;
                baseline.poolMaxSize = 50;
                baseline.threadPoolCore = 15;
                baseline.threadPoolMax = 60;
                baseline.threadPoolQueue = 200;
                baseline.gcAlgorithm = "G1GC";
                baseline.gcThreads = 2;
                break;

            case CPU_INTENSIVE:
                baseline.cpuRequestMillis = 1000;
                baseline.cpuLimitMillis = 2000;
                baseline.memoryRequestMb = 512;
                baseline.memoryLimitMb = 1024;
                baseline.poolMinIdle = 3;
                baseline.poolMaxSize = 10;
                baseline.threadPoolCore = 8;
                baseline.threadPoolMax = 16;
                baseline.threadPoolQueue = 50;
                baseline.gcAlgorithm = "ParallelGC";
                baseline.gcThreads = 4;
                break;

            case MEMORY_INTENSIVE:
                baseline.cpuRequestMillis = 500;
                baseline.cpuLimitMillis = 1000;
                baseline.memoryRequestMb = 2048;
                baseline.memoryLimitMb = 4096;
                baseline.poolMinIdle = 5;
                baseline.poolMaxSize = 15;
                baseline.threadPoolCore = 10;
                baseline.threadPoolMax = 30;
                baseline.threadPoolQueue = 100;
                baseline.gcAlgorithm = "G1GC";
                baseline.gcThreads = 2;
                break;

            case EVENT_DRIVEN:
                baseline.cpuRequestMillis = 500;
                baseline.cpuLimitMillis = 1500;
                baseline.memoryRequestMb = 768;
                baseline.memoryLimitMb = 1536;
                baseline.poolMinIdle = 5;
                baseline.poolMaxSize = 20;
                baseline.threadPoolCore = 20;
                baseline.threadPoolMax = 100;
                baseline.threadPoolQueue = 1000;
                baseline.gcAlgorithm = "G1GC";
                baseline.gcThreads = 2;
                break;

            case MICROSERVICE:
            default:
                baseline.cpuRequestMillis = 250;
                baseline.cpuLimitMillis = 500;
                baseline.memoryRequestMb = 512;
                baseline.memoryLimitMb = 1024;
                baseline.poolMinIdle = 5;
                baseline.poolMaxSize = 20;
                baseline.threadPoolCore = 10;
                baseline.threadPoolMax = 50;
                baseline.threadPoolQueue = 100;
                baseline.gcAlgorithm = "G1GC";
                baseline.gcThreads = 2;
                break;
        }

        // Adjust for load level
        double loadMultiplier = getLoadMultiplier(load);
        baseline.cpuRequestMillis = (int) (baseline.cpuRequestMillis * loadMultiplier);
        baseline.cpuLimitMillis = (int) (baseline.cpuLimitMillis * loadMultiplier);
        baseline.memoryRequestMb = (int) (baseline.memoryRequestMb * loadMultiplier);
        baseline.memoryLimitMb = (int) (baseline.memoryLimitMb * loadMultiplier);
        baseline.poolMaxSize = (int) (baseline.poolMaxSize * loadMultiplier);
        baseline.threadPoolMax = (int) (baseline.threadPoolMax * loadMultiplier);

        // Set replicas based on load
        baseline.replicas = getReplicaCount(load);

        // Calculate JVM heap (70% of memory limit)
        baseline.jvmHeapMinMb = (int) (baseline.memoryRequestMb * 0.5);
        baseline.jvmHeapMaxMb = (int) (baseline.memoryLimitMb * 0.7);

        return baseline;
    }

    private double getLoadMultiplier(LoadLevel load) {
        switch (load) {
            case LOW: return 0.7;
            case MEDIUM: return 1.0;
            case HIGH: return 1.5;
            case VERY_HIGH: return 2.5;
            default: return 1.0;
        }
    }

    private int getReplicaCount(LoadLevel load) {
        switch (load) {
            case LOW: return 1;
            case MEDIUM: return 2;
            case HIGH: return 3;
            case VERY_HIGH: return 5;
            default: return 2;
        }
    }

    private double calculateMonthlyCost(int cpuMillis, int memoryMb) {
        double cpuCost = (cpuMillis / 1000.0) * 30.0; // $30 per vCPU per month
        double memoryCost = (memoryMb / 1024.0) * 5.0; // $5 per GB per month
        return cpuCost + memoryCost;
    }

    /**
     * Internal class to hold baseline resource values
     */
    private static class BaselineResources {
        int cpuRequestMillis;
        int cpuLimitMillis;
        int memoryRequestMb;
        int memoryLimitMb;
        int jvmHeapMinMb;
        int jvmHeapMaxMb;
        int poolMinIdle;
        int poolMaxSize;
        int threadPoolCore;
        int threadPoolMax;
        int threadPoolQueue;
        int replicas;
        String gcAlgorithm;
        int gcThreads;
    }
}
