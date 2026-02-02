// SaveYourMoney - Modern Dashboard Application Logic

// State
let dashboardData = null;
let currentService = null;
let serviceRecommendations = {
    'cpu-hungry-service': null,
    'memory-leaker-service': null,
    'db-connection-service': null
};

// Service metadata
const serviceMetadata = {
    'cpu-hungry-service': {
        displayName: 'CPU-Hungry-Service',
        icon: '🔥',
        description: 'High CPU utilization service'
    },
    'memory-leaker-service': {
        displayName: 'Memory-Leaker-Service',
        icon: '💾',
        description: 'Memory leak simulation service'
    },
    'db-connection-service': {
        displayName: 'DB-Connection-Service',
        icon: '🗄️',
        description: 'Database connection pool service'
    }
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    console.log('SaveYourMoney Dashboard initialized');
});

// Load dashboard overview
async function loadDashboard() {
    try {
        dashboardData = await API.getDashboard();
        renderServicesOverview();
    } catch (error) {
        console.error('Error loading dashboard:', error);
        renderErrorState();
    }
}

// Render services overview (list of services)
function renderServicesOverview() {
    const content = document.getElementById('main-content');

    content.innerHTML = `
        <!-- Hero Section -->
        <div class="flex flex-col lg:flex-row gap-6 justify-between items-start lg:items-center">
            <div class="flex flex-col gap-2">
                <h1 class="text-3xl font-black text-slate-900 dark:text-white tracking-tight">Resource Optimization Dashboard</h1>
                <p class="text-slate-500 dark:text-[#92adc9]">
                    AI-powered analysis for ${Object.keys(serviceRecommendations).length} services
                </p>
            </div>
            <div class="flex flex-wrap items-center gap-3">
                <div class="flex flex-col items-end mr-4">
                    <span class="text-xs font-medium uppercase tracking-wider text-slate-500">Total Savings</span>
                    <span class="text-2xl font-bold text-primary dark:text-primary">$${Math.round(dashboardData?.totalMonthlySavings || 0)}<span class="text-sm font-normal text-slate-400">/mo</span></span>
                </div>
                <button onclick="analyzeAll()" class="h-11 px-6 rounded-lg bg-primary hover:bg-primary/90 text-white font-bold shadow-lg shadow-blue-500/20 flex items-center gap-2 transition-all transform active:scale-95">
                    <span class="material-symbols-outlined text-xl">auto_awesome</span>
                    Analyze All Services
                </button>
            </div>
        </div>

        <!-- Services Grid -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            ${Object.keys(serviceRecommendations).map(serviceName => renderServiceCard(serviceName)).join('')}
        </div>
    `;
}

// Render individual service card for overview
function renderServiceCard(serviceName) {
    const meta = serviceMetadata[serviceName];
    const data = dashboardData?.[serviceName.replace(/-/g, '')] || {};

    const hasIssues = data.cpuThrottlingDetected || data.memoryLeakDetected || data.connectionPoolExhaustion;
    const statusColor = hasIssues ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400';
    const statusText = hasIssues ? 'Issues Detected' : 'Healthy';

    return `
        <div class="bg-surface-light dark:bg-[#1d2832] rounded-xl border border-slate-200 dark:border-[#324d67] p-6 shadow-sm hover:shadow-lg transition-all cursor-pointer" onclick="loadServiceDetail('${serviceName}')">
            <div class="flex items-start justify-between mb-4">
                <div class="flex items-center gap-3">
                    <div class="text-3xl">${meta.icon}</div>
                    <div>
                        <h3 class="text-lg font-bold text-slate-900 dark:text-white">${meta.displayName}</h3>
                        <p class="text-sm text-slate-500 dark:text-slate-400">${meta.description}</p>
                    </div>
                </div>
                <span class="px-2.5 py-0.5 rounded-full text-xs font-bold ${statusColor} border">${statusText}</span>
            </div>

            <div class="grid grid-cols-2 gap-4 mb-4">
                <div class="bg-slate-50 dark:bg-[#111a22] rounded-lg p-3">
                    <div class="text-xs text-slate-500 dark:text-slate-400 mb-1">Monthly Savings</div>
                    <div class="text-xl font-bold text-primary">$${Math.round(data.monthlySavings || 0)}</div>
                </div>
                <div class="bg-slate-50 dark:bg-[#111a22] rounded-lg p-3">
                    <div class="text-xs text-slate-500 dark:text-slate-400 mb-1">Confidence</div>
                    <div class="text-xl font-bold text-slate-900 dark:text-white">${Math.round((data.confidenceScore || 0) * 100)}%</div>
                </div>
            </div>

            <button onclick="event.stopPropagation(); loadServiceDetail('${serviceName}')" class="w-full h-10 rounded-lg border border-primary text-primary hover:bg-primary hover:text-white font-bold transition-colors flex items-center justify-center gap-2">
                <span class="material-symbols-outlined text-lg">visibility</span>
                View Details
            </button>
        </div>
    `;
}

