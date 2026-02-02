// SaveYourMoney - Dashboard Application Logic with Firestore Integration

// State
let dashboardData = {
    metrics: null,
    recommendations: [],
    insights: [],
    deployments: []
};

let currentView = 'overview'; // 'overview' or 'detail'
let unsubscribeListeners = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', async () => {
    console.log('SaveYourMoney Dashboard initialized');

    // Initialize Firebase
    const initialized = initializeFirebase();

    if (initialized) {
        await loadDashboardData();
        setupRealtimeListeners();
    } else {
        renderFirebaseError();
    }
});

// Load all dashboard data
async function loadDashboardData() {
    try {
        showLoading();

        // Load all data in parallel
        const [metrics, recommendations, insights, deployments] = await Promise.all([
            FirestoreAPI.getMetrics(),
            FirestoreAPI.getRecommendations(12),
            FirestoreAPI.getInsights(),
            FirestoreAPI.getDeployments()
        ]);

        dashboardData.metrics = metrics;
        dashboardData.recommendations = recommendations;
        dashboardData.insights = insights;
        dashboardData.deployments = deployments;

        renderDashboard();
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        renderErrorState();
    }
}

// Setup real-time listeners
function setupRealtimeListeners() {
    // Listen to metrics changes
    const metricsUnsubscribe = FirestoreAPI.onMetricsChange((metrics) => {
        dashboardData.metrics = metrics;
        updateMetricsUI(metrics);
    });

    // Listen to recommendations changes
    const recommendationsUnsubscribe = FirestoreAPI.onRecommendationsChange((recommendations) => {
        dashboardData.recommendations = recommendations;
        updateRecommendationsUI(recommendations);
    });

    unsubscribeListeners.push(metricsUnsubscribe, recommendationsUnsubscribe);
}

// Render main dashboard
function renderDashboard() {
    const content = document.getElementById('main-content');
    const { metrics, recommendations, insights } = dashboardData;

    content.innerHTML = `
        <!-- Hero Section -->
        <div class="flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6">
            <div>
                <h1 class="text-2xl md:text-3xl font-bold text-slate-900 tracking-tight">Resource Overview</h1>
                <p class="text-slate-500 mt-1">Optimization opportunities for your clusters.</p>
            </div>
            <div class="flex items-center gap-3">
                <span class="text-sm text-slate-500">Last updated: Just now</span>
                <button onclick="refreshDashboard()" class="flex items-center gap-2 px-4 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-lg text-sm font-bold transition-colors shadow-sm">
                    <span class="material-symbols-outlined text-[18px]">refresh</span>
                    Refresh
                </button>
            </div>
        </div>

        <!-- Metrics Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            ${renderMetricsCards(metrics)}
        </div>

        <!-- Main Grid -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- Optimization Opportunities Table -->
            <div class="lg:col-span-2 bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex flex-col">
                ${renderRecommendationsTable(recommendations)}
            </div>

            <!-- AI Insights Panel -->
            <div class="bg-white rounded-xl border border-slate-200 shadow-sm p-6 flex flex-col gap-5">
                ${renderInsightsPanel(insights)}
            </div>
        </div>
    `;

    hideLoading();
}

