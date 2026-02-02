// Firebase Firestore API Service - SaveYourMoney Dashboard

// Firebase Configuration (will be initialized from firebase-config.js)
let db = null;

// Initialize Firebase
function initializeFirebase() {
    // Firebase config should be loaded from external file
    // This will be set in index.html
    if (typeof firebase === 'undefined') {
        console.error('Firebase SDK not loaded');
        return false;
    }

    try {
        // Check if firebase config is available
        if (typeof firebaseConfig === 'undefined') {
            console.error('Firebase config not found. Please update firebase-config.js');
            return false;
        }

        // Initialize Firebase
        if (!firebase.apps.length) {
            firebase.initializeApp(firebaseConfig);
        }

        db = firebase.firestore();
        console.log('✅ Firebase initialized successfully');
        return true;
    } catch (error) {
        console.error('❌ Error initializing Firebase:', error);
        return false;
    }
}

const FirestoreAPI = {
    // Get dashboard metrics
    async getMetrics() {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            const doc = await db.collection('metrics').doc('current').get();

            if (doc.exists) {
                return doc.data();
            } else {
                console.warn('No metrics document found');
                return {
                    totalAnnualSavings: 0,
                    savingsPercentage: 0,
                    idleCpuCores: 0,
                    wastedRamGB: 0,
                    aiRecommendationsCount: 0
                };
            }
        } catch (error) {
            console.error('Error fetching metrics:', error);
            throw error;
        }
    },

    // Get all recommendations
    async getRecommendations(limit = 12) {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            const snapshot = await db.collection('recommendations')
                .orderBy('potentialSavingsMonthly', 'desc')
                .limit(limit)
                .get();

            const recommendations = [];
            snapshot.forEach(doc => {
                recommendations.push({
                    id: doc.id,
                    ...doc.data()
                });
            });

            return recommendations;
        } catch (error) {
            console.error('Error fetching recommendations:', error);
            throw error;
        }
    },

    // Get single recommendation by ID
    async getRecommendation(id) {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            const doc = await db.collection('recommendations').doc(id).get();

            if (doc.exists) {
                return {
                    id: doc.id,
                    ...doc.data()
                };
            }
            return null;
        } catch (error) {
            console.error('Error fetching recommendation:', error);
            throw error;
        }
    },

    // Get AI insights
    async getInsights() {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            const snapshot = await db.collection('insights')
                .orderBy('createdAt', 'desc')
                .get();

            const insights = [];
            snapshot.forEach(doc => {
                insights.push({
                    id: doc.id,
                    ...doc.data()
                });
            });

            return insights;
        } catch (error) {
            console.error('Error fetching insights:', error);
            throw error;
        }
    },

    // Get deployments
    async getDeployments() {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            const snapshot = await db.collection('deployments').get();

            const deployments = [];
            snapshot.forEach(doc => {
                deployments.push({
                    id: doc.id,
                    ...doc.data()
                });
            });

            return deployments;
        } catch (error) {
            console.error('Error fetching deployments:', error);
            throw error;
        }
    },

    // Get recommendations by priority
    async getRecommendationsByPriority(priority) {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            const snapshot = await db.collection('recommendations')
                .where('priority', '==', priority)
                .orderBy('potentialSavingsMonthly', 'desc')
                .get();

            const recommendations = [];
            snapshot.forEach(doc => {
                recommendations.push({
                    id: doc.id,
                    ...doc.data()
                });
            });

            return recommendations;
        } catch (error) {
            console.error('Error fetching recommendations by priority:', error);
            throw error;
        }
    },

    // Real-time listener for metrics
    onMetricsChange(callback) {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        return db.collection('metrics').doc('current')
            .onSnapshot(doc => {
                if (doc.exists) {
                    callback(doc.data());
                }
            }, error => {
                console.error('Error in metrics listener:', error);
            });
    },

    // Real-time listener for recommendations
    onRecommendationsChange(callback) {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        return db.collection('recommendations')
            .orderBy('potentialSavingsMonthly', 'desc')
            .onSnapshot(snapshot => {
                const recommendations = [];
                snapshot.forEach(doc => {
                    recommendations.push({
                        id: doc.id,
                        ...doc.data()
                    });
                });
                callback(recommendations);
            }, error => {
                console.error('Error in recommendations listener:', error);
            });
    },

    // Update recommendation status
    async updateRecommendationStatus(id, status) {
        if (!db) {
            throw new Error('Firebase not initialized');
        }

        try {
            await db.collection('recommendations').doc(id).update({
                status: status,
                updatedAt: firebase.firestore.Timestamp.now()
            });
            return true;
        } catch (error) {
            console.error('Error updating recommendation status:', error);
            throw error;
        }
    }
};

// Export for use in other files
window.FirestoreAPI = FirestoreAPI;
window.initializeFirebase = initializeFirebase;
