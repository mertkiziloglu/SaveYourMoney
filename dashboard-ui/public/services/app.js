// Main Application Logic - SaveYourMoney Dashboard

// State
let dashboardData = null;
let serviceRecommendations = {
    'cpu-hungry-service': null,
    'memory-leaker-service': null,
    'db-connection-service': null
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    console.log('SaveYourMoney Dashboard initialized');
    loadDashboard();

    // Auto-refresh every 30 seconds
    setInterval(() => {
        refreshData();
    }, 30000);
});

// Load dashboard data
async function loadDashboard() {
    try {
        showLoading();
        dashboardData = await API.getDashboard();
        updateSummaryCards(dashboardData);

        // Load individual service data
        await loadServiceData('cpu-hungry-service');
        await loadServiceData('memory-leaker-service');
        await loadServiceData('db-connection-service');

        hideLoading();
        showToast('Dashboard loaded successfully', 'success');
    } catch (error) {
        console.error('Error loading dashboard:', error);
        hideLoading();
        showToast('Failed to load dashboard. Make sure services are running.', 'error');
    }
}

// Update summary cards
function updateSummaryCards(data) {
    document.getElementById('services-analyzed').textContent = data.servicesAnalyzed || 3;
    document.getElementById('monthly-savings').textContent =
        `$${Math.round(data.totalMonthlySavings || 0)}`;
    document.getElementById('annual-savings').textContent =
        `$${Math.round(data.totalAnnualSavings || 0)}`;

    // Count total issues
    let totalIssues = 0;
    ['cpuHungryService', 'memoryLeakerService', 'dbConnectionService'].forEach(key => {
        if (data[key]) {
            if (data[key].cpuThrottlingDetected) totalIssues++;
            if (data[key].memoryLeakDetected) totalIssues++;
            if (data[key].connectionPoolExhaustion) totalIssues++;
        }
    });
    document.getElementById('issues-detected').textContent = totalIssues;
}

// Load service data
async function loadServiceData(serviceName) {
    try {
        const analysis = await API.getLatestAnalysis(serviceName);

        if (analysis) {
            updateServiceCard(serviceName, analysis);
        } else {
            setServiceStatus(serviceName, 'No analysis data yet');
        }
    } catch (error) {
        console.error(`Error loading ${serviceName}:`, error);
        setServiceStatus(serviceName, 'Error loading data');
    }
}

// Update service card
function updateServiceCard(serviceName, analysis) {
    const prefix = serviceName.split('-')[0]; // cpu, memory, or db

    // Update status
    const hasIssues = analysis.cpuThrottlingDetected ||
                     analysis.memoryLeakDetected ||
                     analysis.connectionPoolExhaustion;

    const statusElement = document.getElementById(`${prefix}-status`);
    if (statusElement) {
        statusElement.textContent = hasIssues ? 'Issues Detected' : 'Optimized';
        statusElement.className = `service-status ${hasIssues ? 'issues' : 'optimized'}`;
    }

    // Update metrics
    if (prefix === 'cpu') {
        document.getElementById('cpu-p95').textContent =
            `${analysis.p95CpuUsage?.toFixed(1) || '-'}%`;
        document.getElementById('cpu-response').textContent =
            `${analysis.p95ResponseTime?.toFixed(0) || '-'}ms`;
        document.getElementById('cpu-confidence').textContent =
            `${(analysis.confidenceScore * 100)?.toFixed(0) || '-'}%`;
    } else if (prefix === 'memory') {
        document.getElementById('memory-p95').textContent =
            `${analysis.p95MemoryUsage?.toFixed(1) || '-'}%`;
        document.getElementById('memory-heap').textContent =
            `${(analysis.maxMemoryUsage)?.toFixed(1) || '-'}%`;
        document.getElementById('memory-confidence').textContent =
            `${(analysis.confidenceScore * 100)?.toFixed(0) || '-'}%`;
    } else if (prefix === 'db') {
        document.getElementById('db-pool').textContent = '100%'; // Mock
        document.getElementById('db-response').textContent =
            `${analysis.p95ResponseTime?.toFixed(0) || '-'}ms`;
        document.getElementById('db-confidence').textContent =
            `${(analysis.confidenceScore * 100)?.toFixed(0) || '-'}%`;
    }

    // Update issues
    updateIssues(prefix, analysis);

    // Update changes table
    updateChangesTable(prefix, analysis);

    // Update chart
    if (prefix === 'cpu') {
        Charts.createCPUChart(analysis);
    } else if (prefix === 'memory') {
        Charts.createMemoryChart(analysis);
    } else if (prefix === 'db') {
        Charts.createDBChart(analysis);
    }
}

// Update issues section
function updateIssues(prefix, analysis) {
    const issuesContainer = document.getElementById(`${prefix}-issues`);
    if (!issuesContainer) return;

    issuesContainer.innerHTML = '';

    const issues = [];
    if (analysis.cpuThrottlingDetected) {
        issues.push('CPU Throttling Detected');
    }
    if (analysis.memoryLeakDetected) {
        issues.push('Memory Leak Detected');
    }
    if (analysis.connectionPoolExhaustion) {
        issues.push('Connection Pool Exhaustion');
    }

    if (issues.length > 0) {
        const issuesHtml = issues.map(issue =>
            `<span class="issue-badge">⚠️ ${issue}</span>`
        ).join('');
        issuesContainer.innerHTML = `<div style="margin-bottom: 15px;">${issuesHtml}</div>`;
    }
}