// Load service detail view
async function loadServiceDetail(serviceName) {
    currentService = serviceName;
    document.getElementById('breadcrumb-service').textContent = serviceMetadata[serviceName].displayName;

    try {
        const analysis = await API.getLatestAnalysis(serviceName);

        if (analysis) {
            serviceRecommendations[serviceName] = analysis;
            renderServiceDetail(serviceName, analysis);
        } else {
            showToast('No analysis data available. Running analysis...', 'info');
            await analyzeService(serviceName);
        }
    } catch (error) {
        console.error(`Error loading ${serviceName}:`, error);
        showToast('Failed to load service details', 'error');
    }
}

// Render service detail view
function renderServiceDetail(serviceName, analysis) {
    const meta = serviceMetadata[serviceName];
    const hasIssues = analysis.cpuThrottlingDetected || analysis.memoryLeakDetected || analysis.connectionPoolExhaustion;
    const monthlySavings = analysis.estimatedMonthlySavings || 250;

    const content = document.getElementById('main-content');
    content.innerHTML = `
        <!-- Hero Section -->
        <div class="flex flex-col lg:flex-row gap-6 justify-between items-start lg:items-center">
            <div class="flex flex-col gap-2">
                <div class="flex items-center gap-3">
                    <button onclick="loadDashboard()" class="size-10 flex items-center justify-center rounded-lg hover:bg-slate-100 dark:hover:bg-[#233648] transition-colors">
                        <span class="material-symbols-outlined">arrow_back</span>
                    </button>
                    <h1 class="text-3xl font-black text-slate-900 dark:text-white tracking-tight">${meta.displayName}</h1>
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-bold ${hasIssues ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'} border">
                        ${hasIssues ? 'Issues Detected' : 'Healthy'}
                    </span>
                </div>
                <p class="text-slate-500 dark:text-[#92adc9]">
                    Namespace: <span class="font-mono text-slate-700 dark:text-slate-300">default</span> •
                    Last analyzed: <span class="text-slate-700 dark:text-slate-300">Just now</span>
                </p>
            </div>
            <div class="flex flex-wrap items-center gap-3">
                <div class="flex flex-col items-end mr-4">
                    <span class="text-xs font-medium uppercase tracking-wider text-slate-500">Projected Savings</span>
                    <span class="text-2xl font-bold text-primary dark:text-primary">-$${monthlySavings.toFixed(2)}<span class="text-sm font-normal text-slate-400">/mo</span></span>
                </div>
                <button onclick="analyzeService('${serviceName}')" class="h-11 px-4 rounded-lg border border-slate-200 dark:border-[#324d67] text-slate-700 dark:text-slate-300 font-bold hover:bg-slate-50 dark:hover:bg-[#233648] transition-colors">
                    Re-analyze
                </button>
                <button onclick="createPR('${serviceName}')" class="h-11 px-6 rounded-lg bg-primary hover:bg-primary/90 text-white font-bold shadow-lg shadow-blue-500/20 flex items-center gap-2 transition-all transform active:scale-95">
                    <svg aria-hidden="true" class="size-5" fill="currentColor" viewBox="0 0 24 24"><path clip-rule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.606 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.334.202 2.353.1 2.606.636.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" fill-rule="evenodd"></path></svg>
                    Create Pull Request
                </button>
            </div>
        </div>

        <!-- Main Grid -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- Left Col: YAML Diff (Spans 2 cols) -->
            <div class="lg:col-span-2 flex flex-col gap-6">
                ${renderYAMLDiff(analysis)}
                ${renderPerformanceChart(analysis)}
            </div>

            <!-- Right Col: Resource Chart & AI Insights -->
            <div class="flex flex-col gap-6">
                ${renderResourceChart(analysis)}
                ${renderAIInsights(analysis)}
            </div>
        </div>
    `;
}