// Render metrics cards
function renderMetricsCards(metrics) {
    if (!metrics) return '';

    return `
        <div class="flex flex-col gap-3 rounded-xl p-5 bg-white border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-between">
                <p class="text-slate-500 text-sm font-medium">Total Annual Savings</p>
                <div class="size-8 rounded-full bg-green-50 flex items-center justify-center text-green-600">
                    <span class="material-symbols-outlined text-[20px]">savings</span>
                </div>
            </div>
            <div class="flex items-baseline gap-2">
                <p class="text-slate-900 text-3xl font-bold tracking-tight">$${metrics.totalAnnualSavings.toLocaleString()}</p>
                <span class="text-green-600 text-sm font-bold bg-green-50 px-1.5 py-0.5 rounded flex items-center gap-0.5">
                    <span class="material-symbols-outlined text-[14px]">trending_up</span> ${metrics.savingsPercentage}%
                </span>
            </div>
        </div>

        <div class="flex flex-col gap-3 rounded-xl p-5 bg-white border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-between">
                <p class="text-slate-500 text-sm font-medium">Idle CPU Cores</p>
                <div class="size-8 rounded-full bg-red-50 flex items-center justify-center text-red-500">
                    <span class="material-symbols-outlined text-[20px]">memory</span>
                </div>
            </div>
            <div class="flex items-baseline gap-2">
                <p class="text-slate-900 text-3xl font-bold tracking-tight">${metrics.idleCpuCores}</p>
                <span class="text-red-500 text-sm font-medium">Cores wasted</span>
            </div>
        </div>

        <div class="flex flex-col gap-3 rounded-xl p-5 bg-white border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
            <div class="flex items-center justify-between">
                <p class="text-slate-500 text-sm font-medium">Wasted RAM (GB)</p>
                <div class="size-8 rounded-full bg-orange-50 flex items-center justify-center text-orange-500">
                    <span class="material-symbols-outlined text-[20px]">storage</span>
                </div>
            </div>
            <div class="flex items-baseline gap-2">
                <p class="text-slate-900 text-3xl font-bold tracking-tight">${metrics.wastedRamGB}</p>
                <span class="text-orange-500 text-sm font-medium">GB Unused</span>
            </div>
        </div>

        <div class="flex flex-col gap-3 rounded-xl p-5 bg-white border border-slate-200 shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
            <div class="absolute right-0 top-0 w-24 h-24 bg-primary/5 rounded-bl-full -mr-4 -mt-4 transition-transform group-hover:scale-110"></div>
            <div class="flex items-center justify-between relative z-10">
                <p class="text-slate-500 text-sm font-medium">AI Recommendations</p>
                <div class="size-8 rounded-full bg-blue-50 flex items-center justify-center text-primary">
                    <span class="material-symbols-outlined text-[20px]">auto_awesome</span>
                </div>
            </div>
            <div class="flex items-baseline gap-2 relative z-10">
                <p class="text-slate-900 text-3xl font-bold tracking-tight">${metrics.aiRecommendationsCount}</p>
                <span class="text-primary text-sm font-medium">Actionable items</span>
            </div>
        </div>
    `;
}

// Render recommendations table
function renderRecommendationsTable(recommendations) {
    const displayedRecs = recommendations.slice(0, 3); // Show first 3

    return `
        <div class="p-5 border-b border-slate-100 flex justify-between items-center bg-white">
            <h3 class="text-slate-900 font-bold text-lg flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-[24px]">tune</span>
                Optimization Opportunities
            </h3>
            <div class="flex gap-2">
                <button class="text-xs font-bold text-slate-600 px-3 py-1.5 bg-slate-50 border border-slate-200 rounded hover:bg-slate-100">Filter</button>
                <button class="text-xs font-bold text-slate-600 px-3 py-1.5 bg-slate-50 border border-slate-200 rounded hover:bg-slate-100">Export</button>
            </div>
        </div>
        <div class="overflow-x-auto flex-1">
            <table class="w-full text-left border-collapse">
                <thead class="bg-slate-50 text-slate-500 text-xs font-bold uppercase tracking-wider border-b border-slate-100">
                    <tr>
                        <th class="px-6 py-4">Namespace / Deployment</th>
                        <th class="px-6 py-4">Current Config</th>
                        <th class="px-6 py-4">AI Recommended</th>
                        <th class="px-6 py-4 text-right">Potential Savings</th>
                        <th class="px-6 py-4 text-center">Action</th>
                    </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                    ${displayedRecs.map(rec => renderRecommendationRow(rec)).join('')}
                </tbody>
            </table>
        </div>
        <div class="p-4 bg-slate-50 border-t border-slate-200 text-center">
            <button onclick="showAllRecommendations()" class="text-sm font-bold text-primary hover:underline">
                View all ${recommendations.length} recommendations
            </button>
        </div>
    `;
}

