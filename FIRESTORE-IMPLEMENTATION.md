# 🔥 Firestore Implementation Guide

Complete Firestore database design, schema, indexes, and data loading scripts for SaveYourMoney project.

---

## 📋 Table of Contents

1. [Database Architecture](#database-architecture)
2. [Collection Schemas](#collection-schemas)
3. [Security Rules](#security-rules)
4. [Indexes](#indexes)
5. [Dummy Data Scripts](#dummy-data-scripts)
6. [Setup Instructions](#setup-instructions)
7. [Query Examples](#query-examples)

---

## 1. Database Architecture

### Collections Overview

```
Firestore Database (saveyourmoney)
│
├── services/                    # Service metadata and configuration
│   ├── {serviceId}/            # Document per service
│   │   ├── metadata            # Service info (name, type, status)
│   │   └── currentConfig       # Current resource configuration
│
├── metrics/                     # Time-series metrics data
│   └── {metricId}/             # Document per metric snapshot
│       ├── serviceName
│       ├── timestamp
│       ├── cpuUsage
│       ├── memoryUsage
│       └── ...
│
├── recommendations/             # Analysis results and recommendations
│   └── {recommendationId}/     # Document per recommendation
│       ├── serviceName
│       ├── analysisDate
│       ├── kubernetesResources
│       ├── jvmConfiguration
│       └── costAnalysis
│
├── deployments/                 # Deployment history
│   └── {deploymentId}/         # Document per deployment
│       ├── serviceName
│       ├── deploymentDate
│       ├── status
│       ├── prUrl
│       └── changes
│
├── cost-tracking/              # Cost analysis over time
│   └── {trackingId}/           # Document per cost snapshot
│       ├── date
│       ├── totalMonthlyCost
│       ├── breakdown
│       └── trends
│
└── alerts/                     # Performance alerts
    └── {alertId}/              # Document per alert
        ├── serviceName
        ├── alertType
        ├── severity
        ├── timestamp
        └── details
```

---

## 2. Collection Schemas

### 2.1 Services Collection

**Collection:** `services`
**Purpose:** Store service metadata and current configuration

**Document Structure:**
```javascript
{
  // Document ID: service name (e.g., "analyzer-service")

  // Metadata
  "serviceName": "analyzer-service",
  "serviceType": "WEB_API", // WEB_API, BACKGROUND_JOB, DATABASE_HEAVY, etc.
  "status": "running", // running, stopped, error
  "environment": "production", // production, staging, development
  "createdAt": Timestamp,
  "updatedAt": Timestamp,

  // Endpoints
  "baseUrl": "https://analyzer-service-xxxxx.run.app",
  "healthCheckUrl": "https://analyzer-service-xxxxx.run.app/api/health",
  "metricsUrl": "https://analyzer-service-xxxxx.run.app/actuator/prometheus",

  // Current Configuration
  "currentConfig": {
    "cpu": {
      "request": "250m",
      "limit": "1000m"
    },
    "memory": {
      "request": "512Mi",
      "limit": "1024Mi"
    },
    "replicas": 2,
    "jvm": {
      "heapMin": "256m",
      "heapMax": "716m"
    }
  },

  // Monitoring
  "monitoring": {
    "enabled": true,
    "collectionInterval": 10, // seconds
    "lastMetricTimestamp": Timestamp
  },

  // Tags
  "tags": ["backend", "ai", "optimization"],
  "team": "platform-team",
  "owner": "devops@company.com"
}
```

### 2.2 Metrics Collection

**Collection:** `metrics`
**Purpose:** Store time-series metrics snapshots

**Document Structure:**
```javascript
{
  // Document ID: auto-generated

  // Identification
  "serviceName": "analyzer-service",
  "timestamp": Timestamp,

  // CPU Metrics
  "cpuUsage": 0.45, // 45%
  "cpuUsageMillis": 450, // 450m
  "cpuLimit": 1000, // 1000m
  "cpuThrottling": false,

  // Memory Metrics
  "memoryUsageBytes": 536870912, // 512MB in bytes
  "memoryUsageMb": 512,
  "memoryLimitBytes": 1073741824, // 1GB in bytes
  "memoryLimitMb": 1024,
  "heapUsedBytes": 402653184, // ~384MB
  "heapMaxBytes": 751619276, // ~716MB
  "heapUsagePercent": 53.5,

  // GC Metrics
  "gcPauseSeconds": 0.025, // 25ms
  "gcCount": 15,
  "gcTotalTime": 0.375, // Total GC time in seconds

  // Request Metrics
  "requestCount": 1523,
  "requestLatencyP50": 45, // milliseconds
  "requestLatencyP95": 120,
  "requestLatencyP99": 250,
  "errorRate": 0.02, // 2%

  // Database Connection Pool (if applicable)
  "dbConnectionsActive": 8,
  "dbConnectionsIdle": 12,
  "dbConnectionsMax": 20,
  "dbConnectionWaitTime": 5, // milliseconds

  // Thread Pool
  "threadPoolActive": 15,
  "threadPoolMax": 50,
  "threadPoolQueueSize": 25,

  // Metadata
  "collectedBy": "MetricsCollectorService",
  "version": "1.0"
}
```

### 2.3 Recommendations Collection

**Collection:** `recommendations`
**Purpose:** Store AI analysis results and resource recommendations

**Document Structure:**
```javascript
{
  // Document ID: auto-generated

  // Identification
  "serviceName": "analyzer-service",
  "recommendationId": "rec_20260203_001",
  "analysisDate": Timestamp,
  "version": "1.0",

  // Analysis Metadata
  "confidenceScore": 0.92, // 92% confidence
  "samplesAnalyzed": 360, // 1 hour of 10s intervals
  "analysisWindow": {
    "startTime": Timestamp,
    "endTime": Timestamp,
    "durationMinutes": 60
  },

  // Detected Issues
  "detectedIssues": {
    "CPU Throttling": "CPU usage P95 exceeds 80% - causing request delays",
    "Memory Leak": "Memory usage increased 35% over analysis window"
  },
  "issueCount": 2,
  "severity": "high", // low, medium, high, critical

  // Kubernetes Resources Recommendation
  "kubernetesResources": {
    "cpuRequest": "500m",
    "cpuLimit": "1500m",
    "memoryRequest": "768Mi",
    "memoryLimit": "2048Mi",
    "replicas": 3
  },

  // JVM Configuration
  "jvmConfiguration": {
    "heapSizeMin": "384m",
    "heapSizeMax": "1433m",
    "gcAlgorithm": "G1GC",
    "gcThreads": 2,
    "javaOpts": "-Xms384m -Xmx1433m -XX:+UseG1GC -XX:ParallelGCThreads=2"
  },

  // Connection Pool Config
  "connectionPoolConfig": {
    "minimumIdle": 10,
    "maximumPoolSize": 30,
    "connectionTimeout": 30000,
    "idleTimeout": 600000,
    "maxLifetime": 1800000
  },

  // Thread Pool Config
  "threadPoolConfig": {
    "corePoolSize": 20,
    "maxPoolSize": 60,
    "queueCapacity": 150,
    "keepAliveSeconds": 60
  },

  // Cost Analysis
  "costAnalysis": {
    "currentMonthlyCost": 12.50,
    "recommendedMonthlyCost": 18.75,
    "monthlySavings": -6.25, // Negative = cost increase for better performance
    "annualSavings": -75.00,
    "costIncrease": true,
    "roi": "Better performance justifies 50% cost increase",
    "breakdown": {
      "currentCpu": 7.50,
      "currentMemory": 5.00,
      "recommendedCpu": 11.25,
      "recommendedMemory": 7.50
    }
  },

  // Statistical Data
  "statistics": {
    "cpu": {
      "mean": 0.62,
      "p50": 0.58,
      "p95": 0.85,
      "p99": 0.92,
      "max": 0.98,
      "stdDev": 0.15
    },
    "memory": {
      "mean": 614572800,
      "p50": 598671872,
      "p95": 751619276,
      "p99": 805306368,
      "max": 825456789,
      "stdDev": 104857600
    }
  },

  // Action Required
  "actionRequired": true,
  "priority": "high",
  "estimatedImpact": "Eliminates CPU throttling, prevents OOMKilled errors",

  // Implementation Status
  "implementationStatus": "pending", // pending, pr_created, approved, deployed, rejected
  "prUrl": null,
  "deploymentId": null,

  // Created By
  "createdBy": "ResourceAnalyzerService",
  "approvedBy": null,
  "approvedAt": null
}
```

### 2.4 Deployments Collection

**Collection:** `deployments`
**Purpose:** Track deployment history and PR status

**Document Structure:**
```javascript
{
  // Document ID: auto-generated

  // Identification
  "deploymentId": "deploy_20260203_001",
  "serviceName": "analyzer-service",
  "recommendationId": "rec_20260203_001",

  // Dates
  "createdAt": Timestamp,
  "deployedAt": Timestamp,
  "completedAt": Timestamp,

  // Status
  "status": "completed", // pending, in_progress, completed, failed, rolled_back
  "stage": "production", // development, staging, production

  // Pull Request Info
  "pullRequest": {
    "url": "https://dev.azure.com/org/project/_git/repo/pullrequest/123",
    "prNumber": 123,
    "title": "Optimize analyzer-service resources based on AI analysis",
    "description": "Auto-generated PR with resource optimizations",
    "author": "SaveYourMoney Bot",
    "reviewers": ["devops-team@company.com"],
    "status": "merged", // open, merged, closed, rejected
    "mergedAt": Timestamp,
    "mergedBy": "john.doe@company.com"
  },

  // Changes Applied
  "changes": {
    "kubernetes": {
      "before": {
        "cpuRequest": "250m",
        "cpuLimit": "1000m",
        "memoryRequest": "512Mi",
        "memoryLimit": "1024Mi"
      },
      "after": {
        "cpuRequest": "500m",
        "cpuLimit": "1500m",
        "memoryRequest": "768Mi",
        "memoryLimit": "2048Mi"
      }
    },
    "jvm": {
      "before": { "heapMax": "716m" },
      "after": { "heapMax": "1433m" }
    }
  },

  // Cost Impact
  "costImpact": {
    "before": 12.50,
    "after": 18.75,
    "difference": 6.25,
    "percentChange": 50.0
  },

  // Performance Impact (measured post-deployment)
  "performanceImpact": {
    "cpuThrottlingBefore": true,
    "cpuThrottlingAfter": false,
    "responseTimeP95Before": 250,
    "responseTimeP95After": 120,
    "improvement": "52% faster P95 response time"
  },

  // Files Modified
  "filesModified": [
    "k8s/analyzer-service-deployment.yaml",
    "analyzer-service/src/main/resources/application-prod.properties"
  ],

  // Metadata
  "automatedDeployment": true,
  "rollbackPlan": "Revert to previous deployment.yaml version",
  "notes": "Automated optimization based on 60 minutes of production metrics"
}
```

### 2.5 Cost Tracking Collection

**Collection:** `cost-tracking`
**Purpose:** Track costs over time for trending and reporting

**Document Structure:**
```javascript
{
  // Document ID: date-based (e.g., "2026-02-03")

  "date": Timestamp,
  "period": "daily", // daily, weekly, monthly

  // Total Costs
  "totalMonthlyCost": 245.80,
  "totalAnnualCost": 2949.60,

  // Breakdown by Service
  "services": {
    "analyzer-service": {
      "cpu": 11.25,
      "memory": 7.50,
      "total": 18.75,
      "replicas": 3
    },
    "code-generator-service": {
      "cpu": 11.25,
      "memory": 7.50,
      "total": 18.75,
      "replicas": 3
    },
    "cpu-hungry-service": {
      "cpu": 7.50,
      "memory": 2.50,
      "total": 10.00,
      "replicas": 2
    }
  },

  // Trends
  "trends": {
    "dailyChange": -5.20, // Cost reduction
    "weeklyChange": -18.50,
    "monthlyChange": -65.30,
    "percentChange": -21.0 // 21% cost reduction
  },

  // Optimization Impact
  "optimizations": {
    "totalOptimized": 5, // Number of services optimized
    "savingsGenerated": 65.30,
    "costIncreasesJustified": 2, // Performance improvements
    "netSavings": 45.80
  },

  // Projections
  "projections": {
    "nextMonthEstimate": 230.50,
    "annualProjection": 2766.00,
    "potentialSavingsRemaining": 120.00 // If all services optimized
  }
}
```

### 2.6 Alerts Collection

**Collection:** `alerts`
**Purpose:** Store performance and resource alerts

**Document Structure:**
```javascript
{
  // Document ID: auto-generated

  "alertId": "alert_20260203_001",
  "serviceName": "memory-leaker-service",
  "timestamp": Timestamp,

  // Alert Details
  "alertType": "MEMORY_LEAK", // CPU_THROTTLING, MEMORY_LEAK, CONNECTION_EXHAUSTION, HIGH_ERROR_RATE
  "severity": "critical", // low, medium, high, critical
  "title": "Memory Leak Detected",
  "description": "Memory usage increased 45% over last hour - potential memory leak",

  // Trigger Conditions
  "triggerConditions": {
    "metric": "memory_usage",
    "threshold": 1.2, // 20% growth
    "actual": 1.45, // 45% growth
    "window": "1 hour"
  },

  // Impact
  "impact": {
    "currentMemoryUsage": "945Mi",
    "memoryLimit": "1024Mi",
    "utilizationPercent": 92.3,
    "estimatedTimeToOOM": "15 minutes"
  },

  // Recommendation
  "recommendation": "Immediate action required - increase memory limit or fix memory leak",
  "recommendationId": "rec_20260203_005",
  "automatedAction": "PR created with increased memory limit",

  // Status
  "status": "open", // open, acknowledged, resolved, false_positive
  "acknowledgedBy": null,
  "acknowledgedAt": null,
  "resolvedAt": null,
  "resolution": null,

  // Notification
  "notificationSent": true,
  "notificationChannels": ["email", "slack"],
  "recipients": ["oncall@company.com", "devops-team@company.com"]
}
```

---

## 3. Security Rules

**File:** `firestore.rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isAdmin() {
      return isAuthenticated() &&
             request.auth.token.role == 'admin';
    }

    function isDevOps() {
      return isAuthenticated() &&
             (request.auth.token.role == 'admin' ||
              request.auth.token.role == 'devops');
    }

    // Services Collection
    // Read: Anyone (for dashboard)
    // Write: DevOps and Admin only
    match /services/{serviceId} {
      allow read: if true;
      allow create, update: if isDevOps();
      allow delete: if isAdmin();
    }

    // Metrics Collection
    // Read: Anyone (for dashboard)
    // Write: Authenticated services only
    match /metrics/{metricId} {
      allow read: if true;
      allow create: if isAuthenticated();
      allow update, delete: if isAdmin();
    }

    // Recommendations Collection
    // Read: Anyone (for dashboard)
    // Write: Analyzer service and admin
    match /recommendations/{recommendationId} {
      allow read: if true;
      allow create: if isAuthenticated();
      allow update: if isDevOps();
      allow delete: if isAdmin();
    }

    // Deployments Collection
    // Read: Anyone (for dashboard)
    // Write: Code generator service and admin
    match /deployments/{deploymentId} {
      allow read: if true;
      allow create: if isAuthenticated();
      allow update: if isDevOps();
      allow delete: if isAdmin();
    }

    // Cost Tracking Collection
    // Read: Anyone (for dashboard)
    // Write: System only
    match /cost-tracking/{trackingId} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Alerts Collection
    // Read: Anyone (for dashboard)
    // Write: Monitoring system and admin
    match /alerts/{alertId} {
      allow read: if true;
      allow create: if isAuthenticated();
      allow update: if isDevOps();
      allow delete: if isAdmin();
    }
  }
}
```

**For Development/Testing (Permissive Rules):**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

---

## 4. Indexes

**File:** `firestore.indexes.json`

```json
{
  "indexes": [
    {
      "collectionGroup": "metrics",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "serviceName", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "metrics",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "serviceName", "order": "ASCENDING" },
        { "fieldPath": "cpuUsage", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "recommendations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "serviceName", "order": "ASCENDING" },
        { "fieldPath": "analysisDate", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "recommendations",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "implementationStatus", "order": "ASCENDING" },
        { "fieldPath": "priority", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "deployments",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "serviceName", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "deployments",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "alerts",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "serviceName", "order": "ASCENDING" },
        { "fieldPath": "severity", "order": "DESCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "alerts",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "timestamp", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "cost-tracking",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "period", "order": "ASCENDING" },
        { "fieldPath": "date", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

---

## 5. Dummy Data Scripts

### 5.1 Setup Script

**File:** `firebase-setup/setup-firestore.js`

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: serviceAccount.project_id
});

const db = admin.firestore();

console.log('✅ Firebase Admin initialized');
console.log(`📦 Project ID: ${serviceAccount.project_id}`);

module.exports = { db, admin };
```

### 5.2 Load Dummy Data Script

**File:** `firebase-setup/load-dummy-data.js`

```javascript
const { db, admin } = require('./setup-firestore');

async function loadDummyData() {
  console.log('🚀 Loading dummy data into Firestore...\n');

  try {
    // 1. Load Services
    await loadServices();

    // 2. Load Metrics
    await loadMetrics();

    // 3. Load Recommendations
    await loadRecommendations();

    // 4. Load Deployments
    await loadDeployments();

    // 5. Load Cost Tracking
    await loadCostTracking();

    // 6. Load Alerts
    await loadAlerts();

    console.log('\n✅ All dummy data loaded successfully!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error loading data:', error);
    process.exit(1);
  }
}

// ===== 1. SERVICES =====
async function loadServices() {
  console.log('📋 Loading services...');

  const services = [
    {
      id: 'analyzer-service',
      data: {
        serviceName: 'analyzer-service',
        serviceType: 'WEB_API',
        status: 'running',
        environment: 'production',
        createdAt: admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now(),
        baseUrl: 'https://analyzer-service-xxxxx.run.app',
        healthCheckUrl: 'https://analyzer-service-xxxxx.run.app/api/health',
        metricsUrl: 'https://analyzer-service-xxxxx.run.app/actuator/prometheus',
        currentConfig: {
          cpu: { request: '250m', limit: '1000m' },
          memory: { request: '512Mi', limit: '1024Mi' },
          replicas: 2,
          jvm: { heapMin: '256m', heapMax: '716m' }
        },
        monitoring: {
          enabled: true,
          collectionInterval: 10,
          lastMetricTimestamp: admin.firestore.Timestamp.now()
        },
        tags: ['backend', 'ai', 'optimization'],
        team: 'platform-team',
        owner: 'devops@company.com'
      }
    },
    {
      id: 'code-generator-service',
      data: {
        serviceName: 'code-generator-service',
        serviceType: 'WEB_API',
        status: 'running',
        environment: 'production',
        createdAt: admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now(),
        baseUrl: 'https://code-generator-service-xxxxx.run.app',
        healthCheckUrl: 'https://code-generator-service-xxxxx.run.app/api/health',
        metricsUrl: 'https://code-generator-service-xxxxx.run.app/actuator/prometheus',
        currentConfig: {
          cpu: { request: '250m', limit: '1000m' },
          memory: { request: '512Mi', limit: '1024Mi' },
          replicas: 2,
          jvm: { heapMin: '256m', heapMax: '716m' }
        },
        monitoring: {
          enabled: true,
          collectionInterval: 10,
          lastMetricTimestamp: admin.firestore.Timestamp.now()
        },
        tags: ['backend', 'automation', 'codegen'],
        team: 'platform-team',
        owner: 'devops@company.com'
      }
    },
    {
      id: 'cpu-hungry-service',
      data: {
        serviceName: 'cpu-hungry-service',
        serviceType: 'CPU_INTENSIVE',
        status: 'running',
        environment: 'production',
        createdAt: admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now(),
        baseUrl: 'https://cpu-hungry-service-xxxxx.run.app',
        healthCheckUrl: 'https://cpu-hungry-service-xxxxx.run.app/api/health',
        metricsUrl: 'https://cpu-hungry-service-xxxxx.run.app/actuator/prometheus',
        currentConfig: {
          cpu: { request: '200m', limit: '500m' },
          memory: { request: '256Mi', limit: '512Mi' },
          replicas: 1,
          jvm: { heapMin: '128m', heapMax: '358m' }
        },
        monitoring: {
          enabled: true,
          collectionInterval: 10,
          lastMetricTimestamp: admin.firestore.Timestamp.now()
        },
        tags: ['demo', 'cpu-intensive'],
        team: 'demo-team',
        owner: 'demo@company.com'
      }
    },
    {
      id: 'memory-leaker-service',
      data: {
        serviceName: 'memory-leaker-service',
        serviceType: 'MEMORY_INTENSIVE',
        status: 'running',
        environment: 'production',
        createdAt: admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now(),
        baseUrl: 'https://memory-leaker-service-xxxxx.run.app',
        healthCheckUrl: 'https://memory-leaker-service-xxxxx.run.app/api/health',
        metricsUrl: 'https://memory-leaker-service-xxxxx.run.app/actuator/prometheus',
        currentConfig: {
          cpu: { request: '250m', limit: '500m' },
          memory: { request: '512Mi', limit: '1024Mi' },
          replicas: 1,
          jvm: { heapMin: '256m', heapMax: '716m' }
        },
        monitoring: {
          enabled: true,
          collectionInterval: 10,
          lastMetricTimestamp: admin.firestore.Timestamp.now()
        },
        tags: ['demo', 'memory-leak'],
        team: 'demo-team',
        owner: 'demo@company.com'
      }
    },
    {
      id: 'db-connection-service',
      data: {
        serviceName: 'db-connection-service',
        serviceType: 'DATABASE_HEAVY',
        status: 'running',
        environment: 'production',
        createdAt: admin.firestore.Timestamp.now(),
        updatedAt: admin.firestore.Timestamp.now(),
        baseUrl: 'https://db-connection-service-xxxxx.run.app',
        healthCheckUrl: 'https://db-connection-service-xxxxx.run.app/api/health',
        metricsUrl: 'https://db-connection-service-xxxxx.run.app/actuator/prometheus',
        currentConfig: {
          cpu: { request: '250m', limit: '500m' },
          memory: { request: '512Mi', limit: '1024Mi' },
          replicas: 1,
          jvm: { heapMin: '256m', heapMax: '716m' }
        },
        monitoring: {
          enabled: true,
          collectionInterval: 10,
          lastMetricTimestamp: admin.firestore.Timestamp.now()
        },
        tags: ['demo', 'database-heavy'],
        team: 'demo-team',
        owner: 'demo@company.com'
      }
    }
  ];

  const batch = db.batch();
  services.forEach(service => {
    const ref = db.collection('services').doc(service.id);
    batch.set(ref, service.data);
  });
  await batch.commit();

  console.log(`  ✅ Loaded ${services.length} services`);
}

// ===== 2. METRICS =====
async function loadMetrics() {
  console.log('📊 Loading metrics...');

  const services = ['analyzer-service', 'code-generator-service', 'cpu-hungry-service'];
  const metricsCount = 50; // 50 metrics per service
  let totalMetrics = 0;

  for (const serviceName of services) {
    const batch = db.batch();

    for (let i = 0; i < metricsCount; i++) {
      const timestamp = new Date();
      timestamp.setMinutes(timestamp.getMinutes() - (metricsCount - i) * 2); // Every 2 minutes

      const metric = {
        serviceName,
        timestamp: admin.firestore.Timestamp.fromDate(timestamp),

        // Vary CPU based on service
        cpuUsage: serviceName === 'cpu-hungry-service'
          ? 0.70 + Math.random() * 0.25  // 70-95%
          : 0.30 + Math.random() * 0.30, // 30-60%
        cpuUsageMillis: serviceName === 'cpu-hungry-service' ? 350 + Math.random() * 125 : 75 + Math.random() * 75,
        cpuLimit: serviceName === 'cpu-hungry-service' ? 500 : 1000,
        cpuThrottling: serviceName === 'cpu-hungry-service' && Math.random() > 0.7,

        // Memory usage (with leak simulation for memory-leaker)
        memoryUsageBytes: 400000000 + Math.random() * 200000000 + (i * 5000000), // Gradual increase
        memoryUsageMb: 400 + Math.random() * 200 + (i * 5),
        memoryLimitBytes: 1073741824,
        memoryLimitMb: 1024,
        heapUsedBytes: 300000000 + Math.random() * 150000000,
        heapMaxBytes: 751619276,
        heapUsagePercent: 40 + Math.random() * 30,

        // GC
        gcPauseSeconds: 0.010 + Math.random() * 0.040,
        gcCount: 10 + Math.floor(Math.random() * 10),
        gcTotalTime: 0.250 + Math.random() * 0.200,

        // Requests
        requestCount: 1000 + Math.floor(Math.random() * 1000),
        requestLatencyP50: 30 + Math.random() * 40,
        requestLatencyP95: 80 + Math.random() * 100,
        requestLatencyP99: 150 + Math.random() * 150,
        errorRate: Math.random() * 0.05,

        // DB
        dbConnectionsActive: 5 + Math.floor(Math.random() * 10),
        dbConnectionsIdle: 8 + Math.floor(Math.random() * 12),
        dbConnectionsMax: 20,
        dbConnectionWaitTime: Math.random() * 20,

        // Threads
        threadPoolActive: 10 + Math.floor(Math.random() * 20),
        threadPoolMax: 50,
        threadPoolQueueSize: Math.floor(Math.random() * 40),

        collectedBy: 'MetricsCollectorService',
        version: '1.0'
      };

      const ref = db.collection('metrics').doc();
      batch.set(ref, metric);
      totalMetrics++;
    }

    await batch.commit();
  }

  console.log(`  ✅ Loaded ${totalMetrics} metrics across ${services.length} services`);
}

// ===== 3. RECOMMENDATIONS =====
async function loadRecommendations() {
  console.log('💡 Loading recommendations...');

  const recommendations = [
    {
      serviceName: 'cpu-hungry-service',
      recommendationId: 'rec_20260203_001',
      analysisDate: admin.firestore.Timestamp.now(),
      version: '1.0',
      confidenceScore: 0.92,
      samplesAnalyzed: 360,
      analysisWindow: {
        startTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 3600000)),
        endTime: admin.firestore.Timestamp.now(),
        durationMinutes: 60
      },
      detectedIssues: {
        'CPU Throttling': 'CPU usage P95 exceeds 85% - causing request delays',
        'Insufficient Resources': 'Current limits too low for workload'
      },
      issueCount: 2,
      severity: 'high',
      kubernetesResources: {
        cpuRequest: '500m',
        cpuLimit: '1500m',
        memoryRequest: '512Mi',
        memoryLimit: '1024Mi',
        replicas: 2
      },
      jvmConfiguration: {
        heapSizeMin: '256m',
        heapSizeMax: '716m',
        gcAlgorithm: 'G1GC',
        gcThreads: 2,
        javaOpts: '-Xms256m -Xmx716m -XX:+UseG1GC'
      },
      connectionPoolConfig: {
        minimumIdle: 5,
        maximumPoolSize: 15,
        connectionTimeout: 30000,
        idleTimeout: 600000,
        maxLifetime: 1800000
      },
      threadPoolConfig: {
        corePoolSize: 10,
        maxPoolSize: 30,
        queueCapacity: 100,
        keepAliveSeconds: 60
      },
      costAnalysis: {
        currentMonthlyCost: 8.50,
        recommendedMonthlyCost: 17.50,
        monthlySavings: -9.00,
        annualSavings: -108.00,
        costIncrease: true,
        roi: 'Performance improvement justifies 106% cost increase',
        breakdown: {
          currentCpu: 6.00,
          currentMemory: 2.50,
          recommendedCpu: 12.50,
          recommendedMemory: 5.00
        }
      },
      statistics: {
        cpu: {
          mean: 0.78,
          p50: 0.75,
          p95: 0.87,
          p99: 0.93,
          max: 0.98,
          stdDev: 0.12
        }
      },
      actionRequired: true,
      priority: 'high',
      estimatedImpact: 'Eliminates CPU throttling, reduces P95 latency by 60%',
      implementationStatus: 'pending',
      prUrl: null,
      deploymentId: null,
      createdBy: 'ResourceAnalyzerService',
      approvedBy: null,
      approvedAt: null
    },
    {
      serviceName: 'memory-leaker-service',
      recommendationId: 'rec_20260203_002',
      analysisDate: admin.firestore.Timestamp.now(),
      version: '1.0',
      confidenceScore: 0.88,
      samplesAnalyzed: 360,
      analysisWindow: {
        startTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 3600000)),
        endTime: admin.firestore.Timestamp.now(),
        durationMinutes: 60
      },
      detectedIssues: {
        'Memory Leak': 'Memory usage increased 35% over analysis window',
        'OOM Risk': 'Approaching memory limit - OOMKilled risk'
      },
      issueCount: 2,
      severity: 'critical',
      kubernetesResources: {
        cpuRequest: '250m',
        cpuLimit: '500m',
        memoryRequest: '1024Mi',
        memoryLimit: '2048Mi',
        replicas: 1
      },
      jvmConfiguration: {
        heapSizeMin: '512m',
        heapSizeMax: '1433m',
        gcAlgorithm: 'G1GC',
        gcThreads: 2,
        javaOpts: '-Xms512m -Xmx1433m -XX:+UseG1GC'
      },
      connectionPoolConfig: {
        minimumIdle: 5,
        maximumPoolSize: 20,
        connectionTimeout: 30000,
        idleTimeout: 600000,
        maxLifetime: 1800000
      },
      threadPoolConfig: {
        corePoolSize: 10,
        maxPoolSize: 50,
        queueCapacity: 100,
        keepAliveSeconds: 60
      },
      costAnalysis: {
        currentMonthlyCost: 8.75,
        recommendedMonthlyCost: 12.50,
        monthlySavings: -3.75,
        annualSavings: -45.00,
        costIncrease: true,
        roi: 'Prevents OOMKilled errors and service downtime',
        breakdown: {
          currentCpu: 6.25,
          currentMemory: 2.50,
          recommendedCpu: 7.50,
          recommendedMemory: 5.00
        }
      },
      statistics: {
        memory: {
          mean: 750000000,
          p50: 700000000,
          p95: 950000000,
          p99: 1000000000,
          max: 1050000000,
          stdDev: 150000000
        }
      },
      actionRequired: true,
      priority: 'critical',
      estimatedImpact: 'Prevents OOMKilled errors, allows time to fix memory leak',
      implementationStatus: 'pr_created',
      prUrl: 'https://dev.azure.com/org/project/_git/repo/pullrequest/456',
      deploymentId: null,
      createdBy: 'ResourceAnalyzerService',
      approvedBy: null,
      approvedAt: null
    }
  ];

  const batch = db.batch();
  recommendations.forEach(rec => {
    const ref = db.collection('recommendations').doc();
    batch.set(ref, rec);
  });
  await batch.commit();

  console.log(`  ✅ Loaded ${recommendations.length} recommendations`);
}

// ===== 4. DEPLOYMENTS =====
async function loadDeployments() {
  console.log('🚀 Loading deployments...');

  const deployments = [
    {
      deploymentId: 'deploy_20260201_001',
      serviceName: 'analyzer-service',
      recommendationId: 'rec_20260131_005',
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 172800000)), // 2 days ago
      deployedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 172000000)),
      completedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 171900000)),
      status: 'completed',
      stage: 'production',
      pullRequest: {
        url: 'https://dev.azure.com/org/project/_git/repo/pullrequest/101',
        prNumber: 101,
        title: 'Optimize analyzer-service resources',
        description: 'Auto-generated PR with resource optimizations',
        author: 'SaveYourMoney Bot',
        reviewers: ['devops-team@company.com'],
        status: 'merged',
        mergedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 172000000)),
        mergedBy: 'john.doe@company.com'
      },
      changes: {
        kubernetes: {
          before: { cpuRequest: '200m', cpuLimit: '500m', memoryRequest: '256Mi', memoryLimit: '512Mi' },
          after: { cpuRequest: '250m', cpuLimit: '1000m', memoryRequest: '512Mi', memoryLimit: '1024Mi' }
        },
        jvm: {
          before: { heapMax: '358m' },
          after: { heapMax: '716m' }
        }
      },
      costImpact: {
        before: 7.50,
        after: 12.50,
        difference: 5.00,
        percentChange: 66.67
      },
      performanceImpact: {
        cpuThrottlingBefore: true,
        cpuThrottlingAfter: false,
        responseTimeP95Before: 350,
        responseTimeP95After: 150,
        improvement: '57% faster P95 response time'
      },
      filesModified: [
        'k8s/analyzer-service-deployment.yaml',
        'analyzer-service/src/main/resources/application-prod.properties'
      ],
      automatedDeployment: true,
      rollbackPlan: 'Revert to previous deployment.yaml version',
      notes: 'Successfully eliminated CPU throttling'
    },
    {
      deploymentId: 'deploy_20260202_001',
      serviceName: 'code-generator-service',
      recommendationId: 'rec_20260201_008',
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 86400000)), // 1 day ago
      deployedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 86000000)),
      completedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 85900000)),
      status: 'completed',
      stage: 'production',
      pullRequest: {
        url: 'https://dev.azure.com/org/project/_git/repo/pullrequest/102',
        prNumber: 102,
        title: 'Optimize code-generator-service resources',
        description: 'Auto-generated PR with resource optimizations',
        author: 'SaveYourMoney Bot',
        reviewers: ['devops-team@company.com'],
        status: 'merged',
        mergedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 86000000)),
        mergedBy: 'jane.doe@company.com'
      },
      changes: {
        kubernetes: {
          before: { cpuRequest: '200m', cpuLimit: '500m', memoryRequest: '256Mi', memoryLimit: '512Mi' },
          after: { cpuRequest: '250m', cpuLimit: '1000m', memoryRequest: '512Mi', memoryLimit: '1024Mi' }
        },
        jvm: {
          before: { heapMax: '358m' },
          after: { heapMax: '716m' }
        }
      },
      costImpact: {
        before: 7.50,
        after: 12.50,
        difference: 5.00,
        percentChange: 66.67
      },
      performanceImpact: {
        cpuThrottlingBefore: false,
        cpuThrottlingAfter: false,
        responseTimeP95Before: 180,
        responseTimeP95After: 120,
        improvement: '33% faster P95 response time'
      },
      filesModified: [
        'k8s/code-generator-service-deployment.yaml',
        'code-generator-service/src/main/resources/application-prod.properties'
      ],
      automatedDeployment: true,
      rollbackPlan: 'Revert to previous deployment.yaml version',
      notes: 'Proactive optimization to handle increased load'
    },
    {
      deploymentId: 'deploy_20260203_001',
      serviceName: 'memory-leaker-service',
      recommendationId: 'rec_20260203_002',
      createdAt: admin.firestore.Timestamp.now(),
      deployedAt: null,
      completedAt: null,
      status: 'pending',
      stage: 'production',
      pullRequest: {
        url: 'https://dev.azure.com/org/project/_git/repo/pullrequest/456',
        prNumber: 456,
        title: 'URGENT: Increase memory limits for memory-leaker-service',
        description: 'Critical memory leak detected - temporary limit increase while investigating root cause',
        author: 'SaveYourMoney Bot',
        reviewers: ['oncall@company.com', 'devops-team@company.com'],
        status: 'open',
        mergedAt: null,
        mergedBy: null
      },
      changes: {
        kubernetes: {
          before: { cpuRequest: '250m', cpuLimit: '500m', memoryRequest: '512Mi', memoryLimit: '1024Mi' },
          after: { cpuRequest: '250m', cpuLimit: '500m', memoryRequest: '1024Mi', memoryLimit: '2048Mi' }
        },
        jvm: {
          before: { heapMax: '716m' },
          after: { heapMax: '1433m' }
        }
      },
      costImpact: {
        before: 8.75,
        after: 12.50,
        difference: 3.75,
        percentChange: 42.86
      },
      performanceImpact: null,
      filesModified: [
        'k8s/memory-leaker-service-deployment.yaml',
        'demo-services/memory-leaker-service/src/main/resources/application-prod.properties'
      ],
      automatedDeployment: true,
      rollbackPlan: 'Revert to previous deployment.yaml version',
      notes: 'Awaiting approval - critical memory leak requires immediate action'
    }
  ];

  const batch = db.batch();
  deployments.forEach(deploy => {
    const ref = db.collection('deployments').doc();
    batch.set(ref, deploy);
  });
  await batch.commit();

  console.log(`  ✅ Loaded ${deployments.length} deployments`);
}

// ===== 5. COST TRACKING =====
async function loadCostTracking() {
  console.log('💰 Loading cost tracking...');

  const costRecords = [];

  // Last 7 days of cost data
  for (let i = 7; i >= 0; i--) {
    const date = new Date();
    date.setDate(date.getDate() - i);
    date.setHours(0, 0, 0, 0);

    const baseCost = 245.80;
    const optimizationProgress = (7 - i) * 5; // $5/day savings as optimizations are applied

    costRecords.push({
      date: admin.firestore.Timestamp.fromDate(date),
      period: 'daily',
      totalMonthlyCost: baseCost - optimizationProgress,
      totalAnnualCost: (baseCost - optimizationProgress) * 12,
      services: {
        'analyzer-service': { cpu: 11.25, memory: 7.50, total: 18.75, replicas: 3 },
        'code-generator-service': { cpu: 11.25, memory: 7.50, total: 18.75, replicas: 3 },
        'cpu-hungry-service': { cpu: 7.50 - (i * 0.5), memory: 2.50, total: 10.00 - (i * 0.5), replicas: 2 },
        'memory-leaker-service': { cpu: 7.50, memory: 2.50 + (i * 0.2), total: 10.00 + (i * 0.2), replicas: 1 },
        'db-connection-service': { cpu: 7.50, memory: 2.50, total: 10.00, replicas: 1 }
      },
      trends: {
        dailyChange: i === 0 ? 0 : -5.00,
        weeklyChange: -optimizationProgress,
        monthlyChange: -optimizationProgress * 4,
        percentChange: -(optimizationProgress / baseCost) * 100
      },
      optimizations: {
        totalOptimized: Math.min(i + 1, 5),
        savingsGenerated: optimizationProgress,
        costIncreasesJustified: 2,
        netSavings: optimizationProgress - 10
      },
      projections: {
        nextMonthEstimate: baseCost - optimizationProgress - 10,
        annualProjection: (baseCost - optimizationProgress - 10) * 12,
        potentialSavingsRemaining: Math.max(0, 120 - optimizationProgress)
      }
    });
  }

  const batch = db.batch();
  costRecords.forEach(record => {
    const ref = db.collection('cost-tracking').doc();
    batch.set(ref, record);
  });
  await batch.commit();

  console.log(`  ✅ Loaded ${costRecords.length} cost tracking records`);
}

// ===== 6. ALERTS =====
async function loadAlerts() {
  console.log('🚨 Loading alerts...');

  const alerts = [
    {
      alertId: 'alert_20260203_001',
      serviceName: 'memory-leaker-service',
      timestamp: admin.firestore.Timestamp.now(),
      alertType: 'MEMORY_LEAK',
      severity: 'critical',
      title: 'Memory Leak Detected',
      description: 'Memory usage increased 45% over last hour - potential memory leak',
      triggerConditions: {
        metric: 'memory_usage',
        threshold: 1.2,
        actual: 1.45,
        window: '1 hour'
      },
      impact: {
        currentMemoryUsage: '945Mi',
        memoryLimit: '1024Mi',
        utilizationPercent: 92.3,
        estimatedTimeToOOM: '15 minutes'
      },
      recommendation: 'Immediate action required - increase memory limit or fix memory leak',
      recommendationId: 'rec_20260203_002',
      automatedAction: 'PR #456 created with increased memory limit',
      status: 'open',
      acknowledgedBy: null,
      acknowledgedAt: null,
      resolvedAt: null,
      resolution: null,
      notificationSent: true,
      notificationChannels: ['email', 'slack'],
      recipients: ['oncall@company.com', 'devops-team@company.com']
    },
    {
      alertId: 'alert_20260202_001',
      serviceName: 'cpu-hungry-service',
      timestamp: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 86400000)),
      alertType: 'CPU_THROTTLING',
      severity: 'high',
      title: 'CPU Throttling Detected',
      description: 'CPU usage P95 exceeds 85% - requests experiencing latency',
      triggerConditions: {
        metric: 'cpu_usage_p95',
        threshold: 0.80,
        actual: 0.87,
        window: '30 minutes'
      },
      impact: {
        currentCpuUsage: '435m',
        cpuLimit: '500m',
        utilizationPercent: 87.0,
        p95Latency: '850ms (2x normal)'
      },
      recommendation: 'Increase CPU limits to eliminate throttling',
      recommendationId: 'rec_20260203_001',
      automatedAction: 'Analysis completed, recommendation generated',
      status: 'acknowledged',
      acknowledgedBy: 'devops@company.com',
      acknowledgedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 82800000)),
      resolvedAt: null,
      resolution: null,
      notificationSent: true,
      notificationChannels: ['email'],
      recipients: ['devops-team@company.com']
    },
    {
      alertId: 'alert_20260201_001',
      serviceName: 'db-connection-service',
      timestamp: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 172800000)),
      alertType: 'CONNECTION_EXHAUSTION',
      severity: 'medium',
      title: 'Connection Pool Near Capacity',
      description: 'Database connection pool at 85% capacity',
      triggerConditions: {
        metric: 'db_connections_active',
        threshold: 16,
        actual: 17,
        window: '10 minutes'
      },
      impact: {
        activeConnections: 17,
        maxConnections: 20,
        utilizationPercent: 85.0,
        waitTime: '125ms'
      },
      recommendation: 'Increase connection pool size to 30',
      recommendationId: 'rec_20260131_009',
      automatedAction: 'Configuration updated',
      status: 'resolved',
      acknowledgedBy: 'devops@company.com',
      acknowledgedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 172000000)),
      resolvedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 171000000)),
      resolution: 'Connection pool increased to 30, issue resolved',
      notificationSent: true,
      notificationChannels: ['email'],
      recipients: ['devops-team@company.com']
    }
  ];

  const batch = db.batch();
  alerts.forEach(alert => {
    const ref = db.collection('alerts').doc();
    batch.set(ref, alert);
  });
  await batch.commit();

  console.log(`  ✅ Loaded ${alerts.length} alerts`);
}

// Run the script
loadDummyData();
```

### 5.3 Package.json

**File:** `firebase-setup/package.json`

```json
{
  "name": "saveyourmoney-firestore-setup",
  "version": "1.0.0",
  "description": "Firestore setup and data loading scripts for SaveYourMoney",
  "main": "setup-firestore.js",
  "scripts": {
    "load-data": "node load-dummy-data.js",
    "clear-data": "node clear-firestore.js"
  },
  "dependencies": {
    "firebase-admin": "^12.0.0"
  },
  "author": "SaveYourMoney Team",
  "license": "MIT"
}
```

### 5.4 Clear Data Script (Bonus)

**File:** `firebase-setup/clear-firestore.js`

```javascript
const { db } = require('./setup-firestore');

async function clearAllCollections() {
  console.log('🗑️  Clearing all Firestore collections...\n');

  const collections = [
    'services',
    'metrics',
    'recommendations',
    'deployments',
    'cost-tracking',
    'alerts'
  ];

  try {
    for (const collectionName of collections) {
      console.log(`Deleting ${collectionName}...`);
      const snapshot = await db.collection(collectionName).get();

      if (snapshot.empty) {
        console.log(`  ℹ️  ${collectionName} is already empty`);
        continue;
      }

      const batch = db.batch();
      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      await batch.commit();

      console.log(`  ✅ Deleted ${snapshot.size} documents from ${collectionName}`);
    }

    console.log('\n✅ All collections cleared successfully!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Error clearing collections:', error);
    process.exit(1);
  }
}

clearAllCollections();
```

---

## 6. Setup Instructions

### 6.1 Prerequisites

1. **GCP Project** with Firestore enabled
2. **Service Account Key** (JSON file)
3. **Node.js** 18+ installed

### 6.2 Step-by-Step Setup

```bash
# 1. Navigate to firebase-setup directory
cd firebase-setup

# 2. Install dependencies
npm install

# 3. Place your service account key
# Download from: GCP Console → IAM & Admin → Service Accounts → Create Key (JSON)
# Save as: serviceAccountKey.json

# 4. Update firebase-config.json with your project ID (if needed)
{
  "projectId": "your-gcp-project-id"
}

# 5. Load dummy data
npm run load-data

# 6. Verify data in Firestore Console
# https://console.cloud.google.com/firestore/data
```

### 6.3 Security Rules Deployment

```bash
# Deploy security rules from GCP Console UI:
# Firestore → Rules → Paste rules from section 3 → Publish

# Or use Firebase CLI (if installed):
firebase deploy --only firestore:rules
```

### 6.4 Indexes Deployment

```bash
# Deploy indexes from GCP Console UI:
# Firestore → Indexes → Create composite indexes from section 4

# Or use Firebase CLI:
firebase deploy --only firestore:indexes
```

---

## 7. Query Examples

### 7.1 JavaScript/TypeScript (Dashboard)

```javascript
// Get all services
const servicesSnapshot = await db.collection('services').get();
const services = servicesSnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));

// Get latest metrics for a service
const metricsSnapshot = await db.collection('metrics')
  .where('serviceName', '==', 'analyzer-service')
  .orderBy('timestamp', 'desc')
  .limit(50)
  .get();

// Get pending recommendations
const recommendationsSnapshot = await db.collection('recommendations')
  .where('implementationStatus', '==', 'pending')
  .orderBy('priority', 'desc')
  .get();

// Get recent deployments
const deploymentsSnapshot = await db.collection('deployments')
  .orderBy('createdAt', 'desc')
  .limit(10)
  .get();

// Get open alerts
const alertsSnapshot = await db.collection('alerts')
  .where('status', '==', 'open')
  .where('severity', 'in', ['high', 'critical'])
  .orderBy('timestamp', 'desc')
  .get();

// Get cost tracking for last 7 days
const costSnapshot = await db.collection('cost-tracking')
  .where('period', '==', 'daily')
  .orderBy('date', 'desc')
  .limit(7)
  .get();
```

### 7.2 Java (Spring Boot Backend)

```java
// Inject Firestore
@Autowired
private Firestore firestore;

// Save metric
CollectionReference metricsRef = firestore.collection("metrics");
DocumentReference newMetric = metricsRef.document();
newMetric.set(metricData).get();

// Query recommendations
Query query = firestore.collection("recommendations")
    .whereEqualTo("serviceName", "analyzer-service")
    .orderBy("analysisDate", Query.Direction.DESCENDING)
    .limit(10);

ApiFuture<QuerySnapshot> querySnapshot = query.get();
List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
```

---

## 8. Testing

```bash
# Test Firestore connection
node -e "require('./setup-firestore'); console.log('✅ Connection successful');"

# Load test data
npm run load-data

# Clear all data
npm run clear-data

# Reload fresh data
npm run clear-data && npm run load-data
```

---

## 📊 Summary

**6 Collections** designed for complete SaveYourMoney functionality:
- ✅ **services** - Service metadata (5 documents)
- ✅ **metrics** - Time-series metrics (150+ documents)
- ✅ **recommendations** - AI recommendations (2 documents)
- ✅ **deployments** - Deployment history (3 documents)
- ✅ **cost-tracking** - Cost analysis (8 documents)
- ✅ **alerts** - Performance alerts (3 documents)

**Total: 171+ sample documents** ready for testing!

---

**Created By**: Claude Sonnet 4.5
**Date**: 2026-02-03
**Version**: 1.0