// Render YAML Diff Viewer
function renderYAMLDiff(analysis) {
    const current = {
        cpuRequest: analysis.currentCpuRequest || '500m',
        cpuLimit: analysis.currentCpuLimit || '1000m',
        memoryRequest: analysis.currentMemoryRequest || '1Gi',
        memoryLimit: analysis.currentMemoryLimit || '2Gi'
    };

    const recommended = {
        cpuRequest: analysis.recommendedCpuRequest || '250m',
        cpuLimit: analysis.recommendedCpuLimit || '500m',
        memoryRequest: analysis.recommendedMemoryRequest || '750Mi',
        memoryLimit: analysis.recommendedMemoryLimit || '1.5Gi'
    };

    return `
        <div class="bg-surface-light dark:bg-[#1d2832] rounded-xl border border-slate-200 dark:border-[#324d67] overflow-hidden shadow-sm">
            <div class="px-4 py-3 border-b border-slate-200 dark:border-[#324d67] bg-slate-50 dark:bg-[#111a22] flex items-center justify-between">
                <div class="flex items-center gap-2">
                    <span class="material-symbols-outlined text-slate-500 text-lg">code</span>
                    <span class="text-sm font-bold text-slate-700 dark:text-slate-200">deployment.yaml</span>
                    <span class="text-xs px-2 py-0.5 rounded bg-slate-200 dark:bg-[#324d67] text-slate-600 dark:text-slate-300 font-mono">diff</span>
                </div>
                <div class="flex items-center gap-4 text-xs font-medium">
                    <div class="flex items-center gap-1.5">
                        <span class="size-2 rounded-full bg-red-500"></span>
                        <span class="text-slate-500 dark:text-slate-400">Current</span>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <span class="size-2 rounded-full bg-green-500"></span>
                        <span class="text-slate-500 dark:text-slate-400">AI Recommended</span>
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-2 text-sm font-mono leading-relaxed bg-white dark:bg-[#0d1117] text-slate-800 dark:text-slate-300">
                <!-- Original Pane -->
                <div class="border-r border-slate-200 dark:border-[#30363d] overflow-x-auto code-scroll p-4">
                    <div class="opacity-50">apiVersion: apps/v1</div>
                    <div class="opacity-50">kind: Deployment</div>
                    <div class="opacity-50">metadata:</div>
                    <div class="opacity-50">  name: ${currentService}</div>
                    <div class="opacity-50">spec:</div>
                    <div class="opacity-50">  template:</div>
                    <div class="opacity-50">    spec:</div>
                    <div class="opacity-50">      containers:</div>
                    <div class="opacity-50">      - name: main</div>
                    <div>        resources:</div>
                    <div>          requests:</div>
                    <div class="bg-red-500/10 dark:bg-red-900/20 w-full block border-l-2 border-red-500 pl-1 text-red-700 dark:text-red-400 line-through decoration-red-500/50">            cpu: ${current.cpuRequest}</div>
                    <div class="bg-red-500/10 dark:bg-red-900/20 w-full block border-l-2 border-red-500 pl-1 text-red-700 dark:text-red-400 line-through decoration-red-500/50">            memory: ${current.memoryRequest}</div>
                    <div>          limits:</div>
                    <div class="bg-red-500/10 dark:bg-red-900/20 w-full block border-l-2 border-red-500 pl-1 text-red-700 dark:text-red-400 line-through decoration-red-500/50">            cpu: ${current.cpuLimit}</div>
                    <div class="bg-red-500/10 dark:bg-red-900/20 w-full block border-l-2 border-red-500 pl-1 text-red-700 dark:text-red-400 line-through decoration-red-500/50">            memory: ${current.memoryLimit}</div>
                </div>

                <!-- New Pane -->
                <div class="overflow-x-auto code-scroll p-4 bg-slate-50/50 dark:bg-[#0d1117]">
                    <div class="opacity-50">apiVersion: apps/v1</div>
                    <div class="opacity-50">kind: Deployment</div>
                    <div class="opacity-50">metadata:</div>
                    <div class="opacity-50">  name: ${currentService}</div>
                    <div class="opacity-50">spec:</div>
                    <div class="opacity-50">  template:</div>
                    <div class="opacity-50">    spec:</div>
                    <div class="opacity-50">      containers:</div>
                    <div class="opacity-50">      - name: main</div>
                    <div>        resources:</div>
                    <div>          requests:</div>
                    <div class="bg-green-500/10 dark:bg-green-900/20 w-full block border-l-2 border-green-500 pl-1 text-green-700 dark:text-green-400">            cpu: ${recommended.cpuRequest}</div>
                    <div class="bg-green-500/10 dark:bg-green-900/20 w-full block border-l-2 border-green-500 pl-1 text-green-700 dark:text-green-400">            memory: ${recommended.memoryRequest}</div>
                    <div>          limits:</div>
                    <div class="bg-green-500/10 dark:bg-green-900/20 w-full block border-l-2 border-green-500 pl-1 text-green-700 dark:text-green-400">            cpu: ${recommended.cpuLimit}</div>
                    <div class="bg-green-500/10 dark:bg-green-900/20 w-full block border-l-2 border-green-500 pl-1 text-green-700 dark:text-green-400">            memory: ${recommended.memoryLimit}</div>
                </div>
            </div>
        </div>
    `;
}