// Render single recommendation row
function renderRecommendationRow(rec) {
    return `
        <tr class="hover:bg-slate-50 transition-colors group">
            <td class="px-6 py-4">
                <div class="flex flex-col">
                    <span class="text-slate-900 font-bold text-sm">${rec.deploymentName}</span>
                    <span class="text-slate-500 text-xs mt-0.5">${rec.namespace}</span>
                </div>
            </td>
            <td class="px-6 py-4">
                <div class="text-slate-600 text-xs font-mono bg-slate-100 px-2 py-1 rounded inline-block border border-slate-200">
                    ${rec.currentConfig.cpu} CPU / ${rec.currentConfig.memory}GB
                </div>
            </td>
            <td class="px-6 py-4">
                <div class="flex items-center gap-2">
                    <span class="material-symbols-outlined text-primary text-[16px]">arrow_right_alt</span>
                    <div class="text-primary text-xs font-mono font-bold bg-blue-50 px-2 py-1 rounded border border-blue-100">
                        ${rec.recommendedConfig.cpu} CPU / ${rec.recommendedConfig.memory}GB
                    </div>
                </div>
            </td>
            <td class="px-6 py-4 text-right">
                <span class="text-green-600 font-bold text-sm">+$${rec.potentialSavingsMonthly}/mo</span>
            </td>
            <td class="px-6 py-4 text-center">
                <button onclick="viewRecommendationDetail('${rec.id}')" class="text-primary hover:text-blue-700 hover:bg-blue-50 text-xs font-bold py-2 px-3 rounded-lg border border-transparent hover:border-blue-100 transition-all whitespace-nowrap flex items-center justify-center gap-1 mx-auto">
                    View Detail
                    <span class="material-symbols-outlined text-[16px]">visibility</span>
                </button>
            </td>
        </tr>
    `;
}

// Render insights panel
function renderInsightsPanel(insights) {
    return `
        <div class="flex items-center gap-3 pb-2 border-b border-slate-100">
            <div class="p-1.5 bg-indigo-50 text-indigo-600 rounded-lg">
                <span class="material-symbols-outlined text-[24px]">psychology</span>
            </div>
            <h3 class="text-lg font-bold text-slate-900">AI Insights</h3>
        </div>
        <div class="flex flex-col gap-3">
            ${insights.map(insight => renderInsight(insight)).join('')}
        </div>
        <div class="mt-auto pt-4">
            <button class="w-full py-2.5 rounded-lg bg-slate-900 text-white hover:bg-slate-800 text-sm font-bold transition-all shadow-sm">
                Run Deep Analysis
            </button>
        </div>
    `;
}

// Render single insight
function renderInsight(insight) {
    const colorMap = {
        high: { bg: 'bg-red-50', border: 'border-red-100', icon: 'text-red-500', text: 'text-red-700', desc: 'text-red-600/80', highlight: 'text-red-800' },
        medium: { bg: 'bg-orange-50', border: 'border-orange-100', icon: 'text-orange-500', text: 'text-orange-700', desc: 'text-orange-600/80', highlight: 'text-orange-800' },
        low: { bg: 'bg-blue-50', border: 'border-blue-100', icon: 'text-blue-500', text: 'text-blue-700', desc: 'text-blue-600/80', highlight: 'text-blue-800' }
    };

    const iconMap = {
        cpu_throttling: 'speed',
        memory_leak: 'water_drop',
        cost_optimization: 'savings'
    };

    const colors = colorMap[insight.severity] || colorMap.low;
    const icon = iconMap[insight.type] || 'info';

    return `
        <div class="${colors.bg} border ${colors.border} rounded-lg p-4 relative overflow-hidden">
            <div class="flex items-start gap-3">
                <span class="material-symbols-outlined ${colors.icon} mt-0.5 text-[20px]">${icon}</span>
                <div>
                    <p class="font-bold text-sm ${colors.text} mb-1">${insight.title}</p>
                    <p class="text-xs ${colors.desc} leading-relaxed">
                        ${insight.description}
                    </p>
                </div>
            </div>
        </div>
    `;
}

