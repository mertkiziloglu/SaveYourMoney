// API Service - SaveYourMoney Dashboard

const API_BASE_URL = 'http://localhost:8084/api';
const CODE_GEN_URL = 'http://localhost:8085/api';

const API = {
    // Get dashboard summary
    async getDashboard() {
        const response = await fetch(`${API_BASE_URL}/dashboard`);
        if (!response.ok) throw new Error('Failed to fetch dashboard data');
        return response.json();
    },

    // Analyze specific service
    async analyzeService(serviceName) {
        const response = await fetch(`${API_BASE_URL}/analyze/${serviceName}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) throw new Error(`Failed to analyze ${serviceName}`);
        return response.json();
    },

    // Analyze all services
    async analyzeAll() {
        const response = await fetch(`${API_BASE_URL}/analyze-all`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) throw new Error('Failed to analyze all services');
        return response.json();
    },

    // Get latest analysis for a service
    async getLatestAnalysis(serviceName) {
        const response = await fetch(`${API_BASE_URL}/latest-analysis/${serviceName}`);
        if (!response.ok) return null;
        return response.json();
    },

    // Get metrics for a service
    async getMetrics(serviceName, limit = 50) {
        const response = await fetch(`${API_BASE_URL}/metrics/${serviceName}?limit=${limit}`);
        if (!response.ok) throw new Error(`Failed to fetch metrics for ${serviceName}`);
        return response.json();
    },

    // Generate code files
    async generateCode(serviceName, recommendation) {
        const response = await fetch(`${CODE_GEN_URL}/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(recommendation)
        });
        if (!response.ok) throw new Error('Failed to generate code');
        return response.json();
    },

    // Create Pull Request
    async createPR(serviceName, recommendation) {
        const response = await fetch(`${CODE_GEN_URL}/generate-and-pr`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(recommendation)
        });
        if (!response.ok) throw new Error('Failed to create PR');
        return response.json();
    },

    // ========== Anomaly Detection APIs ==========

    // Get active anomalies
    async getActiveAnomalies() {
        const response = await fetch(`${API_BASE_URL}/anomalies/active`);
        if (!response.ok) throw new Error('Failed to fetch active anomalies');
        return response.json();
    },

    // Get service anomalies
    async getServiceAnomalies(serviceName, limit = 50) {
        const response = await fetch(`${API_BASE_URL}/anomalies/${serviceName}?limit=${limit}`);
        if (!response.ok) throw new Error(`Failed to fetch anomalies for ${serviceName}`);
        return response.json();
    },

    // Get anomaly statistics
    async getAnomalyStats() {
        const response = await fetch(`${API_BASE_URL}/anomalies/stats`);
        if (!response.ok) throw new Error('Failed to fetch anomaly stats');
        return response.json();
    },

    // Resolve anomaly
    async resolveAnomaly(anomalyId) {
        const response = await fetch(`${API_BASE_URL}/anomalies/${anomalyId}/resolve`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) throw new Error('Failed to resolve anomaly');
        return response.json();
    },

    // Get anomaly timeline
    async getAnomalyTimeline(serviceName, hoursAgo = 24) {
        const response = await fetch(`${API_BASE_URL}/anomalies/timeline/${serviceName}?hoursAgo=${hoursAgo}`);
        if (!response.ok) throw new Error(`Failed to fetch anomaly timeline for ${serviceName}`);
        return response.json();
    }
};
