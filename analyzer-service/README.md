# Analyzer Service

## Overview

The **SaveYourMoney Analyzer Service** is an AI-powered resource optimization engine that:

1. **Collects Metrics** from demo services via Prometheus endpoints
2. **Analyzes Performance** using statistical methods (P95, P99, trend detection)
3. **Detects Issues** (CPU throttling, memory leaks, connection pool exhaustion)
4. **Generates Recommendations** for Kubernetes resources, JVM configuration, and connection pools
5. **Calculates Cost Savings** based on optimized resource allocation

---

## Features

### 🔍 Metrics Collection
- Automatic scraping every 10 seconds
- Parses Prometheus format metrics
- Stores time-series data in H2 database
- Tracks CPU, memory, GC, threads, HTTP requests, HikariCP connections

### 🤖 AI Analysis Engine
- Statistical analysis (P95, P99, max, avg, std dev)
- Pattern detection:
  - CPU throttling (P95 > 80%)
  - Memory leaks (continuous growth trend)
  - Connection pool exhaustion (>50% samples maxed out)
- Confidence scoring based on data quality

### 💡 Recommendations
- **Kubernetes Resources**: CPU/memory request and limits
- **JVM Configuration**: Heap sizes (Xms/Xmx), GC settings
- **Connection Pool**: HikariCP max pool size, min idle
- **Thread Pool**: Tomcat thread configuration

### 💰 Cost Analysis
- Current vs recommended monthly costs
- Estimated savings (monthly and annual)
- Cost per vCPU: $30/month
- Cost per GB memory: $5/month

---

## API Endpoints

### Health Check
```bash
GET http://localhost:8084/api/health
```

### Analyze Single Service
```bash
POST http://localhost:8084/api/analyze/cpu-hungry-service
```

**Response:**
```json
{
  "serviceName": "cpu-hungry-service",
  "kubernetes": {
    "cpuRequest": "500m",
    "cpuLimit": "1000m",
    "memoryRequest": "512Mi",
    "memoryLimit": "768Mi"
  },
  "jvm": {
    "xms": "384m",
    "xmx": "435m",
    "gcType": "G1GC"
  },
  "costAnalysis": {
    "currentMonthlyCost": 8.25,
    "recommendedMonthlyCost": 17.5,
    "monthlySavings": -9.25,
    "annualSavings": -111.0,
    "savingsPercentage": -112
  },
  "confidenceScore": 0.8,
  "rationale": "CPU P95: 85%, recommending 500m (current: 100m). Memory leak detected.",
  "detectedIssues": {
    "CPU Throttling": "CPU usage exceeds limits causing performance degradation"
  }
}
```

### Analyze All Services
```bash
POST http://localhost:8084/api/analyze-all
```

Returns recommendations for all 3 demo services.

### Get Dashboard Summary
```bash
GET http://localhost:8084/api/dashboard
```

**Response:**
```json
{
  "servicesAnalyzed": 3,
  "totalMonthlySavings": 450.0,
  "totalAnnualSavings": 5400.0,
  "cpuHungryService": { ... },
  "memoryLeakerService": { ... },
  "dbConnectionService": { ... }
}
```

### Get Analysis History
```bash
GET http://localhost:8084/api/analysis-history/cpu-hungry-service
```

### Get Recent Metrics
```bash
GET http://localhost:8084/api/metrics/cpu-hungry-service?limit=50
```

### Manual Metrics Collection
```bash
POST http://localhost:8084/api/collect-metrics
```

---

## How It Works

### 1. Metrics Collection (Automatic)

```java
@Scheduled(fixedDelay = 10000) // Every 10 seconds
public void collectMetrics() {
    // Scrape /actuator/prometheus from each service
    // Parse Prometheus format
    // Save to database
}
```

**Collected Metrics:**
- `process_cpu_usage` → CPU percentage
- `jvm_memory_used_bytes` → Heap usage
- `jvm_gc_pause_seconds` → GC time
- `http_server_requests_seconds` → Request latency (P95, P99)
- `hikari_connections_active` → Active DB connections

### 2. Statistical Analysis

```java
// Calculate P95, P99, max, average
DescriptiveStatistics cpuStats = new DescriptiveStatistics();
for (MetricsSnapshot snapshot : snapshots) {
    cpuStats.addValue(snapshot.getCpuUsagePercent());
}

double p95 = cpuStats.getPercentile(95);
double p99 = cpuStats.getPercentile(99);
```