// Render Performance Impact Chart
function renderPerformanceChart(analysis) {
    return `
        <div class="bg-surface-light dark:bg-[#1d2832] rounded-xl border border-slate-200 dark:border-[#324d67] p-6 shadow-sm">
            <div class="flex justify-between items-center mb-6">
                <div>
                    <h3 class="text-lg font-bold text-slate-900 dark:text-white">Performance Impact Analysis</h3>
                    <p class="text-sm text-slate-500 dark:text-[#92adc9]">P95 Response time projection with optimized resources</p>
                </div>
                <div class="flex items-center gap-4">
                    <div class="flex items-center gap-2">
                        <div class="w-3 h-0.5 bg-slate-400"></div>
                        <span class="text-xs text-slate-500 font-medium">Historical</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <div class="w-3 h-0.5 bg-primary border-t border-dashed"></div>
                        <span class="text-xs text-slate-500 font-medium">Projected (AI)</span>
                    </div>
                </div>
            </div>

            <div class="w-full h-48 relative">
                <svg class="w-full h-full overflow-visible" preserveAspectRatio="none" viewBox="0 0 800 200">
                    <line class="text-slate-200 dark:text-slate-700" stroke="currentColor" stroke-width="1" x1="0" x2="800" y1="180" y2="180"></line>
                    <line class="text-slate-200 dark:text-slate-700" stroke="currentColor" stroke-dasharray="4 4" stroke-width="1" x1="0" x2="800" y1="120" y2="120"></line>
                    <line class="text-slate-200 dark:text-slate-700" stroke="currentColor" stroke-dasharray="4 4" stroke-width="1" x1="0" x2="800" y1="60" y2="60"></line>

                    <path class="text-slate-400" d="M0 140 Q 100 130, 200 150 T 400 140" fill="none" stroke="currentColor" stroke-width="2"></path>
                    <path d="M400 140 Q 500 145, 600 135 T 800 130" fill="none" stroke="#137fec" stroke-dasharray="6 4" stroke-width="2"></path>

                    <circle class="fill-slate-500" cx="400" cy="140" r="4"></circle>
                    <circle class="fill-primary" cx="800" cy="130" r="4"></circle>

                    <text class="text-xs fill-primary font-bold" x="810" y="134">${Math.round(analysis.p95ResponseTime || 142)}ms</text>
                </svg>

                <div class="flex justify-between mt-2 text-xs text-slate-400 font-mono">
                    <span>T-24h</span>
                    <span>T-12h</span>
                    <span>Now</span>
                    <span>T+12h</span>
                    <span>T+24h</span>
                </div>
            </div>
        </div>
    `;
}

