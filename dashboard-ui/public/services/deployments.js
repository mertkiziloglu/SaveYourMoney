// Deployments Page Logic with Firestore Integration

let allDeployments = [];
let filteredDeployments = [];
let currentPage = 1;
const deploymentsPerPage = 10;

// State
let filters = {
    search: '',
    cluster: '',
    namespace: '',
    status: ''
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', async () => {
    console.log('Deployments page initialized');

    // Initialize Firebase
    const initialized = initializeFirebase();

    if (initialized) {
        await loadDeploymentsData();
        setupEventListeners();
    } else {
        renderFirebaseError();
    }
});

// Load deployments data from Firestore
async function loadDeploymentsData() {
    try {
        showLoading();

        // Fetch all deployments
        allDeployments = await FirestoreAPI.getDeployments();
        filteredDeployments = [...allDeployments];

        // Populate filter dropdowns
        populateFilters();

        // Render summary cards
        renderSummaryCards();

        // Render deployments table
        renderDeploymentsTable();

        hideLoading();
    } catch (error) {
        console.error('Error loading deployments:', error);
        renderErrorState();
    }
}

// Populate filter dropdowns
function populateFilters() {
    const clusters = [...new Set(allDeployments.map(d => d.cluster))];
    const namespaces = [...new Set(allDeployments.map(d => d.namespace))];

    const clusterSelect = document.getElementById('cluster-filter');
    const namespaceSelect = document.getElementById('namespace-filter');

    clusters.forEach(cluster => {
        const option = document.createElement('option');
        option.value = cluster;
        option.textContent = cluster;
        clusterSelect.appendChild(option);
    });

    namespaces.forEach(namespace => {
        const option = document.createElement('option');
        option.value = namespace;
        option.textContent = namespace;
        namespaceSelect.appendChild(option);
    });
}

// Render summary cards
function renderSummaryCards() {
    const totalDeployments = allDeployments.length;
    const deploymentsWithSavings = allDeployments.filter(d => d.hasRecommendation && d.potentialSavings > 0).length;
    const totalPotentialSavings = allDeployments
        .filter(d => d.potentialSavings > 0)
        .reduce((sum, d) => sum + d.potentialSavings, 0);

    const container = document.getElementById('summary-cards');
    container.innerHTML = `
        <div class="bg-gradient-to-r from-blue-600 to-blue-500 rounded-xl p-6 text-white shadow-lg relative overflow-hidden">
            <div class="absolute right-0 top-0 opacity-10 transform translate-x-1/4 -translate-y-1/4">
                <span class="material-symbols-outlined text-[150px]">dns</span>
            </div>
            <div class="relative z-10">
                <h3 class="text-blue-100 text-sm font-medium uppercase tracking-wide">Total Active Deployments</h3>
                <p class="text-4xl font-black mt-2">${totalDeployments}</p>
                <div class="flex items-center gap-2 mt-4 text-sm text-blue-50">
                    <span class="material-symbols-outlined text-sm">trending_up</span>
                    <span>+${Math.floor(totalDeployments * 0.02)} deployed today</span>
                </div>
            </div>
        </div>

        <div class="bg-surface-light dark:bg-[#1d2832] rounded-xl p-6 border border-slate-200 dark:border-[#324d67] shadow-sm relative overflow-hidden">
            <div class="flex justify-between items-start">
                <div>
                    <h3 class="text-slate-500 dark:text-slate-400 text-sm font-medium uppercase tracking-wide">Deployments with Savings Potential</h3>
                    <p class="text-4xl font-black mt-2 text-slate-900 dark:text-white">${deploymentsWithSavings}</p>
                    <div class="flex items-center gap-2 mt-4 text-sm text-emerald-600 dark:text-emerald-400 font-medium">
                        <span class="material-symbols-outlined text-sm">savings</span>
                        <span>Potential savings: ~$${totalPotentialSavings.toLocaleString()}/mo</span>
                    </div>
                </div>
                <div class="size-12 rounded-full bg-emerald-100 dark:bg-emerald-900/30 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
                    <span class="material-symbols-outlined">auto_awesome</span>
                </div>
            </div>
        </div>
    `;
}

