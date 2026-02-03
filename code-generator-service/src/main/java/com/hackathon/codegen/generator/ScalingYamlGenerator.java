package com.hackathon.codegen.generator;

import com.hackathon.analyzer.model.scaling.HPARecommendation;
import com.hackathon.analyzer.model.scaling.VPARecommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScalingYamlGenerator {

    /**
     * Generate HPA YAML configuration
     */
    public String generateHPAYaml(HPARecommendation hpa) {
        StringBuilder yaml = new StringBuilder();

        yaml.append("apiVersion: autoscaling/v2\n");
        yaml.append("kind: HorizontalPodAutoscaler\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(hpa.getServiceName()).append("-hpa\n");
        yaml.append("  namespace: default\n");
        yaml.append("spec:\n");
        yaml.append("  scaleTargetRef:\n");
        yaml.append("    apiVersion: apps/v1\n");
        yaml.append("    kind: Deployment\n");
        yaml.append("    name: ").append(hpa.getServiceName()).append("\n");
        yaml.append("  minReplicas: ").append(hpa.getMinReplicas()).append("\n");
        yaml.append("  maxReplicas: ").append(hpa.getMaxReplicas()).append("\n");
        yaml.append("  metrics:\n");

        // CPU metric
        yaml.append("  - type: Resource\n");
        yaml.append("    resource:\n");
        yaml.append("      name: cpu\n");
        yaml.append("      target:\n");
        yaml.append("        type: Utilization\n");
        yaml.append("        averageUtilization: ").append(hpa.getTargetCPUUtilizationPercentage()).append("\n");

        // Memory metric
        if (hpa.getTargetMemoryUtilizationPercentage() != null) {
            yaml.append("  - type: Resource\n");
            yaml.append("    resource:\n");
            yaml.append("      name: memory\n");
            yaml.append("      target:\n");
            yaml.append("        type: Utilization\n");
            yaml.append("        averageUtilization: ").append(hpa.getTargetMemoryUtilizationPercentage()).append("\n");
        }

        // Custom metrics
        if (hpa.getCustomMetrics() != null && !hpa.getCustomMetrics().isEmpty()) {
            for (HPARecommendation.CustomMetricTarget metric : hpa.getCustomMetrics()) {
                yaml.append("  - type: ").append(metric.getMetricType()).append("\n");
                yaml.append("    pods:\n");
                yaml.append("      metric:\n");
                yaml.append("        name: ").append(metric.getMetricName()).append("\n");
                yaml.append("      target:\n");
                yaml.append("        type: ").append(metric.getTargetType()).append("\n");
                yaml.append("        averageValue: ").append(metric.getTargetValue().intValue()).append("\n");
            }
        }

        // Scaling behavior
        yaml.append("  behavior:\n");

        // Scale up behavior
        if (hpa.getScaleUpPolicy() != null) {
            yaml.append("    scaleUp:\n");
            yaml.append("      stabilizationWindowSeconds: ")
                .append(hpa.getScaleUpPolicy().getStabilizationWindowSeconds()).append("\n");
            yaml.append("      policies:\n");
            yaml.append("      - type: Percent\n");
            yaml.append("        value: ").append(hpa.getScaleUpPolicy().getPercentagePerScale()).append("\n");
            yaml.append("        periodSeconds: ").append(hpa.getScaleUpPolicy().getPeriodSeconds()).append("\n");

            if (hpa.getScaleUpPolicy().getPodsPerScale() != null) {
                yaml.append("      - type: Pods\n");
                yaml.append("        value: ").append(hpa.getScaleUpPolicy().getPodsPerScale()).append("\n");
                yaml.append("        periodSeconds: ").append(hpa.getScaleUpPolicy().getPeriodSeconds()).append("\n");
            }
            yaml.append("      selectPolicy: Max\n");
        }

        // Scale down behavior
        if (hpa.getScaleDownPolicy() != null) {
            yaml.append("    scaleDown:\n");
            yaml.append("      stabilizationWindowSeconds: ")
                .append(hpa.getScaleDownPolicy().getStabilizationWindowSeconds()).append("\n");
            yaml.append("      policies:\n");
            yaml.append("      - type: Percent\n");
            yaml.append("        value: ").append(hpa.getScaleDownPolicy().getPercentagePerScale()).append("\n");
            yaml.append("        periodSeconds: ").append(hpa.getScaleDownPolicy().getPeriodSeconds()).append("\n");
            yaml.append("      - type: Pods\n");
            yaml.append("        value: ").append(hpa.getScaleDownPolicy().getPodsPerScale()).append("\n");
            yaml.append("        periodSeconds: ").append(hpa.getScaleDownPolicy().getPeriodSeconds()).append("\n");
            yaml.append("      selectPolicy: Min\n");
        }

        return yaml.toString();
    }

    /**
     * Generate VPA YAML configuration
     */
    public String generateVPAYaml(VPARecommendation vpa) {
        StringBuilder yaml = new StringBuilder();

        yaml.append("apiVersion: autoscaling.k8s.io/v1\n");
        yaml.append("kind: VerticalPodAutoscaler\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(vpa.getServiceName()).append("-vpa\n");
        yaml.append("  namespace: default\n");
        yaml.append("spec:\n");
        yaml.append("  targetRef:\n");
        yaml.append("    apiVersion: apps/v1\n");
        yaml.append("    kind: Deployment\n");
        yaml.append("    name: ").append(vpa.getServiceName()).append("\n");
        yaml.append("  updatePolicy:\n");
        yaml.append("    updateMode: ").append(vpa.getUpdateMode()).append("\n");

        // Resource policy
        if (vpa.getResourcePolicy() != null) {
            yaml.append("  resourcePolicy:\n");
            yaml.append("    containerPolicies:\n");
            yaml.append("    - containerName: ").append(vpa.getServiceName()).append("\n");
            yaml.append("      controlledResources:\n");
            yaml.append("      - cpu\n");
            yaml.append("      - memory\n");

            if (vpa.getResourcePolicy().getCpuRange() != null) {
                yaml.append("      minAllowed:\n");
                yaml.append("        cpu: ").append(vpa.getResourcePolicy().getCpuRange().getMin()).append("\n");
                yaml.append("        memory: ").append(vpa.getResourcePolicy().getMemoryRange().getMin()).append("\n");
                yaml.append("      maxAllowed:\n");
                yaml.append("        cpu: ").append(vpa.getResourcePolicy().getCpuRange().getMax()).append("\n");
                yaml.append("        memory: ").append(vpa.getResourcePolicy().getMemoryRange().getMax()).append("\n");
            }

            yaml.append("      mode: ").append(vpa.getUpdateMode()).append("\n");
        }

        // Recommendation
        yaml.append("  # Recommended resources:\n");
        yaml.append("  # requests:\n");
        yaml.append("  #   cpu: ").append(vpa.getRecommendedRequests().getCpu()).append("\n");
        yaml.append("  #   memory: ").append(vpa.getRecommendedRequests().getMemory()).append("\n");
        yaml.append("  # limits:\n");
        yaml.append("  #   cpu: ").append(vpa.getRecommendedLimits().getCpu()).append("\n");
        yaml.append("  #   memory: ").append(vpa.getRecommendedLimits().getMemory()).append("\n");

        return yaml.toString();
    }

    /**
     * Generate updated Deployment YAML with scaling recommendations
     */
    public String generateDeploymentYaml(String serviceName, VPARecommendation vpa, HPARecommendation hpa) {
        StringBuilder yaml = new StringBuilder();

        yaml.append("apiVersion: apps/v1\n");
        yaml.append("kind: Deployment\n");
        yaml.append("metadata:\n");
        yaml.append("  name: ").append(serviceName).append("\n");
        yaml.append("  namespace: default\n");
        yaml.append("  labels:\n");
        yaml.append("    app: ").append(serviceName).append("\n");
        yaml.append("spec:\n");
        yaml.append("  replicas: ").append(hpa != null ? hpa.getRecommendedReplicas() : 3).append("\n");
        yaml.append("  selector:\n");
        yaml.append("    matchLabels:\n");
        yaml.append("      app: ").append(serviceName).append("\n");
        yaml.append("  template:\n");
        yaml.append("    metadata:\n");
        yaml.append("      labels:\n");
        yaml.append("        app: ").append(serviceName).append("\n");
        yaml.append("    spec:\n");
        yaml.append("      containers:\n");
        yaml.append("      - name: ").append(serviceName).append("\n");
        yaml.append("        image: ").append(serviceName).append(":latest\n");
        yaml.append("        ports:\n");
        yaml.append("        - containerPort: 8080\n");
        yaml.append("        resources:\n");
        yaml.append("          requests:\n");
        yaml.append("            cpu: ").append(vpa.getRecommendedRequests().getCpu()).append("\n");
        yaml.append("            memory: ").append(vpa.getRecommendedRequests().getMemory()).append("\n");
        yaml.append("          limits:\n");
        yaml.append("            cpu: ").append(vpa.getRecommendedLimits().getCpu()).append("\n");
        yaml.append("            memory: ").append(vpa.getRecommendedLimits().getMemory()).append("\n");

        return yaml.toString();
    }
}