// Render Resource Utilization Chart
function renderResourceChart(analysis) {
    const avgCpuUsage = analysis.p95CpuUsage || 35;
    const currentLimit = 100;
    const newLimit = 50;

    return `
        <div class="bg-surface-light dark:bg-[#1d2832] rounded-xl border border-slate-200 dark:border-[#324d67] p-5 shadow-sm">
            <h3 class="text-base font-bold text-slate-900 dark:text-white mb-1">CPU Usage vs Limits</h3>
            <p class="text-sm text-slate-500 mb-4">Last 30 Days</p>

            <div class="relative h-40 w-full mb-4">
                <svg class="overflow-visible" fill="none" height="100%" preserveAspectRatio="none" viewBox="0 0 400 150" width="100%">
                    <defs>
                        <linearGradient gradientUnits="userSpaceOnUse" id="gradient-cpu" x1="200" x2="200" y1="0" y2="150">
                            <stop stop-color="#137fec" stop-opacity="0.5"></stop>
                            <stop offset="1" stop-color="#137fec" stop-opacity="0"></stop>
                        </linearGradient>
                    </defs>

                    <path d="M0 120 C 50 120, 100 40, 150 40 C 200 40, 250 80, 300 80 S 350 20, 400 20 V 150 H 0 Z" fill="url(#gradient-cpu)" opacity="0.4"></path>
                    <path d="M0 120 C 50 120, 100 40, 150 40 C 200 40, 250 80, 300 80 S 350 20, 400 20" fill="none" stroke="#137fec" stroke-width="2"></path>

                    <line stroke="#ef4444" stroke-dasharray="4 2" stroke-width="1" x1="0" x2="400" y1="10" y2="10"></line>
                    <line stroke="#22c55e" stroke-dasharray="4 2" stroke-width="1" x1="0" x2="400" y1="60" y2="60"></line>
                </svg>

                <div class="absolute top-[8px] right-0 text-[10px] text-red-500 bg-surface-light dark:bg-[#1d2832] px-1">Limit (Current): ${currentLimit}%</div>
                <div class="absolute top-[58px] right-0 text-[10px] text-green-600 bg-surface-light dark:bg-[#1d2832] px-1">Limit (Optimized): ${newLimit}%</div>
            </div>

            <div class="flex justify-between items-center text-sm">
                <span class="text-slate-500">Avg Usage</span>
                <span class="font-mono font-bold text-slate-900 dark:text-white">${avgCpuUsage.toFixed(1)}%</span>
            </div>
        </div>
    `;
}

