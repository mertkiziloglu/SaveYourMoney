// Charts Service - SaveYourMoney Dashboard

const Charts = {
    cpuChart: null,
    memoryChart: null,
    dbChart: null,

    // Create CPU metrics chart
    createCPUChart(analysisData) {
        const ctx = document.getElementById('cpu-chart');
        if (!ctx) return;

        if (this.cpuChart) {
            this.cpuChart.destroy();
        }

        const currentCpu = this.parseCpuToMillicores(analysisData.currentCpuRequest || '100m');
        const recommendedCpu = this.parseCpuToMillicores(analysisData.recommendedCpuRequest || '500m');

        this.cpuChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['CPU Request', 'CPU Limit'],
                datasets: [
                    {
                        label: 'Current (Suboptimal)',
                        data: [
                            this.parseCpuToMillicores(analysisData.currentCpuRequest),
                            this.parseCpuToMillicores(analysisData.currentCpuLimit)
                        ],
                        backgroundColor: 'rgba(239, 68, 68, 0.7)',
                        borderColor: 'rgba(239, 68, 68, 1)',
                        borderWidth: 2
                    },
                    {
                        label: 'Recommended (Optimized)',
                        data: [
                            this.parseCpuToMillicores(analysisData.recommendedCpuRequest),
                            this.parseCpuToMillicores(analysisData.recommendedCpuLimit)
                        ],
                        backgroundColor: 'rgba(16, 185, 129, 0.7)',
                        borderColor: 'rgba(16, 185, 129, 1)',
                        borderWidth: 2
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'CPU Resources Comparison',
                        font: { size: 16, weight: 'bold' }
                    },
                    legend: {
                        display: true,
                        position: 'top'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Millicores (m)'
                        }
                    }
                }
            }
        });
    },

    // Create Memory metrics chart
    createMemoryChart(analysisData) {
        const ctx = document.getElementById('memory-chart');
        if (!ctx) return;

        if (this.memoryChart) {
            this.memoryChart.destroy();
        }

        this.memoryChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['Memory Request', 'Memory Limit'],
                datasets: [
                    {
                        label: 'Current (Suboptimal)',
                        data: [
                            this.parseMemoryToMi(analysisData.currentMemoryRequest),
                            this.parseMemoryToMi(analysisData.currentMemoryLimit)
                        ],
                        backgroundColor: 'rgba(239, 68, 68, 0.7)',
                        borderColor: 'rgba(239, 68, 68, 1)',
                        borderWidth: 2
                    },
                    {
                        label: 'Recommended (Optimized)',
                        data: [
                            this.parseMemoryToMi(analysisData.recommendedMemoryRequest),
                            this.parseMemoryToMi(analysisData.recommendedMemoryLimit)
                        ],
                        backgroundColor: 'rgba(16, 185, 129, 0.7)',
                        borderColor: 'rgba(16, 185, 129, 1)',
                        borderWidth: 2
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'Memory Resources Comparison',
                        font: { size: 16, weight: 'bold' }
                    },
                    legend: {
                        display: true,
                        position: 'top'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: {
                            display: true,
                            text: 'Megabytes (Mi)'
                        }
                    }
                }
            }
        });
    },

    // Create DB Connection Pool chart
    createDBChart(analysisData) {
        const ctx = document.getElementById('db-chart');
        if (!ctx) return;

        if (this.dbChart) {
            this.dbChart.destroy();
        }

        const currentPool = 5; // Hardcoded current suboptimal value
        const recommendedPool = analysisData.recommendedMaxPoolSize || 50;

        this.dbChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Current Pool Size', 'Recommended Pool Size'],
                datasets: [{
                    data: [currentPool, recommendedPool - currentPool],
                    backgroundColor: [
                        'rgba(239, 68, 68, 0.7)',
                        'rgba(16, 185, 129, 0.7)'
                    ],
                    borderColor: [
                        'rgba(239, 68, 68, 1)',
                        'rgba(16, 185, 129, 1)'
                    ],
                    borderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    title: {
                        display: true,
                        text: 'Connection Pool Size Comparison',
                        font: { size: 16, weight: 'bold' }
                    },
                    legend: {
                        display: true,
                        position: 'bottom'
                    }
                }
            }
        });
    },

    // Helper: Parse CPU to millicores
    parseCpuToMillicores(cpu) {
        if (!cpu) return 0;
        if (cpu.endsWith('m')) {
            return parseInt(cpu.replace('m', ''));
        }
        return parseFloat(cpu) * 1000;
    },

    // Helper: Parse Memory to Mi
    parseMemoryToMi(memory) {
        if (!memory) return 0;
        if (memory.endsWith('Mi')) {
            return parseInt(memory.replace('Mi', ''));
        } else if (memory.endsWith('Gi')) {
            return parseInt(memory.replace('Gi', '')) * 1024;
        }
        return parseInt(memory) / (1024 * 1024);
    }
};
