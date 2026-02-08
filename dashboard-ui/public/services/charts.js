/**
 * SaveYourMoney Chart.js Integration
 * Renders interactive charts for dashboard metrics visualization.
 */

const ChartManager = {
    instances: {},

    /**
     * Initialize a cost savings comparison bar chart.
     * @param {string} canvasId  - the <canvas> element ID
     * @param {Object} data      - { labels: string[], current: number[], optimized: number[] }
     */
    renderCostChart(canvasId, data) {
        this.destroy(canvasId);
        const ctx = document.getElementById(canvasId)?.getContext('2d');
        if (!ctx) return;

        this.instances[canvasId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: data.labels,
                datasets: [
                    {
                        label: 'Current Monthly Cost ($)',
                        data: data.current,
                        backgroundColor: 'rgba(239, 68, 68, 0.7)',
                        borderColor: 'rgba(239, 68, 68, 1)',
                        borderWidth: 1,
                        borderRadius: 6
                    },
                    {
                        label: 'Optimized Monthly Cost ($)',
                        data: data.optimized,
                        backgroundColor: 'rgba(34, 197, 94, 0.7)',
                        borderColor: 'rgba(34, 197, 94, 1)',
                        borderWidth: 1,
                        borderRadius: 6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top',
                        labels: { font: { family: 'Inter, sans-serif', size: 12 }, usePointStyle: true }
                    },
                    tooltip: {
                        callbacks: {
                            label: ctx => `${ctx.dataset.label}: $${ctx.parsed.y.toFixed(2)}`
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: v => `$${v}` },
                        grid: { color: 'rgba(148,163,184,0.1)' }
                    },
                    x: { grid: { display: false } }
                }
            }
        });
    },

    /**
     * Initialize a CPU/Memory usage doughnut gauge.
     * @param {string} canvasId
     * @param {number} usage    - percentage 0-100
     * @param {string} label    - e.g. 'CPU' or 'Memory'
     */
    renderGaugeChart(canvasId, usage, label) {
        this.destroy(canvasId);
        const ctx = document.getElementById(canvasId)?.getContext('2d');
        if (!ctx) return;

        const color = usage > 80 ? '#ef4444' : usage > 60 ? '#f59e0b' : '#22c55e';

        this.instances[canvasId] = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: [`${label} Used`, 'Available'],
                datasets: [{
                    data: [usage, 100 - usage],
                    backgroundColor: [color, 'rgba(148,163,184,0.1)'],
                    borderWidth: 0,
                    cutout: '75%'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: { label: ctx => `${ctx.label}: ${ctx.parsed}%` }
                    }
                }
            },
            plugins: [{
                id: 'centerText',
                afterDraw(chart) {
                    const { ctx: c, chartArea: { width, height, top, left } } = chart;
                    c.save();
                    c.font = 'bold 24px Inter, sans-serif';
                    c.fillStyle = getComputedStyle(document.documentElement)
                        .getPropertyValue('--chart-text') || '#1e293b';
                    c.textAlign = 'center';
                    c.textBaseline = 'middle';
                    c.fillText(`${Math.round(usage)}%`, left + width / 2, top + height / 2);
                    c.restore();
                }
            }]
        });
    },

    /**
     * Render anomaly timeline as a line chart.
     * @param {string} canvasId
     * @param {Object} data - { timestamps: string[], counts: number[] }
     */
    renderTimelineChart(canvasId, data) {
        this.destroy(canvasId);
        const ctx = document.getElementById(canvasId)?.getContext('2d');
        if (!ctx) return;

        this.instances[canvasId] = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.timestamps,
                datasets: [{
                    label: 'Anomalies',
                    data: data.counts,
                    borderColor: '#f59e0b',
                    backgroundColor: 'rgba(245, 158, 11, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointBackgroundColor: '#f59e0b'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 },
                        grid: { color: 'rgba(148,163,184,0.1)' }
                    },
                    x: { grid: { display: false } }
                }
            }
        });
    },

    /** Destroy a chart instance to prevent memory leaks. */
    destroy(canvasId) {
        if (this.instances[canvasId]) {
            this.instances[canvasId].destroy();
            delete this.instances[canvasId];
        }
    }
};