// Render AI Insights
function renderAIInsights(analysis) {
    const insights = [];

    // Over-provisioned detection
    if (analysis.p95CpuUsage < 40 || analysis.p95MemoryUsage < 40) {
        insights.push({
            color: 'indigo',
            icon: 'psychology',
            title: 'Over-provisioned',
            description: `This service has utilized <40% of its requested resources for the last 90 days. Reducing requests is safe.`
        });
    }

    // HPA recommendation
    if (analysis.cpuThrottlingDetected) {
        insights.push({
            color: 'amber',
            icon: 'speed',
            title: 'HPA Recommended',
            description: `With tighter CPU limits, enable Horizontal Pod Autoscaling (HPA) to handle burst traffic.`
        });
    }

    // QoS class warning
    insights.push({
        color: 'slate',
        icon: 'security',
        title: 'QoS Class',
        description: `Change will shift QoS from Guaranteed to Burstable. Ensure node capacity is sufficient.`
    });

    return `
        <div class="flex flex-col gap-4">
            <h3 class="text-sm font-bold text-slate-500 uppercase tracking-wider px-1">AI Insights & Tips</h3>
            ${insights.map(insight => `
                <div class="p-4 rounded-lg ${getInsightColorClasses(insight.color)} flex gap-3">
                    <span class="material-symbols-outlined ${getInsightIconColor(insight.color)} shrink-0">${insight.icon}</span>
                    <div>
                        <h4 class="text-sm font-bold ${getInsightTitleColor(insight.color)} mb-1">${insight.title}</h4>
                        <p class="text-xs ${getInsightDescColor(insight.color)} leading-relaxed">${insight.description}</p>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

// Helper functions for insight colors
function getInsightColorClasses(color) {
    const classes = {
        'indigo': 'bg-indigo-50 dark:bg-indigo-900/20 border border-indigo-100 dark:border-indigo-800',
        'amber': 'bg-amber-50 dark:bg-amber-900/10 border border-amber-100 dark:border-amber-800',
        'slate': 'bg-surface-light dark:bg-[#1d2832] border border-slate-200 dark:border-[#324d67] shadow-sm'
    };
    return classes[color] || classes['slate'];
}

function getInsightIconColor(color) {
    const colors = {
        'indigo': 'text-indigo-600 dark:text-indigo-400',
        'amber': 'text-amber-600 dark:text-amber-400',
        'slate': 'text-slate-400'
    };
    return colors[color] || colors['slate'];
}

function getInsightTitleColor(color) {
    const colors = {
        'indigo': 'text-indigo-900 dark:text-indigo-300',
        'amber': 'text-amber-900 dark:text-amber-300',
        'slate': 'text-slate-900 dark:text-white'
    };
    return colors[color] || colors['slate'];
}

function getInsightDescColor(color) {
    const colors = {
        'indigo': 'text-indigo-700 dark:text-indigo-200',
        'amber': 'text-amber-700 dark:text-amber-200',
        'slate': 'text-slate-500 dark:text-slate-400'
    };
    return colors[color] || colors['slate'];
}

// Error state rendering
function renderErrorState() {
    const content = document.getElementById('main-content');
    content.innerHTML = `
        <div class="flex flex-col items-center justify-center h-96 gap-4">
            <span class="material-symbols-outlined text-6xl text-slate-300 dark:text-slate-600">error_outline</span>
            <h2 class="text-2xl font-bold text-slate-900 dark:text-white">Failed to Load Dashboard</h2>
            <p class="text-slate-500 dark:text-slate-400">Make sure the analyzer service is running on port 8084</p>
            <button onclick="loadDashboard()" class="h-11 px-6 rounded-lg bg-primary hover:bg-primary/90 text-white font-bold">
                Try Again
            </button>
        </div>
    `;
}

// Refresh all data
async function refreshData() {
    if (currentService) {
        await loadServiceDetail(currentService);
    } else {
        await loadDashboard();
    }
    showToast('Dashboard refreshed', 'success');
}

// Analyze single service
async function analyzeService(serviceName) {
    try {
        showToast(`Analyzing ${serviceName}...`, 'info');
        const recommendation = await API.analyzeService(serviceName);
        serviceRecommendations[serviceName] = recommendation;

        // Reload service detail
        await loadServiceDetail(serviceName);
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
    toast.className = `px-4 py-3 rounded-lg shadow-lg flex items-center gap-3 transform transition-all duration-300 ${getToastClasses(type)}`;

    const icon = getToastIcon(type);
    toast.innerHTML = `
        <span class="material-symbols-outlined text-xl">${icon}</span>
        <span class="font-medium">${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => toast.classList.add('translate-x-0', 'opacity-100'), 10);

    setTimeout(() => {
        toast.classList.add('translate-x-full', 'opacity-0');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function getToastClasses(type) {
    const classes = {
        'success': 'bg-green-500 text-white',
        'error': 'bg-red-500 text-white',
        'info': 'bg-blue-500 text-white',
        'warning': 'bg-amber-500 text-white'
    };
    return classes[type] || classes['info'];
}

function getToastIcon(type) {
    const icons = {
        'success': 'check_circle',
        'error': 'error',
        'info': 'info',
        'warning': 'warning'
    };
    return icons[type] || icons['info'];
}