// Update changes table
function updateChangesTable(prefix, analysis) {
    const tableElement = document.getElementById(`${prefix}-changes`);
    if (!tableElement) return;

    let rows = '';

    // CPU/Memory changes
    if (prefix === 'cpu' || prefix === 'memory') {
        rows += `
            <tr>
                <th>Resource</th>
                <th>Current</th>
                <th>Recommended</th>
                <th>Change</th>
            </tr>
            <tr>
                <td>CPU Request</td>
                <td class="change-before">${analysis.currentCpuRequest || '100m'}</td>
                <td class="change-after">${analysis.recommendedCpuRequest || '500m'}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
            <tr>
                <td>CPU Limit</td>
                <td class="change-before">${analysis.currentCpuLimit || '200m'}</td>
                <td class="change-after">${analysis.recommendedCpuLimit || '1000m'}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
            <tr>
                <td>Memory Request</td>
                <td class="change-before">${analysis.currentMemoryRequest || '256Mi'}</td>
                <td class="change-after">${analysis.recommendedMemoryRequest || '512Mi'}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
            <tr>
                <td>Memory Limit</td>
                <td class="change-before">${analysis.currentMemoryLimit || '512Mi'}</td>
                <td class="change-after">${analysis.recommendedMemoryLimit || '1Gi'}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
        `;
    }

    // JVM changes
    if (analysis.recommendedJvmXms) {
        rows += `
            <tr>
                <td>JVM Xms</td>
                <td class="change-before">256m</td>
                <td class="change-after">${analysis.recommendedJvmXms}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
            <tr>
                <td>JVM Xmx</td>
                <td class="change-before">256m</td>
                <td class="change-after">${analysis.recommendedJvmXmx}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
        `;
    }

    // Connection pool changes
    if (prefix === 'db' && analysis.recommendedMaxPoolSize) {
        rows += `
            <tr>
                <td>Max Pool Size</td>
                <td class="change-before">5</td>
                <td class="change-after">${analysis.recommendedMaxPoolSize}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
            <tr>
                <td>Min Idle</td>
                <td class="change-before">2</td>
                <td class="change-after">${analysis.recommendedMinIdle || 10}</td>
                <td><span class="change-badge">✅ Optimized</span></td>
            </tr>
        `;
    }

    tableElement.innerHTML = rows;
}

// Set service status
function setServiceStatus(serviceName, status) {
    const prefix = serviceName.split('-')[0];
    const statusElement = document.getElementById(`${prefix}-status`);
    if (statusElement) {
        statusElement.textContent = status;
        statusElement.className = 'service-status analyzing';
    }
}

// Refresh all data
async function refreshData() {
    const refreshIcon = document.getElementById('refresh-icon');
    refreshIcon.style.animation = 'spin 1s linear infinite';

    await loadDashboard();

    refreshIcon.style.animation = '';
}

// Analyze single service
async function analyzeService(serviceName) {
    try {
        showToast(`Analyzing ${serviceName}...`, 'info');
        setServiceStatus(serviceName, 'Analyzing...');

        const recommendation = await API.analyzeService(serviceName);
        serviceRecommendations[serviceName] = recommendation;

        // Reload service data
        await loadServiceData(serviceName);

        showToast(`Analysis complete for ${serviceName}`, 'success');
    } catch (error) {
        console.error(`Error analyzing ${serviceName}:`, error);
        showToast(`Failed to analyze ${serviceName}`, 'error');
    }
}

// Analyze all services
async function analyzeAll() {
    try {
        showToast('Analyzing all services...', 'info');

        const recommendations = await API.analyzeAll();

        Object.keys(recommendations).forEach(serviceName => {
            serviceRecommendations[serviceName] = recommendations[serviceName];
        });

        // Reload dashboard
        await loadDashboard();

        showToast('All services analyzed successfully!', 'success');
    } catch (error) {
        console.error('Error analyzing all services:', error);
        showToast('Failed to analyze all services', 'error');
    }
}

// Generate code for a service
async function generateCode(serviceName) {
    try {
        const recommendation = serviceRecommendations[serviceName];

        if (!recommendation) {
            showToast('Please analyze the service first', 'error');
            return;
        }

        showToast(`Generating code for ${serviceName}...`, 'info');

        const artifacts = await API.generateCode(serviceName, recommendation);

        if (artifacts.success) {
            showToast(`Code generated! ${artifacts.files.length} files created.`, 'success');
            console.log('Generated files:', artifacts.files);
        } else {
            showToast('Code generation failed', 'error');
        }
    } catch (error) {
        console.error('Error generating code:', error);
        showToast('Failed to generate code', 'error');
    }
}

// Create Pull Request
async function createPR(serviceName) {
    try {
        const recommendation = serviceRecommendations[serviceName];

        if (!recommendation) {
            showToast('Please analyze the service first', 'error');
            return;
        }

        showToast(`Creating Pull Request for ${serviceName}...`, 'info');

        const result = await API.createPR(serviceName, recommendation);

        if (result.success) {
            showToast('Pull Request created successfully!', 'success');

            if (result.pullRequestUrl) {
                setTimeout(() => {
                    window.open(result.pullRequestUrl, '_blank');
                }, 1000);
            }
        } else {
            showToast('PR creation failed', 'error');
        }
    } catch (error) {
        console.error('Error creating PR:', error);
        showToast('Failed to create Pull Request', 'error');
    }
}

// Show toast notification
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Show loading indicator
function showLoading() {
    // Could add a global loading overlay here
    console.log('Loading...');
}

// Hide loading indicator
function hideLoading() {
    console.log('Loading complete');
}