### 3. Issue Detection

**CPU Throttling:**
```java
boolean cpuThrottling = p95 > 80.0 || max > 90.0;
```

**Memory Leak:**
```java
// Compare first half vs second half average
double firstHalfAvg = ...;
double secondHalfAvg = ...;
boolean memoryLeak = secondHalfAvg > firstHalfAvg * 1.2; // 20% increase
```

**Connection Pool Exhaustion:**
```java
long exhaustedCount = snapshots.stream()
    .filter(s -> s.activeConnections == s.maxConnections)
    .count();
boolean poolExhaustion = exhaustedCount > snapshots.size() * 0.5; // >50%
```

### 4. Recommendation Generation

**CPU Request = P95 + 20% Safety Margin:**
```java
double p95Percent = cpuStats.getPercentile(95);
double cpuCores = (p95Percent / 100.0) * 1.20; // +20%
int millicores = (int) Math.ceil(cpuCores * 1000);
return millicores + "m"; // e.g., "500m"
```

**Memory Request = Max Heap + 20% Safety Margin:**
```java
long maxHeapBytes = snapshots.stream()
    .mapToLong(MetricsSnapshot::getHeapUsedBytes)
    .max().orElse(256MB);
long recommendedBytes = (long) (maxHeapBytes * 1.20);
return (recommendedBytes / 1MB) + "Mi"; // e.g., "512Mi"
```

**Connection Pool = P95 Active Connections + 20%:**
```java
double p95Active = connectionStats.getPercentile(95);
int recommended = (int) Math.ceil(p95Active * 1.20);
return Math.max(10, recommended); // Minimum 10
```

### 5. Cost Calculation

```java
double cpuCost = cpuCores * $30/month;
double memoryCost = memoryGB * $5/month;
double monthlySavings = currentCost - recommendedCost;
double annualSavings = monthlySavings * 12;
```

---

## Running the Service

### Local Development

```bash
cd ~/Desktop/SaveYourMoney/analyzer-service
mvn spring-boot:run
```

**Prerequisites:**
- Demo services must be running (ports 8081, 8082, 8083)
- Analyzer will start collecting metrics automatically after 5 seconds

### Test the Analyzer

```bash
# Wait 30 seconds for metrics collection
sleep 30

# Analyze CPU-Hungry-Service
curl -X POST http://localhost:8084/api/analyze/cpu-hungry-service

# Get dashboard summary
curl http://localhost:8084/api/dashboard
```

---

## Configuration

Edit `application.yml` to change service URLs:

```yaml
analyzer:
  services:
    cpu-hungry:
      url: http://localhost:8081
    memory-leaker:
      url: http://localhost:8082
    db-connection:
      url: http://localhost:8083
```

For cloud deployment, use environment URLs:
```yaml
analyzer:
  services:
    cpu-hungry:
      url: ${CPU_HUNGRY_URL:http://localhost:8081}
```

---

## Database

**H2 Console:** http://localhost:8084/h2-console

**JDBC URL:** `jdbc:h2:mem:analyzerdb`

**Tables:**
- `metrics_snapshot` - Time-series metrics data
- `analysis_result` - Analysis results and recommendations

---

## Next Steps

This analyzer service provides recommendations in JSON format. The next component (**Code Generator Service - Task #5**) will:

1. Take these recommendations
2. Generate actual code files:
   - `deployment.yaml` (Kubernetes)
   - `application.properties` (Spring Boot)
   - `values.yaml` (Helm)
3. Create Pull Requests in Azure DevOps

---

## Example Workflow

```bash
# 1. Start all demo services
cd cpu-hungry-service && mvn spring-boot:run &
cd memory-leaker-service && mvn spring-boot:run &
cd db-connection-service && mvn spring-boot:run &

# 2. Start analyzer
cd analyzer-service && mvn spring-boot:run &

# 3. Run load tests (JMeter)
# ... metrics will be collected automatically ...

# 4. Analyze services
curl -X POST http://localhost:8084/api/analyze-all

# 5. Get recommendations
curl http://localhost:8084/api/dashboard
```

---

**Built for SaveYourMoney Hackathon 2025** 🚀
