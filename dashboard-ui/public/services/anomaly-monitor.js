// ========== Anomaly Detection Functions ==========

let anomalyPollingInterval = null;
let lastAnomalyCheck = null;

// Load and display active anomalies
async function loadActiveAnomalies() {
    try {
        const anomalies = await API.getActiveAnomalies();
        const stats = await API.getAnomalyStats();

        renderAnomalies(anomalies, stats);
        lastAnomalyCheck = new Date();
        updateLastCheckTime();
    } catch (error) {
        console.error('Error loading anomalies:', error);
    }
}

// Render anomalies on the dashboard
function renderAnomalies(anomalies, stats) {
    const container = document.getElementById('anomaly-alerts-container');
    const anomaliesDiv = document.getElementById('active-anomalies');

    if (!anomalies || anomalies.length === 0) {
        container.style.display = 'none';
        return;
    }

    container.style.display = 'block';

    // Update subtitle with count
    const subtitle = document.getElementById('anomaly-subtitle');
    if (subtitle) {
        subtitle.textContent = `${anomalies.length} active anomaly alert${anomalies.length !== 1 ? 's' : ''} detected`;
    }

    // Render stats summary
    let html = renderAnomalyStats(stats);

    // Render anomaly cards
    html += '<div class="anomalies-grid">';
    anomalies.forEach(anomaly => {
        html += renderAnomalyCard(anomaly);
    });
    html += '</div>';

    anomaliesDiv.innerHTML = html;
}

// Render anomaly statistics
function renderAnomalyStats(stats) {
    if (!stats || !stats.bySeverity) return '';

    return `
        <div class="anomaly-stats">
            <div class="anomaly-stat stat-critical">
                <div class="anomaly-stat-value">${stats.bySeverity.CRITICAL || 0}</div>
                <div class="anomaly-stat-label">Critical</div>
            </div>
            <div class="anomaly-stat stat-high">
                <div class="anomaly-stat-value">${stats.bySeverity.HIGH || 0}</div>
                <div class="anomaly-stat-label">High</div>
            </div>
            <div class="anomaly-stat stat-medium">
                <div class="anomaly-stat-value">${stats.bySeverity.MEDIUM || 0}</div>
                <div class="anomaly-stat-label">Medium</div>
            </div>
            <div class="anomaly-stat stat-low">
                <div class="anomaly-stat-value">${stats.bySeverity.LOW || 0}</div>
                <div class="anomaly-stat-label">Low</div>
            </div>
        </div>
    `;
}

// Render individual anomaly card
function renderAnomalyCard(anomaly) {
    const severityClass = `anomaly-${anomaly.severity.toLowerCase()}`;
    const icon = getAnomalyIcon(anomaly.metricType);
    const timeAgo = getTimeAgo(new Date(anomaly.detectedAt));

    return `
        <div class="anomaly-card ${severityClass}">
            <div class="anomaly-content">
                <div class="anomaly-header">
                    <span class="material-symbols-outlined anomaly-icon">${icon}</span>
                    <div>
                        <h4 class="anomaly-title">
                            ${formatAnomalyType(anomaly.anomalyType)}
                            <span class="anomaly-service">${anomaly.serviceName}</span>
                        </h4>
                        <span class="anomaly-badge badge-${anomaly.severity.toLowerCase()}">
                            ${anomaly.severity}
                        </span>
                    </div>
                </div>

                <p class="anomaly-description">${anomaly.description}</p>

                <div class="anomaly-details">
                    <div class="anomaly-detail">
                        <span class="anomaly-detail-label">Actual Value</span>
                        <span class="anomaly-detail-value">${formatValue(anomaly.actualValue, anomaly.metricType)}</span>
                    </div>
                    <div class="anomaly-detail">
                        <span class="anomaly-detail-label">Expected Value</span>
                        <span class="anomaly-detail-value">${formatValue(anomaly.expectedValue, anomaly.metricType)}</span>
                    </div>
                    <div class="anomaly-detail">
                        <span class="anomaly-detail-label">Z-Score</span>
                        <span class="anomaly-detail-value">${anomaly.zScore.toFixed(2)}σ</span>
                    </div>
                    <div class="anomaly-detail">
                        <span class="anomaly-detail-label">Metric</span>
                        <span class="anomaly-detail-value">${formatMetricType(anomaly.metricType)}</span>
                    </div>
                </div>

                <div class="anomaly-meta">
                    <span class="anomaly-time">
                        <span class="material-symbols-outlined" style="font-size: 14px;">schedule</span>
                        ${timeAgo}
                    </span>
                </div>
            </div>

            <div class="anomaly-actions">
                <button class="anomaly-resolve-btn" onclick="resolveAnomaly(${anomaly.id})">
                    ✓ Resolve
                </button>
            </div>
        </div>
    `;
}

