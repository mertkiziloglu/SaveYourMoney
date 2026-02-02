# JMeter Load Tests - SaveYourMoney

## Overview

This directory contains JMeter test scripts designed to stress-test the demo services and expose their resource configuration problems.

---

## 📁 Directory Structure

```
jmeter-tests/
├── scripts/                          # JMeter test plans (.jmx files)
│   ├── cpu-hungry-service.jmx       # CPU throttling test
│   ├── memory-leaker-service.jmx    # Memory leak test
│   └── db-connection-service.jmx    # Connection pool exhaustion test
├── results/                          # Test results (.jtl files)
├── reports/                          # HTML test reports
└── run-all-tests.sh                 # Orchestration script
```

---

## 🧪 Test Plans

### 1️⃣ CPU-Hungry-Service Test

**File:** `cpu-hungry-service.jmx`

**Configuration:**
- **Threads:** 100 concurrent users
- **Ramp-up:** 60 seconds
- **Duration:** 10 minutes (600 seconds)
- **Think time:** 500ms between requests

**Endpoints Hit:**
- `POST /api/hash` - SHA-256 hashing (1000 iterations)
- `GET /api/fibonacci` - Recursive Fibonacci (n=35)
- `GET /api/burn-cpu` - Combined CPU-intensive operations

**Expected Problem:** CPU throttling at 100m/200m limits → Slow response times

**Success Criteria:**
- ✅ CPU usage reaches 90%+
- ✅ Response time P95 > 2000ms
- ✅ Metrics show CPU throttling

---

### 2️⃣ Memory-Leaker-Service Test

**File:** `memory-leaker-service.jmx`

**Configuration:**
- **Threads:** 50 concurrent users
- **Ramp-up:** 60 seconds
- **Duration:** 15 minutes (900 seconds)
- **Think time:** 1000ms between requests (gradual growth)

**Endpoints Hit:**
- `POST /api/cache/add` - Add 100KB to leaky cache
- `POST /api/process/large` - Process 10MB data chunks
- `POST /api/session/create` - Create 200KB session data
- `POST /api/memory-bomb` - Rapid memory allocation (20 iterations)

**Expected Problem:** Memory leak → Gradual heap growth → OOMKilled

**Success Criteria:**
- ✅ Heap usage grows continuously
- ✅ Eventually reaches 512Mi limit
- ✅ Service crashes with OOMKilled

---

### 3️⃣ DB-Connection-Service Test

**File:** `db-connection-service.jmx`

**Configuration:**
- **Setup Thread:** Initialize test data (100 users, 500 orders)
- **Threads:** 200 concurrent users
- **Ramp-up:** 30 seconds
- **Duration:** 5 minutes (300 seconds)
- **Think time:** 200ms between requests (fast pace)

**Endpoints Hit:**
- `GET /api/users/slow-query` - Slow N+1 query pattern
- `GET /api/orders/complex-query` - JOIN FETCH query
- `GET /api/queries/multiple` - Sequential queries (count=10)
- `GET /api/connection-bomb` - Combined DB operations

**Expected Problem:** Connection pool exhaustion (max pool size = 5)

**Success Criteria:**
- ✅ HikariCP pool maxed out (5/5 active)
- ✅ Connection timeout errors
- ✅ Response time degradation

---

## 🚀 Running Tests

### Prerequisites

1. **Install JMeter:**

   ```bash
   # Mac
   brew install jmeter

   # Or download from https://jmeter.apache.org
   ```

2. **Start all demo services:**

   ```bash
   # Terminal 1 - CPU Hungry
   cd ~/Desktop/SaveYourMoney/demo-services/cpu-hungry-service
   mvn spring-boot:run

   # Terminal 2 - Memory Leaker
   cd ~/Desktop/SaveYourMoney/demo-services/memory-leaker-service
   mvn spring-boot:run

   # Terminal 3 - DB Connection
   cd ~/Desktop/SaveYourMoney/demo-services/db-connection-service
   mvn spring-boot:run
   ```

3. **Verify services are running:**

   ```bash
   curl http://localhost:8081/api/health  # CPU Hungry
   curl http://localhost:8082/api/health  # Memory Leaker
   curl http://localhost:8083/api/health  # DB Connection
   ```

---

### Option 1: Run All Tests (Orchestrated)

**Use the orchestration script:**

```bash
cd ~/Desktop/SaveYourMoney/jmeter-tests
./run-all-tests.sh
```

This will:
1. Check if all services are running
2. Run CPU test (10 minutes)
3. Wait 5 seconds
4. Run Memory test (15 minutes)
5. Wait 5 seconds
6. Run DB test (5 minutes)
7. Generate HTML reports

**Total runtime:** ~35 minutes

---

### Option 2: Run Individual Tests

**CPU-Hungry-Service:**

```bash
jmeter -n -t scripts/cpu-hungry-service.jmx \
  -l results/cpu-hungry-results.jtl \
  -e -o reports/cpu-hungry-report
```

**Memory-Leaker-Service:**

```bash
jmeter -n -t scripts/memory-leaker-service.jmx \
  -l results/memory-leaker-results.jtl \
  -e -o reports/memory-leaker-report
```

**DB-Connection-Service:**

```bash
jmeter -n -t scripts/db-connection-service.jmx \
  -l results/db-connection-results.jtl \
  -e -o reports/db-connection-report
```

---

### Option 3: JMeter GUI Mode (Development)

**Open test in GUI for editing:**

```bash
jmeter -t scripts/cpu-hungry-service.jmx
```

**Note:** GUI mode is for development only. Use non-GUI (`-n`) mode for actual load testing.

---

## 📊 Viewing Results

### HTML Reports

After tests complete, open the HTML reports in your browser:

