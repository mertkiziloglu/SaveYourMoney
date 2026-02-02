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

  // Deployments (Full deployment info)
  deployments: [
    {
      id: 'backend-payment-service',
      name: 'backend-payment-service',
      namespace: 'production-cluster-01',
      cluster: 'prod-cluster-east-1',
      environment: 'Production',
      replicas: 3,
      currentResources: {
        cpuRequest: '4',
        cpuLimit: '4',
        memoryRequest: '8Gi',
        memoryLimit: '8Gi'
      },
      recommendedResources: {
        cpuRequest: '2',
        cpuLimit: '2',
        memoryRequest: '4Gi',
        memoryLimit: '4Gi'
      },
      metrics: {
        cpuUsagePercent: 45,
        memoryUsagePercent: 52,
        requestsPerSecond: 1250,
        errorRate: 0.02
      },
      status: 'running',
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-15')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'frontend-dashboard-ui',
      name: 'frontend-dashboard-ui',
      namespace: 'production-cluster-01',
      cluster: 'prod-cluster-east-1',
      environment: 'Production',
      replicas: 2,
      currentResources: {
        cpuRequest: '2',
        cpuLimit: '2',
        memoryRequest: '4Gi',
        memoryLimit: '4Gi'
      },
      recommendedResources: {
        cpuRequest: '0.5',
        cpuLimit: '0.5',
        memoryRequest: '1Gi',
        memoryLimit: '1Gi'
      },
      metrics: {
        cpuUsagePercent: 18,
        memoryUsagePercent: 25,
        requestsPerSecond: 450,
        errorRate: 0.01
      },
      status: 'running',
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-20')),
      createdAt: admin.firestore.Timestamp.now()
    },
    {
      id: 'redis-cache-worker',
      name: 'redis-cache-worker',
      namespace: 'dev-cluster-west-2',
      cluster: 'dev-cluster-west-2',
      environment: 'Development',
      replicas: 1,
      currentResources: {
        cpuRequest: '8',
        cpuLimit: '8',
        memoryRequest: '32Gi',
        memoryLimit: '32Gi'
      },
      recommendedResources: {
        cpuRequest: '4',
        cpuLimit: '4',
        memoryRequest: '16Gi',
        memoryLimit: '16Gi'
      },
      metrics: {
        cpuUsagePercent: 35,
        memoryUsagePercent: 48,
        requestsPerSecond: 850,
        errorRate: 0.005
      },
      status: 'running',
      lastDeployed: admin.firestore.Timestamp.fromDate(new Date('2024-01-10')),
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