// Update metrics UI (real-time)
function updateMetricsUI(metrics) {
    if (currentView !== 'overview') return;
    renderDashboard();
}

// Update recommendations UI (real-time)
function updateRecommendationsUI(recommendations) {
    if (currentView !== 'overview') return;
    dashboardData.recommendations = recommendations;
    renderDashboard();
}

// Refresh dashboard
async function refreshDashboard() {
    showToast('Refreshing dashboard...', 'info');
    await loadDashboardData();
    showToast('Dashboard refreshed successfully', 'success');
}

// View recommendation detail (placeholder)
function viewRecommendationDetail(id) {
    showToast(`Opening details for ${id}...`, 'info');
    // TODO: Implement detail view
}

// Show all recommendations (placeholder)
function showAllRecommendations() {
    showToast('Showing all recommendations...', 'info');
    // TODO: Implement full list view
}

// Loading state
function showLoading() {
    const content = document.getElementById('main-content');
    content.innerHTML = `
        <div class="flex flex-col items-center justify-center h-96 gap-4">
            <div class="animate-spin rounded-full h-16 w-16 border-b-2 border-primary"></div>
            <p class="text-slate-500 font-medium">Loading dashboard data...</p>
        </div>
    `;
}

function hideLoading() {
    // Loading is hidden when content is rendered
}

// Error states
function renderErrorState() {
    const content = document.getElementById('main-content');
    content.innerHTML = `
        <div class="flex flex-col items-center justify-center h-96 gap-4">
            <span class="material-symbols-outlined text-6xl text-slate-300">error_outline</span>
            <h2 class="text-2xl font-bold text-slate-900">Failed to Load Dashboard</h2>
            <p class="text-slate-500">Could not connect to Firestore database</p>
            <button onclick="location.reload()" class="h-11 px-6 rounded-lg bg-primary hover:bg-primary/90 text-white font-bold">
                Retry
            </button>
        </div>
    `;
}

function renderFirebaseError() {
    const content = document.getElementById('main-content');
    content.innerHTML = `
        <div class="flex flex-col items-center justify-center h-96 gap-4 max-w-2xl mx-auto text-center">
            <span class="material-symbols-outlined text-6xl text-red-300">cloud_off</span>
            <h2 class="text-2xl font-bold text-slate-900">Firebase Not Configured</h2>
            <p class="text-slate-500">Please configure your Firebase project:</p>
            <ol class="text-left text-sm text-slate-600 space-y-2 bg-slate-50 p-4 rounded-lg border border-slate-200">
                <li>1. Create a Firebase project at <a href="https://console.firebase.google.com" class="text-primary font-mono" target="_blank">console.firebase.google.com</a></li>
                <li>2. Update <code class="bg-white px-2 py-0.5 rounded font-mono text-xs">firebase-config.js</code> with your config</li>
                <li>3. Run <code class="bg-white px-2 py-0.5 rounded font-mono text-xs">npm run insert-mock</code> to add sample data</li>
                <li>4. Reload this page</li>
            </ol>
            <a href="../firebase-setup/README.md" class="text-primary hover:underline font-medium">View Setup Guide →</a>
        </div>
    `;
}

// Toast notification
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    const colors = {
        success: 'bg-green-500',
        error: 'bg-red-500',
        info: 'bg-blue-500',
        warning: 'bg-amber-500'
    };

    const icons = {
        success: 'check_circle',
        error: 'error',
        info: 'info',
        warning: 'warning'
    };

    toast.className = `${colors[type] || colors.info} text-white px-4 py-3 rounded-lg shadow-lg flex items-center gap-3 transform transition-all duration-300 translate-x-full opacity-0`;
    toast.innerHTML = `
        <span class="material-symbols-outlined text-xl">${icons[type] || icons.info}</span>
        <span class="font-medium">${message}</span>
    `;

    container.appendChild(toast);
    setTimeout(() => toast.classList.remove('translate-x-full', 'opacity-0'), 10);
    setTimeout(() => {
        toast.classList.add('translate-x-full', 'opacity-0');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    unsubscribeListeners.forEach(unsubscribe => unsubscribe());
});