// Get icon for metric type
function getAnomalyIcon(metricType) {
    const icons = {
        'CPU_USAGE': 'speed',
        'MEMORY_USAGE': 'memory',
        'CONNECTION_POOL': 'database',
        'RESPONSE_TIME': 'schedule'
    };
    return icons[metricType] || 'warning';
}

// Format anomaly type for display
function formatAnomalyType(type) {
    const formatted = type.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
}

// Format metric type for display
function formatMetricType(type) {
    const formatted = type.replace(/_/g, ' ').toLowerCase();
    return formatted.split(' ').map(word =>
        word.charAt(0).toUpperCase() + word.slice(1)
    ).join(' ');
}

// Format value based on metric type
function formatValue(value, metricType) {
    if (value === null || value === undefined) return 'N/A';

    switch(metricType) {
        case 'CPU_USAGE':
        case 'MEMORY_USAGE':
        case 'CONNECTION_POOL':
            return `${value.toFixed(1)}%`;
        case 'RESPONSE_TIME':
            return `${value.toFixed(0)}ms`;
        default:
            return value.toFixed(2);
    }
}

// Get time ago string
function getTimeAgo(date) {
    const seconds = Math.floor((new Date() - date) / 1000);

    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
}

// Resolve an anomaly
async function resolveAnomaly(anomalyId) {
    try {
        await API.resolveAnomaly(anomalyId);
        showToast('Anomaly resolved successfully', 'success');

        // Refresh anomalies
        await loadActiveAnomalies();
    } catch (error) {
        console.error('Error resolving anomaly:', error);
        showToast('Failed to resolve anomaly', 'error');
    }
}

// Refresh anomalies manually
async function refreshAnomalies() {
    showToast('Refreshing anomalies...', 'info');
    await loadActiveAnomalies();
    showToast('Anomalies refreshed', 'success');
}

// Update last check time display
function updateLastCheckTime() {
    const timeElement = document.getElementById('last-check-time');
    if (timeElement && lastAnomalyCheck) {
        const timeAgo = getTimeAgo(lastAnomalyCheck);
        timeElement.textContent = `Last checked ${timeAgo}`;
    }
}

// Start polling for anomalies
function startAnomalyPolling() {
    // Initial load
    loadActiveAnomalies();

    // Poll every 10 seconds
    if (anomalyPollingInterval) {
        clearInterval(anomalyPollingInterval);
    }

    anomalyPollingInterval = setInterval(() => {
        loadActiveAnomalies();
    }, 10000);

    // Update time display every second
    setInterval(updateLastCheckTime, 1000);
}

// Stop polling
function stopAnomalyPolling() {
    if (anomalyPollingInterval) {
        clearInterval(anomalyPollingInterval);
        anomalyPollingInterval = null;
    }
}

// Initialize anomaly monitoring when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', startAnomalyPolling);
} else {
    startAnomalyPolling();
}

// Toast notification helper
function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span class="material-symbols-outlined">${getToastIcon(type)}</span>
        <span>${message}</span>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function getToastIcon(type) {
    const icons = {
        'success': 'check_circle',
        'error': 'error',
        'warning': 'warning',
        'info': 'info'
    };
    return icons[type] || 'info';
}