```bash
# CPU Hungry Report
open reports/cpu-hungry-report/index.html

# Memory Leaker Report
open reports/memory-leaker-report/index.html

# DB Connection Report
open reports/db-connection-report/index.html
```

**Key Metrics to Check:**
- **Response Time:** P95, P99, Max
- **Throughput:** Requests/second
- **Error Rate:** Should increase as problems occur
- **Active Threads:** Over time graph

---

### Raw Results

CSV results are saved in `results/` directory:

```bash
# View summary statistics
cat results/cpu-hungry-results.jtl | tail -20

# Count total requests
wc -l results/cpu-hungry-results.jtl

# Count errors
grep "false" results/cpu-hungry-results.jtl | wc -l
```

---

## 🔬 Monitoring During Tests

### Watch Service Metrics

While tests are running, monitor the services:

**CPU Usage:**
```bash
curl http://localhost:8081/actuator/prometheus | grep process_cpu_usage
```

**Memory Usage:**
```bash
curl http://localhost:8082/actuator/prometheus | grep jvm_memory_used_bytes
```

**Connection Pool:**
```bash
curl http://localhost:8083/actuator/prometheus | grep hikari_connections
```

**Memory Stats (Memory Leaker):**
```bash
watch -n 2 'curl -s http://localhost:8082/api/stats'
```

---

## 🎯 After Tests Complete

### 1. Analyze with SaveYourMoney AI

```bash
# Wait for metrics to be collected (30 seconds)
sleep 30

# Analyze all services
curl -X POST http://localhost:8084/api/analyze-all

# Or analyze individually
curl -X POST http://localhost:8084/api/analyze/cpu-hungry-service
curl -X POST http://localhost:8084/api/analyze/memory-leaker-service
curl -X POST http://localhost:8084/api/analyze/db-connection-service
```

### 2. Generate Optimized Configurations

```bash
# Get recommendations
curl http://localhost:8084/api/analyze/cpu-hungry-service > recommendation.json

# Generate code
curl -X POST http://localhost:8085/api/generate-and-pr \
  -H "Content-Type: application/json" \
  -d @recommendation.json
```

### 3. View Dashboard

```bash
curl http://localhost:8084/api/dashboard | jq
```

---

## 📈 Expected Test Results

### CPU-Hungry-Service

**Before Optimization:**
- Response Time P95: ~2000ms
- Response Time P99: ~3000ms
- Throughput: ~30 req/sec
- Error Rate: 0-5%
- CPU Usage: 90-100% (throttled)

**After Optimization (500m/1000m):**
- Response Time P95: ~800ms
- Response Time P99: ~1200ms
- Throughput: ~100 req/sec
- Error Rate: 0%
- CPU Usage: 60-70%

---

### Memory-Leaker-Service

**Before Optimization:**
- Initial Heap: ~100MB
- After 5 min: ~300MB
- After 10 min: ~450MB
- After 15 min: OOMKilled (512Mi limit)
- Service crashes

**After Optimization (1Gi/2Gi):**
- Heap stabilizes around 800MB
- No OOM crashes
- Service remains stable

---

### DB-Connection-Service

**Before Optimization:**
- Active Connections: 5/5 (maxed out)
- Connection Timeout Errors: 40%+
- Response Time P95: ~5000ms
- Throughput: ~20 req/sec

**After Optimization (pool size: 50):**
- Active Connections: 15-25/50
- Connection Timeout Errors: 0%
- Response Time P95: ~200ms
- Throughput: ~150 req/sec

---

## 🛠️ Customization

### Change Test Duration

Edit the JMX files or use JMeter properties:

```bash
jmeter -n -t scripts/cpu-hungry-service.jmx \
  -Jduration=300 \
  -l results/cpu-short-test.jtl
```

### Change Number of Threads

```bash
jmeter -n -t scripts/cpu-hungry-service.jmx \
  -Jthreads=50 \
  -l results/cpu-light-load.jtl
```

### Change Target Host

```bash
jmeter -n -t scripts/cpu-hungry-service.jmx \
  -JHOST=my-service.example.com \
  -JPORT=80 \
  -l results/cpu-prod-test.jtl
```

---

## 🐛 Troubleshooting

### JMeter Not Found

```bash
# Check if installed
which jmeter

# Mac: Install via Homebrew
brew install jmeter

# Or set JMETER_HOME
export JMETER_HOME=/path/to/jmeter
export PATH=$JMETER_HOME/bin:$PATH
```

### Service Not Responding

```bash
# Check if service is running
curl http://localhost:8081/api/health

# Check logs
# (Look at the terminal where mvn spring-boot:run is running)

# Restart service if needed
# Ctrl+C to stop, then mvn spring-boot:run again
```

### Out of Memory (JMeter itself)

Increase JMeter heap:

```bash
export JVM_ARGS="-Xms512m -Xmx2048m"
jmeter -n -t scripts/cpu-hungry-service.jmx ...
```

### Permission Denied (run-all-tests.sh)

```bash
chmod +x run-all-tests.sh
```

---

## 📊 Test Report Contents

The HTML reports include:

1. **Dashboard** - Summary statistics
2. **Response Times Over Time** - Graph
3. **Active Threads Over Time** - Graph
4. **Throughput** - Requests/second
5. **Response Time Percentiles** - P50, P90, P95, P99
6. **Error Rate** - Percentage of failed requests
7. **Top 5 Errors** - Most common error messages

---

## 🎯 Success Criteria

Tests are successful if they **expose the problems**:

✅ CPU-Hungry: High CPU usage, slow response times
✅ Memory-Leaker: Gradual memory growth, eventual crash
✅ DB-Connection: Connection pool exhaustion, timeouts

Then SaveYourMoney AI can analyze and recommend fixes! 🚀

---

**Built for SaveYourMoney Hackathon 2025** 💰
