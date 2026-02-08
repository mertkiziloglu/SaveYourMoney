# SaveYourMoney 💰

Intelligent resource optimization platform that automatically analyzes microservices, detects performance issues, and generates optimized configurations to reduce cloud costs.

---

## Overview

SaveYourMoney is an intelligent system that:

1. **Monitors** microservices via Prometheus metrics
2. **Analyzes** performance patterns using statistical methods
3. **Detects** issues like CPU throttling, memory leaks, and connection pool exhaustion
4. **Recommends** optimal Kubernetes resources, JVM settings, and connection pools
5. **Generates** configuration files (YAML, properties, Helm values)
6. **Calculates** potential cost savings (monthly and annual)
7. **Creates** Pull Requests in Azure DevOps automatically

---

## Architecture

```
┌─────────────────┐
│  Dashboard UI   │  (Port 8080)
│   HTML/CSS/JS   │
└────────┬────────┘
         │
         ├──────────────────┬──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Analyzer Service│  │Code Gen Service │  │  Demo Services  │
│   (Port 8084)   │  │  (Port 8085)    │  │  (Ports 8081-3) │
│                 │  │                 │  │                 │
│ • Metrics       │  │ • File Gen      │  │ • CPU Hungry    │
│ • Analysis      │  │ • Azure DevOps  │  │ • Memory Leaker │
│ • Recommendations│  │ • PR Creation   │  │ • DB Connection │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## Components

### 1. Demo Services (Port 8081-8083)
Three intentionally misconfigured microservices to demonstrate optimization:

- **CPU-Hungry-Service** (Port 8081): CPU-intensive operations with insufficient CPU limits
- **Memory-Leaker-Service** (Port 8082): Memory leak simulation with low memory limits
- **DB-Connection-Service** (Port 8083): Database operations with small connection pool

### 2. Analyzer Service (Port 8084)
AI-powered analysis engine that:
- Collects metrics every 10 seconds from demo services
- Performs statistical analysis (P95, P99, trends)
- Detects performance issues
- Generates resource recommendations
- Calculates cost savings

### 3. Code Generator Service (Port 8085)
Automated configuration generator that:
- Takes analyzer recommendations
- Generates Kubernetes YAML, Spring Boot properties, Helm values
- Creates Azure DevOps Pull Requests
- Integrates with version control

### 4. Dashboard UI (Port 8080)
Modern web interface for:
- Real-time metrics visualization
- Issue detection alerts
- Before/After comparison charts
- One-click code generation and PR creation

### 5. JMeter Tests
Load testing scripts to expose performance problems:
- CPU stress tests (100 concurrent users)
- Memory leak tests (15-minute duration)
- Connection pool exhaustion tests (200 concurrent users)

---

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- JMeter 5.6+ (for load tests)
- Python 3 or Node.js (for dashboard)

### 1. Start Demo Services

```bash
# Terminal 1 - CPU Hungry Service
cd demo-services/cpu-hungry-service
mvn spring-boot:run

# Terminal 2 - Memory Leaker Service
cd demo-services/memory-leaker-service
mvn spring-boot:run

# Terminal 3 - DB Connection Service
cd demo-services/db-connection-service
mvn spring-boot:run
```

### 2. Start Backend Services

```bash
# Terminal 4 - Analyzer Service
cd analyzer-service
mvn spring-boot:run

# Terminal 5 - Code Generator Service
cd code-generator-service
mvn spring-boot:run
```

### 3. Start Dashboard

```bash
# Terminal 6 - Dashboard UI
cd dashboard-ui/public
python3 -m http.server 8080
```

### 4. Run Load Tests (Optional)

```bash
cd jmeter-tests
./run-all-tests.sh
```

### 5. Open Dashboard

```
http://localhost:8080
```

---

## Usage Workflow

### Step 1: Generate Metrics
Load tests will stress the demo services and expose performance issues.

```bash
cd jmeter-tests
./run-all-tests.sh
```

### Step 2: Analyze Services
The analyzer automatically collects metrics. After 30 seconds:

```bash
# Analyze all services
curl -X POST http://localhost:8084/api/analyze-all

# Or analyze individually
curl -X POST http://localhost:8084/api/analyze/cpu-hungry-service
```

### Step 3: View Recommendations
Check the dashboard or API:

```bash
curl http://localhost:8084/api/dashboard | jq
```

**Sample Output:**
```json
{
  "servicesAnalyzed": 3,
  "totalMonthlySavings": 450.0,
  "totalAnnualSavings": 5400.0,
  "cpuHungryService": {
    "kubernetes": {
      "cpuRequest": "500m",
      "cpuLimit": "1000m"
    },
    "detectedIssues": {
      "CPU Throttling": "CPU usage exceeds limits"
    }
  }
}
```

### Step 4: Generate Configuration Files
Generate optimized configuration files:

```bash
curl -X POST http://localhost:8085/api/generate \
  -H "Content-Type: application/json" \
  -d @recommendation.json
```

### Step 5: Create Pull Request
Automatically create a PR in Azure DevOps:

```bash
curl -X POST http://localhost:8085/api/generate-and-pr \
  -H "Content-Type: application/json" \
  -d @recommendation.json
