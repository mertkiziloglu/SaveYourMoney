/**
 * Firestore Mock Data Insertion Script
 *
 * This script inserts mock data into Firestore for the SaveYourMoney dashboard.
 * Run this after setting up your Firebase project and service account key.
 *
 * Usage:
 *   npm install
 *   node insert-mock-data.js
 */

const admin = require('firebase-admin');
const path = require('path');

// Initialize Firebase Admin SDK
try {
  const serviceAccount = require('./serviceAccountKey.json');

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });

  console.log('✅ Firebase Admin SDK initialized successfully');
} catch (error) {
  console.error('❌ Error initializing Firebase Admin SDK:');
  console.error('   Make sure serviceAccountKey.json exists in this directory');
  console.error('   Download it from Firebase Console > Project Settings > Service accounts');
  process.exit(1);
}

const db = admin.firestore();

// Mock Data Definitions
const mockData = {
  // Dashboard Metrics
  metrics: {
    id: 'current',
    totalAnnualSavings: 124500,
    savingsPercentage: 15,
    idleCpuCores: 45,
    wastedRamGB: 256,
    aiRecommendationsCount: 12,
    lastUpdated: admin.firestore.Timestamp.now()
  },

  // Recommendations (Optimization Opportunities)
  recommendations: [
    {
      id: 'backend-payment-service',
      namespace: 'production-cluster-01',
      deploymentName: 'backend-payment-service',
      currentConfig: {
        cpu: 4,
        memory: 8
      },
      recommendedConfig: {
        cpu: 2,
        memory: 4
      },
      potentialSavingsMonthly: 450,
      status: 'pending',
      priority: 'high',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'frontend-dashboard-ui',
      namespace: 'production-cluster-01',
      deploymentName: 'frontend-dashboard-ui',
      currentConfig: {
        cpu: 2,
        memory: 4
      },
      recommendedConfig: {
        cpu: 0.5,
        memory: 1
      },
      potentialSavingsMonthly: 180,
      status: 'pending',
      priority: 'medium',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'redis-cache-worker',
      namespace: 'dev-cluster-west-2',
      deploymentName: 'redis-cache-worker',
      currentConfig: {
        cpu: 8,
        memory: 32
      },
      recommendedConfig: {
        cpu: 4,
        memory: 16
      },
      potentialSavingsMonthly: 890,
      status: 'pending',
      priority: 'high',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'api-gateway',
      namespace: 'production-cluster-01',
      deploymentName: 'api-gateway',
      currentConfig: {
        cpu: 3,
        memory: 6
      },
      recommendedConfig: {
        cpu: 1.5,
        memory: 3
      },
      potentialSavingsMonthly: 320,
      status: 'pending',
      priority: 'medium',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'notification-service',
      namespace: 'production-cluster-01',
      deploymentName: 'notification-service',
      currentConfig: {
        cpu: 1,
        memory: 2
      },
      recommendedConfig: {
        cpu: 0.25,
        memory: 0.5
      },
      potentialSavingsMonthly: 95,
      status: 'pending',
      priority: 'low',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'auth-service',
      namespace: 'production-cluster-01',
      deploymentName: 'auth-service',
      currentConfig: {
        cpu: 2,
        memory: 4
      },
      recommendedConfig: {
        cpu: 1,
        memory: 2
      },
      potentialSavingsMonthly: 210,
      status: 'pending',
      priority: 'medium',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'logging-service',
      namespace: 'dev-cluster-west-2',
      deploymentName: 'logging-service',
      currentConfig: {
        cpu: 4,
        memory: 16
      },
      recommendedConfig: {
        cpu: 2,
        memory: 8
      },
      potentialSavingsMonthly: 420,
      status: 'pending',
      priority: 'high',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'queue-worker',
      namespace: 'production-cluster-01',
      deploymentName: 'queue-worker',
      currentConfig: {
        cpu: 6,
        memory: 12
      },
      recommendedConfig: {
        cpu: 3,
        memory: 6
      },
      potentialSavingsMonthly: 615,
      status: 'pending',
      priority: 'high',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'analytics-pipeline',
      namespace: 'dev-cluster-west-2',
      deploymentName: 'analytics-pipeline',
      currentConfig: {
        cpu: 5,
        memory: 20
      },
      recommendedConfig: {
        cpu: 2.5,
        memory: 10
      },
      potentialSavingsMonthly: 550,
      status: 'pending',
      priority: 'medium',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'search-indexer',
      namespace: 'production-cluster-01',
      deploymentName: 'search-indexer',
      currentConfig: {
        cpu: 3,
        memory: 8
      },
      recommendedConfig: {
        cpu: 1.5,
        memory: 4
      },
      potentialSavingsMonthly: 380,
      status: 'pending',
      priority: 'medium',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'backup-service',
      namespace: 'qa-cluster-central',
      deploymentName: 'backup-service',
      currentConfig: {
        cpu: 2,
        memory: 6
      },
      recommendedConfig: {
        cpu: 0.5,
        memory: 2
      },
      potentialSavingsMonthly: 240,
      status: 'pending',
      priority: 'low',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'monitoring-agent',
      namespace: 'production-cluster-01',
      deploymentName: 'monitoring-agent',
      currentConfig: {
        cpu: 1,
        memory: 3
      },
      recommendedConfig: {
        cpu: 0.5,
        memory: 1.5
      },
      potentialSavingsMonthly: 125,
      status: 'pending',
      priority: 'low',
      createdAt: admin.firestore.Timestamp.now(),
      updatedAt: admin.firestore.Timestamp.now()
    }
  ],

  // AI Insights
  insights: [
    {
      id: 'cpu-throttling',
      type: 'cpu_throttling',
      severity: 'high',
      title: 'CPU Throttling',
      description: 'High throttling in payment-gateway. Consider scaling.',
      affectedService: 'payment-gateway',
      recommendation: 'Increase CPU limits from 2 cores to 4 cores',
      potentialImpact: 'Improved response times and reduced request failures',
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'memory-leak',
      type: 'memory_leak',
      severity: 'medium',
      title: 'Memory Leaks',
      description: 'analytics-worker shows gradual leak patterns.',
      affectedService: 'analytics-worker',
      recommendation: 'Review memory allocation and implement proper garbage collection',
      potentialImpact: 'Prevent OOM crashes and reduce memory costs',
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'spot-instances',
      type: 'cost_optimization',
      severity: 'low',
      title: 'Spot Instances',
      description: 'Move data-processing to Spot to save 60%.',
      affectedService: 'data-processing',
      recommendation: 'Migrate non-critical workloads to Spot instances',
      potentialImpact: 'Save up to 60% on compute costs for batch jobs',
      createdAt: admin.firestore.Timestamp.now()
    }
  ],

  // Deployments (Full deployment info) - 15 deployments
  deployments: [
    {
      id: 'payment-gateway-v2',
      name: 'payment-gateway-v2',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 3,
      currentResources: {
        cpuRequest: '350m',
        cpuLimit: '1000m',
        memoryRequest: '620Mi',
        memoryLimit: '2Gi'
      },
      recommendedResources: {
        cpuRequest: '500m',
        cpuLimit: '800m',
        memoryRequest: '800Mi',
        memoryLimit: '1.5Gi'
      },
      metrics: {
        cpuUsagePercent: 35,
        memoryUsagePercent: 31,
        requestsPerSecond: 850,
        errorRate: 0.01
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 250,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-15')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'auth-service',
      name: 'auth-service',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 2,
      currentResources: {
        cpuRequest: '1800m',
        cpuLimit: '2000m',
        memoryRequest: '1.2Gi',
        memoryLimit: '4Gi'
      },
      recommendedResources: {
        cpuRequest: '1800m',
        cpuLimit: '2000m',
        memoryRequest: '1.2Gi',
        memoryLimit: '4Gi'
      },
      metrics: {
        cpuUsagePercent: 90,
        memoryUsagePercent: 30,
        requestsPerSecond: 1500,
        errorRate: 0.02
      },
      status: 'Warning',
      healthStatus: 'warning',
      hasRecommendation: false,
      potentialSavings: 0,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-18')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'notification-worker',
      name: 'notification-worker',
      namespace: 'staging',
      cluster: 'gcp-europe-west',
      environment: 'Staging',
      replicas: 2,
      currentResources: {
        cpuRequest: '50m',
        cpuLimit: '500m',
        memoryRequest: '128Mi',
        memoryLimit: '1Gi'
      },
      recommendedResources: {
        cpuRequest: '100m',
        cpuLimit: '300m',
        memoryRequest: '256Mi',
        memoryLimit: '512Mi'
      },
      metrics: {
        cpuUsagePercent: 10,
        memoryUsagePercent: 12,
        requestsPerSecond: 45,
        errorRate: 0.001
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 85,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-20')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'analytics-ingest',
      name: 'analytics-ingest',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 4,
      currentResources: {
        cpuRequest: '1200m',
        cpuLimit: '4000m',
        memoryRequest: '3Gi',
        memoryLimit: '8Gi'
      },
      recommendedResources: {
        cpuRequest: '1500m',
        cpuLimit: '2500m',
        memoryRequest: '4Gi',
        memoryLimit: '6Gi'
      },
      metrics: {
        cpuUsagePercent: 30,
        memoryUsagePercent: 37,
        requestsPerSecond: 2100,
        errorRate: 0.005
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 420,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-12')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'frontend-react',
      name: 'frontend-react',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 3,
      currentResources: {
        cpuRequest: '100m',
        cpuLimit: '100m',
        memoryRequest: '250Mi',
        memoryLimit: '256Mi'
      },
      recommendedResources: {
        cpuRequest: '200m',
        cpuLimit: '400m',
        memoryRequest: '512Mi',
        memoryLimit: '1Gi'
      },
      metrics: {
        cpuUsagePercent: 100,
        memoryUsagePercent: 97,
        requestsPerSecond: 3500,
        errorRate: 0.08
      },
      status: 'Critical',
      healthStatus: 'critical',
      hasRecommendation: true,
      potentialSavings: -150,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-05')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'backend-api-v1',
      name: 'backend-api-v1',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 5,
      currentResources: {
        cpuRequest: '500m',
        cpuLimit: '1000m',
        memoryRequest: '1Gi',
        memoryLimit: '2Gi'
      },
      recommendedResources: {
        cpuRequest: '300m',
        cpuLimit: '600m',
        memoryRequest: '768Mi',
        memoryLimit: '1.5Gi'
      },
      metrics: {
        cpuUsagePercent: 28,
        memoryUsagePercent: 45,
        requestsPerSecond: 1850,
        errorRate: 0.015
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 340,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-22')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'search-service',
      name: 'search-service',
      namespace: 'prod',
      cluster: 'gcp-europe-west',
      environment: 'Production',
      replicas: 3,
      currentResources: {
        cpuRequest: '2000m',
        cpuLimit: '4000m',
        memoryRequest: '4Gi',
        memoryLimit: '8Gi'
      },
      recommendedResources: {
        cpuRequest: '1500m',
        cpuLimit: '3000m',
        memoryRequest: '3Gi',
        memoryLimit: '6Gi'
      },
      metrics: {
        cpuUsagePercent: 42,
        memoryUsagePercent: 55,
        requestsPerSecond: 950,
        errorRate: 0.012
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 520,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-14')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'queue-processor',
      name: 'queue-processor',
      namespace: 'prod',
      cluster: 'azure-asia-south',
      environment: 'Production',
      replicas: 6,
      currentResources: {
        cpuRequest: '800m',
        cpuLimit: '1500m',
        memoryRequest: '2Gi',
        memoryLimit: '4Gi'
      },
      recommendedResources: {
        cpuRequest: '600m',
        cpuLimit: '1200m',
        memoryRequest: '1.5Gi',
        memoryLimit: '3Gi'
      },
      metrics: {
        cpuUsagePercent: 38,
        memoryUsagePercent: 42,
        requestsPerSecond: 0,
        errorRate: 0.008
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 380,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-19')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'email-service',
      name: 'email-service',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 2,
      currentResources: {
        cpuRequest: '200m',
        cpuLimit: '500m',
        memoryRequest: '512Mi',
        memoryLimit: '1Gi'
      },
      recommendedResources: {
        cpuRequest: '150m',
        cpuLimit: '300m',
        memoryRequest: '384Mi',
        memoryLimit: '768Mi'
      },
      metrics: {
        cpuUsagePercent: 22,
        memoryUsagePercent: 35,
        requestsPerSecond: 120,
        errorRate: 0.003
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 95,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-21')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'reporting-engine',
      name: 'reporting-engine',
      namespace: 'prod',
      cluster: 'gcp-europe-west',
      environment: 'Production',
      replicas: 2,
      currentResources: {
        cpuRequest: '1000m',
        cpuLimit: '2000m',
        memoryRequest: '2Gi',
        memoryLimit: '4Gi'
      },
      recommendedResources: {
        cpuRequest: '800m',
        cpuLimit: '1500m',
        memoryRequest: '1.5Gi',
        memoryLimit: '3Gi'
      },
      metrics: {
        cpuUsagePercent: 45,
        memoryUsagePercent: 48,
        requestsPerSecond: 85,
        errorRate: 0.01
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 210,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-16')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'cache-redis',
      name: 'cache-redis',
      namespace: 'prod',
      cluster: 'aws-useast-1',
      environment: 'Production',
      replicas: 3,
      currentResources: {
        cpuRequest: '500m',
        cpuLimit: '1000m',
        memoryRequest: '4Gi',
        memoryLimit: '8Gi'
      },
      recommendedResources: {
        cpuRequest: '300m',
        cpuLimit: '600m',
        memoryRequest: '3Gi',
        memoryLimit: '6Gi'
      },
      metrics: {
        cpuUsagePercent: 25,
        memoryUsagePercent: 52,
        requestsPerSecond: 5500,
        errorRate: 0.001
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 290,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-13')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'data-pipeline',
      name: 'data-pipeline',
      namespace: 'staging',
      cluster: 'gcp-europe-west',
      environment: 'Staging',
      replicas: 1,
      currentResources: {
        cpuRequest: '3000m',
        cpuLimit: '6000m',
        memoryRequest: '8Gi',
        memoryLimit: '16Gi'
      },
      recommendedResources: {
        cpuRequest: '2000m',
        cpuLimit: '4000m',
        memoryRequest: '6Gi',
        memoryLimit: '12Gi'
      },
      metrics: {
        cpuUsagePercent: 35,
        memoryUsagePercent: 48,
        requestsPerSecond: 0,
        errorRate: 0.005
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 650,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-11')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'websocket-server',
      name: 'websocket-server',
      namespace: 'prod',
      cluster: 'azure-asia-south',
      environment: 'Production',
      replicas: 4,
      currentResources: {
        cpuRequest: '400m',
        cpuLimit: '800m',
        memoryRequest: '1Gi',
        memoryLimit: '2Gi'
      },
      recommendedResources: {
        cpuRequest: '600m',
        cpuLimit: '1200m',
        memoryRequest: '1.5Gi',
        memoryLimit: '3Gi'
      },
      metrics: {
        cpuUsagePercent: 72,
        memoryUsagePercent: 68,
        requestsPerSecond: 0,
        errorRate: 0.015
      },
      status: 'Warning',
      healthStatus: 'warning',
      hasRecommendation: true,
      potentialSavings: -180,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-17')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'image-processor',
      name: 'image-processor',
      namespace: 'dev',
      cluster: 'aws-useast-1',
      environment: 'Development',
      replicas: 1,
      currentResources: {
        cpuRequest: '1500m',
        cpuLimit: '3000m',
        memoryRequest: '3Gi',
        memoryLimit: '6Gi'
      },
      recommendedResources: {
        cpuRequest: '1000m',
        cpuLimit: '2000m',
        memoryRequest: '2Gi',
        memoryLimit: '4Gi'
      },
      metrics: {
        cpuUsagePercent: 32,
        memoryUsagePercent: 40,
        requestsPerSecond: 25,
        errorRate: 0.002
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: true,
      potentialSavings: 310,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-23')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'scheduler-cron',
      name: 'scheduler-cron',
      namespace: 'prod',
      cluster: 'gcp-europe-west',
      environment: 'Production',
      replicas: 1,
      currentResources: {
        cpuRequest: '100m',
        cpuLimit: '200m',
        memoryRequest: '256Mi',
        memoryLimit: '512Mi'
      },
      recommendedResources: {
        cpuRequest: '100m',
        cpuLimit: '200m',
        memoryRequest: '256Mi',
        memoryLimit: '512Mi'
      },
      metrics: {
        cpuUsagePercent: 15,
        memoryUsagePercent: 28,
        requestsPerSecond: 0,
        errorRate: 0
      },
      status: 'Healthy',
      healthStatus: 'healthy',
      hasRecommendation: false,
      potentialSavings: 0,
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-09')),
      createdAt: admin.firestore.Timestamp.now()
    }
  ]
};

// Insert data into Firestore
async function insertMockData() {
  try {
    console.log('\n🚀 Starting mock data insertion...\n');

    // Insert metrics
    console.log('📊 Inserting metrics...');
    await db.collection('metrics').doc(mockData.metrics.id).set(mockData.metrics);
    console.log(`   ✅ Inserted metrics document: ${mockData.metrics.id}`);

    // Insert recommendations
    console.log('\n💡 Inserting recommendations...');
    const batch1 = db.batch();
    for (const rec of mockData.recommendations) {
      const ref = db.collection('recommendations').doc(rec.id);
      batch1.set(ref, rec);
    }
    await batch1.commit();
    console.log(`   ✅ Inserted ${mockData.recommendations.length} recommendations`);

    // Insert insights
    console.log('\n🔍 Inserting AI insights...');
    const batch2 = db.batch();
    for (const insight of mockData.insights) {
      const ref = db.collection('insights').doc(insight.id);
      batch2.set(ref, insight);
    }
    await batch2.commit();
    console.log(`   ✅ Inserted ${mockData.insights.length} insights`);

    // Insert deployments
    console.log('\n🚢 Inserting deployments...');
    const batch3 = db.batch();
    for (const deployment of mockData.deployments) {
      const ref = db.collection('deployments').doc(deployment.id);
      batch3.set(ref, deployment);
    }
    await batch3.commit();
    console.log(`   ✅ Inserted ${mockData.deployments.length} deployments`);

    console.log('\n✨ Mock data insertion completed successfully!\n');
    console.log('📊 Summary:');
    console.log(`   - Metrics: 1 document`);
    console.log(`   - Recommendations: ${mockData.recommendations.length} documents`);
    console.log(`   - Insights: ${mockData.insights.length} documents`);
    console.log(`   - Deployments: ${mockData.deployments.length} documents`);
    console.log('\n🎉 You can now view the data in Firebase Console:\n');
    console.log(`   https://console.firebase.google.com/project/${admin.app().options.projectId}/firestore\n`);

  } catch (error) {
    console.error('\n❌ Error inserting mock data:', error);
    process.exit(1);
  }
}

// Run the script
insertMockData()
  .then(() => {
    console.log('✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