// Render deployments table
function renderDeploymentsTable() {
    const tbody = document.getElementById('deployments-tbody');

    // Paginate
    const startIndex = (currentPage - 1) * deploymentsPerPage;
    const endIndex = startIndex + deploymentsPerPage;
    const paginatedDeployments = filteredDeployments.slice(startIndex, endIndex);

    if (paginatedDeployments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="px-6 py-12 text-center text-slate-500">
                    <div class="flex flex-col items-center gap-3">
                        <span class="material-symbols-outlined text-5xl text-slate-300">search_off</span>
                        <p>No deployments found matching your filters</p>
                    </div>
                </td>
            </tr>
        `;
        updatePagination();
        return;
    }

    tbody.innerHTML = paginatedDeployments.map(deployment => renderDeploymentRow(deployment)).join('');
    updatePagination();
}

// Render single deployment row
function renderDeploymentRow(d) {
    const statusBadgeClasses = {
        'Healthy': 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 border-green-200 dark:border-green-800',
        'Warning': 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400 border-amber-200 dark:border-amber-800',
        'Critical': 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 border-red-200 dark:border-red-800'
    };

    const cpuUsageColor = d.metrics.cpuUsagePercent > 80 ? 'text-amber-600 dark:text-amber-400 font-bold' :
                          d.metrics.cpuUsagePercent > 95 ? 'text-red-600 dark:text-red-400 font-bold' :
                          'text-slate-900 dark:text-white';

    const optimizationStatus = d.hasRecommendation ? `
        <div class="flex items-center gap-2">
            <span class="relative flex h-2.5 w-2.5">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                <span class="relative inline-flex rounded-full h-2.5 w-2.5 bg-primary"></span>
            </span>
            <span class="text-primary font-medium">Recommendation Available</span>
        </div>
        <div class="text-xs text-slate-500 mt-0.5">${d.potentialSavings > 0 ? `Save approx. $${d.potentialSavings}/mo` : d.potentialSavings < 0 ? 'Scale up recommended' : 'Optimized'}</div>
    ` : `
        <div class="flex items-center gap-2">
            <span class="h-2.5 w-2.5 rounded-full bg-slate-300 dark:bg-slate-600"></span>
            <span class="text-slate-500 dark:text-slate-400">Optimized</span>
        </div>
    `;

    return `
        <tr class="hover:bg-slate-50 dark:hover:bg-[#233648]/50 transition-colors">
            <td class="px-6 py-4">
                <div class="flex flex-col">
                    <span class="font-bold text-slate-900 dark:text-white">${d.name}</span>
                    <span class="text-xs text-slate-500">cluster: ${d.cluster}</span>
                </div>
            </td>
            <td class="px-6 py-4">
                <span class="font-mono text-slate-600 dark:text-slate-400">${d.namespace}</span>
            </td>
            <td class="px-6 py-4">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusBadgeClasses[d.status]} border">
                    ${d.status}
                </span>
            </td>
            <td class="px-6 py-4">
                <div class="flex flex-col gap-1 text-xs font-mono">
                    <div class="flex items-center gap-2">
                        <span class="text-slate-500 w-8">CPU:</span>
                        <span class="${cpuUsageColor}">${d.currentResources.cpuRequest} / ${d.currentResources.cpuLimit}</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-slate-500 w-8">RAM:</span>
                        <span class="text-slate-900 dark:text-white">${d.currentResources.memoryRequest} / ${d.currentResources.memoryLimit}</span>
                    </div>
                </div>
            </td>
            <td class="px-6 py-4">
                ${optimizationStatus}
            </td>
            <td class="px-6 py-4 text-right">
                <button
                    onclick="viewDeploymentRecommendation('${d.id}')"
                    class="inline-flex items-center justify-center px-4 py-2 border border-slate-300 dark:border-[#324d67] shadow-sm text-sm font-medium rounded-md text-slate-700 dark:text-slate-200 bg-white dark:bg-[#111a22] hover:bg-slate-50 dark:hover:bg-[#233648] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary ${!d.hasRecommendation ? 'opacity-50 cursor-not-allowed' : ''}"
                    ${!d.hasRecommendation ? 'disabled' : ''}
                >
                    View Recommendations
                </button>
            </td>
        </tr>
    `;
}

// Update pagination
function updatePagination() {
    const totalPages = Math.ceil(filteredDeployments.length / deploymentsPerPage);
    const startIndex = (currentPage - 1) * deploymentsPerPage + 1;
    const endIndex = Math.min(currentPage * deploymentsPerPage, filteredDeployments.length);

    document.getElementById('pagination-info').innerHTML = `
        Showing <span class="font-medium text-slate-900 dark:text-white">${startIndex}</span> to
        <span class="font-medium text-slate-900 dark:text-white">${endIndex}</span> of
        <span class="font-medium text-slate-900 dark:text-white">${filteredDeployments.length}</span> results
    `;

    document.getElementById('prev-btn').disabled = currentPage === 1;
    document.getElementById('next-btn').disabled = currentPage >= totalPages;
}

// Setup event listeners
function setupEventListeners() {
    // Search
    document.getElementById('search-input').addEventListener('input', (e) => {
        filters.search = e.target.value.toLowerCase();
        applyFilters();
    });

    // Cluster filter
    document.getElementById('cluster-filter').addEventListener('change', (e) => {
        filters.cluster = e.target.value;
        applyFilters();
    });

    // Namespace filter
    document.getElementById('namespace-filter').addEventListener('change', (e) => {
        filters.namespace = e.target.value;
        applyFilters();
    });

    // Status filter
    document.getElementById('status-filter').addEventListener('change', (e) => {
        filters.status = e.target.value;
        applyFilters();
    });

    // Pagination
    document.getElementById('prev-btn').addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            renderDeploymentsTable();
        }
    });

    document.getElementById('next-btn').addEventListener('click', () => {
        const totalPages = Math.ceil(filteredDeployments.length / deploymentsPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            renderDeploymentsTable();
        }
    });
}

// Apply filters
function applyFilters() {
    filteredDeployments = allDeployments.filter(d => {
        const matchesSearch = !filters.search || d.name.toLowerCase().includes(filters.search);
        const matchesCluster = !filters.cluster || d.cluster === filters.cluster;
        const matchesNamespace = !filters.namespace || d.namespace === filters.namespace;
        const matchesStatus = !filters.status || d.status === filters.status;

        return matchesSearch && matchesCluster && matchesNamespace && matchesStatus;
    });

    currentPage = 1;
    renderDeploymentsTable();
}

// View deployment recommendation
function viewDeploymentRecommendation(deploymentId) {
    const deployment = allDeployments.find(d => d.id === deploymentId);
    if (!deployment) return;

    showToast(`Opening recommendations for ${deployment.name}...`, 'info');
    // TODO: Navigate to recommendation detail page or show modal
    console.log('View recommendation for:', deployment);
}

// Refresh deployments
async function refreshDeployments() {
    showToast('Refreshing deployments...', 'info');
    await loadDeploymentsData();
    showToast('Deployments refreshed successfully', 'success');
}

// Loading state
function showLoading() {
    const tbody = document.getElementById('deployments-tbody');
    tbody.innerHTML = `
        <tr>
            <td colspan="6" class="px-6 py-12 text-center">
                <div class="flex flex-col items-center gap-4">
                    <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <p class="text-slate-500 font-medium">Loading deployments...</p>
                </div>
            </td>
        </tr>
    `;
}

function hideLoading() {
    // Loading is hidden when content is rendered
}

// Error states
function renderErrorState() {
    const tbody = document.getElementById('deployments-tbody');
    tbody.innerHTML = `
        <tr>
            <td colspan="6" class="px-6 py-12 text-center">
                <div class="flex flex-col items-center gap-4">
                    <span class="material-symbols-outlined text-6xl text-slate-300">error_outline</span>
                    <h3 class="text-xl font-bold text-slate-900">Failed to Load Deployments</h3>
                    <p class="text-slate-500">Could not connect to Firestore database</p>
                    <button onclick="location.reload()" class="px-6 py-2 rounded-lg bg-primary hover:bg-primary/90 text-white font-bold">
                        Retry
                    </button>
                </div>
            </td>
        </tr>
    `;
}

function renderFirebaseError() {
    const tbody = document.getElementById('deployments-tbody');
    tbody.innerHTML = `
        <tr>
            <td colspan="6" class="px-6 py-12 text-center">
                <div class="flex flex-col items-center gap-4 max-w-2xl mx-auto">
                    <span class="material-symbols-outlined text-6xl text-red-300">cloud_off</span>
                    <h3 class="text-xl font-bold text-slate-900">Firebase Not Configured</h3>
                    <p class="text-slate-500">Please configure your Firebase project and reload the page</p>
                    <a href="../firebase-setup/README.md" class="text-primary hover:underline font-medium">View Setup Guide →</a>
                </div>
            </td>
        </tr>
    `;
}

// Toast notification
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

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

    const toast = document.createElement('div');
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