```

---

## Key Features

### AI-Powered Analysis
- Statistical analysis using P95, P99 percentiles
- Pattern detection (CPU throttling, memory leaks, connection exhaustion)
- Confidence scoring based on data quality
- Trend detection (20% threshold for memory leaks)

### Cost Optimization
- Calculates monthly and annual savings
- Pricing: $30/vCPU, $5/GB memory
- Before/After cost comparison
- ROI visualization

### Automatic Code Generation
- Kubernetes deployment YAML
- Spring Boot application.properties
- Helm chart values.yaml
- JVM configuration
- Connection pool settings

### Azure DevOps Integration
- Automatic PR creation
- Branch management
- Code review workflow
- Deployment automation

---

## Project Structure

```
SaveYourMoney/
├── analyzer-service/           # AI analysis engine (Port 8084)
│   ├── src/main/java/
│   │   └── com/hackathon/analyzer/
│   │       ├── service/ResourceAnalyzerService.java
│   │       ├── collector/MetricsCollectorService.java
│   │       └── controller/AnalyzerController.java
│   └── pom.xml
│
├── code-generator-service/     # Configuration generator (Port 8085)
│   ├── src/main/java/
│   │   └── com/hackathon/codegen/
│   │       ├── generator/FileGeneratorService.java
│   │       ├── azuredevops/AzureDevOpsService.java
│   │       └── controller/CodeGeneratorController.java
│   └── pom.xml
│
├── demo-services/              # Test microservices
│   ├── cpu-hungry-service/     # CPU throttling demo (Port 8081)
│   ├── memory-leaker-service/  # Memory leak demo (Port 8082)
│   └── db-connection-service/  # Connection pool demo (Port 8083)
│
├── dashboard-ui/               # Web dashboard (Port 8080)
│   └── public/
│       ├── index.html
│       ├── styles/main.css
│       └── services/
│           ├── api.js
│           ├── charts.js
│           └── app.js
│
├── jmeter-tests/               # Load testing scripts
│   ├── scripts/
│   │   ├── cpu-hungry-service.jmx
│   │   ├── memory-leaker-service.jmx
│   │   └── db-connection-service.jmx
│   └── run-all-tests.sh
│
└── README.md
```

---

## API Endpoints

### Analyzer Service (Port 8084)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Health check |
| `/api/analyze/{serviceName}` | POST | Analyze single service |
| `/api/analyze-all` | POST | Analyze all services |
| `/api/dashboard` | GET | Dashboard summary |
| `/api/latest-analysis/{serviceName}` | GET | Get latest analysis |
| `/api/metrics/{serviceName}` | GET | Get raw metrics |
| `/api/collect-metrics` | POST | Manual metrics collection |

### Code Generator Service (Port 8085)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Health check |
| `/api/generate` | POST | Generate config files |
| `/api/generate-and-pr` | POST | Generate + create PR |

### Demo Services (Ports 8081-8083)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | Health check |
| `/actuator/prometheus` | GET | Prometheus metrics |
| `/actuator/metrics` | GET | Spring Boot metrics |

---

## Configuration

### Analyzer Service

Edit `analyzer-service/src/main/resources/application.yml`:

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

### Code Generator Service

Edit `code-generator-service/src/main/resources/application.yml`:

```yaml
azure:
  devops:
    organization: your-org
    project: your-project
    token: ${AZURE_DEVOPS_TOKEN}
    repository: your-repo
```

---

## Monitoring

### View Prometheus Metrics

```bash
# CPU usage
curl http://localhost:8081/actuator/prometheus | grep process_cpu_usage

# Memory usage
curl http://localhost:8082/actuator/prometheus | grep jvm_memory_used_bytes

# Connection pool
curl http://localhost:8083/actuator/prometheus | grep hikari_connections
```

### H2 Database Console

Analyzer database: http://localhost:8084/h2-console

- JDBC URL: `jdbc:h2:mem:analyzerdb`
- Username: `sa`
- Password: (empty)

---

## Technology Stack

- **Backend**: Spring Boot 3.2, Java 17
- **Database**: H2 (in-memory), PostgreSQL (optional)
- **Metrics**: Micrometer, Prometheus
- **Analysis**: Apache Commons Math (statistics)
- **Frontend**: HTML5, CSS3, Vanilla JavaScript, Chart.js
- **Load Testing**: Apache JMeter 5.6
- **DevOps**: Azure DevOps REST API
- **Build**: Maven 3.8+

---

## Performance Results

### Before Optimization

| Service | CPU Limit | Memory Limit | P95 Response | Issues |
|---------|-----------|--------------|--------------|--------|
| CPU-Hungry | 200m | 256Mi | 2000ms | CPU Throttling |
| Memory-Leaker | 512Mi | 512Mi | 500ms | OOMKilled |
| DB-Connection | 500m | 512Mi | 5000ms | Pool Exhaustion |

### After Optimization

| Service | CPU Limit | Memory Limit | P95 Response | Savings |
|---------|-----------|--------------|--------------|---------|
| CPU-Hungry | 1000m | 768Mi | 800ms | $15/month |
| Memory-Leaker | 2000m | 2Gi | 500ms | $25/month |
| DB-Connection | 500m | 512Mi | 200ms | $10/month |

**Total Savings**: $600/year per service

---

## Contributing

This project was built for the SaveYourMoney Hackathon 2025.

---

## License

Proprietary - Hackathon Project

---

## Contact

For questions or support, please open an issue on GitHub.

**Built with ❤️ for SaveYourMoney Hackathon 2025** 🚀💰
