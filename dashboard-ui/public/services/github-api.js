/**
 * GitHub REST API Client — SaveYourMoney Dashboard
 * 
 * Handles branch creation, file commits, and pull request 
 * creation against a GitHub repository via the v3 REST API.
 * 
 * Credentials are stored in localStorage under 'github_config'.
 */

const GitHubAPI = (() => {
    const STORAGE_KEY = 'github_config';
    const API_BASE = 'https://api.github.com';

    // ─── Configuration ───────────────────────────────────────

    function getConfig() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch {
            return null;
        }
    }

    function configure(token, owner, repo) {
        const config = { token, owner, repo };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
        return config;
    }

    function clearConfig() {
        localStorage.removeItem(STORAGE_KEY);
    }

    function isConfigured() {
        const cfg = getConfig();
        return !!(cfg && cfg.token && cfg.owner && cfg.repo);
    }

    // ─── HTTP Helpers ────────────────────────────────────────

    async function apiRequest(method, path, body = null) {
        const cfg = getConfig();
        if (!cfg || !cfg.token) {
            throw new Error('GitHub is not configured. Please set your Personal Access Token.');
        }

        const url = path.startsWith('http') ? path : `${API_BASE}${path}`;
        const headers = {
            'Authorization': `Bearer ${cfg.token}`,
            'Accept': 'application/vnd.github+json',
            'X-GitHub-Api-Version': '2022-11-28',
        };

        const options = { method, headers };
        if (body) {
            headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }

        const response = await fetch(url, options);

        if (!response.ok) {
            const errorBody = await response.text();
            let message;
            try {
                const parsed = JSON.parse(errorBody);
                message = parsed.message || errorBody;
            } catch {
                message = errorBody;
            }
            throw new Error(`GitHub API ${response.status}: ${message}`);
        }

        // 204 No Content
        if (response.status === 204) return null;
        return response.json();
    }

    // ─── Repository Operations ───────────────────────────────

    async function getDefaultBranch() {
        const cfg = getConfig();
        const repo = await apiRequest('GET', `/repos/${cfg.owner}/${cfg.repo}`);
        return repo.default_branch;
    }

    async function getLatestCommitSha(branch) {
        const cfg = getConfig();
        const ref = await apiRequest('GET', `/repos/${cfg.owner}/${cfg.repo}/git/ref/heads/${branch}`);
        return ref.object.sha;
    }

    async function createBranch(branchName, fromSha) {
        const cfg = getConfig();
        return apiRequest('POST', `/repos/${cfg.owner}/${cfg.repo}/git/refs`, {
            ref: `refs/heads/${branchName}`,
            sha: fromSha,
        });
    }

    /**
     * Create or update a single file on a branch.
     * Uses the Contents API which handles blob + tree + commit in one call.
     */
    async function commitFile(branch, filePath, content, commitMessage) {
        const cfg = getConfig();

        // Check if file exists (to get its sha for updates)
        let existingSha = null;
        try {
            const existing = await apiRequest(
                'GET',
                `/repos/${cfg.owner}/${cfg.repo}/contents/${filePath}?ref=${branch}`
            );
            existingSha = existing.sha;
        } catch {
            // File doesn't exist yet — that's fine, we'll create it
        }

        const body = {
            message: commitMessage,
            content: btoa(unescape(encodeURIComponent(content))),
            branch: branch,
        };
        if (existingSha) {
            body.sha = existingSha;
        }

        return apiRequest(
            'PUT',
            `/repos/${cfg.owner}/${cfg.repo}/contents/${filePath}`,
            body
        );
    }

    async function createPullRequest(title, body, headBranch, baseBranch) {
        const cfg = getConfig();
        const pr = await apiRequest('POST', `/repos/${cfg.owner}/${cfg.repo}/pulls`, {
            title,
            body,
            head: headBranch,
            base: baseBranch,
        });
        return pr;
    }

    // ─── High-Level Orchestrator ─────────────────────────────

    /**
     * Full PR workflow: branch → commit → PR.
     * Returns { prUrl, prNumber, branchName }
     */
    async function createOptimizationPR(service) {
        const defaultBranch = await getDefaultBranch();
        const headSha = await getLatestCommitSha(defaultBranch);

        // Deterministic branch name from service + timestamp
        const timestamp = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14);
        const branchName = `saveyourmoney/optimize-${service.serviceName}-${timestamp}`;

        // Step 1: Create feature branch
        await createBranch(branchName, headSha);

        // Step 2: Generate deployment YAML content
        const yamlContent = generateDeploymentYaml(service);

        // Step 3: Commit the file
        const filePath = `k8s/${service.serviceName}/deployment.yaml`;
        const commitMessage = `🚀 Optimize resources for ${service.serviceName}\n\nAI-recommended resource changes:\n- CPU Request: ${service.currentCpuRequest} → ${service.recommendedCpuRequest}\n- CPU Limit: ${service.currentCpuLimit} → ${service.recommendedCpuLimit}\n- Memory Request: ${service.currentMemoryRequest} → ${service.recommendedMemoryRequest}\n- Memory Limit: ${service.currentMemoryLimit} → ${service.recommendedMemoryLimit}\n\nEstimated savings: $${service.estimatedMonthlySavings.toFixed(2)}/month\n\nCo-Authored-By: SaveYourMoney AI <noreply@saveyourmoney.dev>`;

        await commitFile(branchName, filePath, yamlContent, commitMessage);

        // Step 4: Create the PR
        const prTitle = `🚀 Resource Optimization — ${service.serviceName} (-$${service.estimatedMonthlySavings.toFixed(0)}/mo)`;
        const prBody = generatePRBody(service);

        const pr = await createPullRequest(prTitle, prBody, branchName, defaultBranch);

        return {
            prUrl: pr.html_url,
            prNumber: pr.number,
            branchName,
        };
    }

    // ─── YAML & Markdown Generators ──────────────────────────

    function generateDeploymentYaml(service) {
        return `apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${service.serviceName}
  namespace: prod
  labels:
    app: ${service.serviceName}
    managed-by: saveyourmoney-ai
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ${service.serviceName}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: ${service.serviceName}
    spec:
      containers:
      - name: main
        image: gcr.io/saveyourmoney/${service.serviceName}:latest
        resources:
          requests:
            cpu: ${service.recommendedCpuRequest}
            memory: ${service.recommendedMemoryRequest}
          limits:
            cpu: ${service.recommendedCpuLimit}
            memory: ${service.recommendedMemoryLimit}
        ports:
        - containerPort: 8080
          protocol: TCP
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
`;
    }

    function generatePRBody(service) {
        const issues = [];
        if (service.cpuThrottlingDetected) issues.push('⚠️ CPU Throttling detected');
        if (service.memoryLeakDetected) issues.push('⚠️ Memory Leak pattern detected');
        if (service.connectionPoolExhaustion) issues.push('⚠️ Connection Pool exhaustion');

        return `## 🤖 AI-Generated Resource Optimization

**Service:** \`${service.serviceName}\`
**Generated:** ${new Date().toISOString()}
**Tool:** SaveYourMoney AI Engine

---

### 📊 Resource Changes

| Resource | Current | Recommended |
|----------|---------|-------------|
| CPU Request | \`${service.currentCpuRequest}\` | \`${service.recommendedCpuRequest}\` |
| CPU Limit | \`${service.currentCpuLimit}\` | \`${service.recommendedCpuLimit}\` |
| Memory Request | \`${service.currentMemoryRequest}\` | \`${service.recommendedMemoryRequest}\` |
| Memory Limit | \`${service.currentMemoryLimit}\` | \`${service.recommendedMemoryLimit}\` |

### 💰 Expected Impact

- **Monthly Savings:** -$${service.estimatedMonthlySavings.toFixed(2)}/mo
- **Annual Savings:** -$${(service.estimatedMonthlySavings * 12).toFixed(2)}/year

### 🔍 Detected Issues

${issues.length > 0 ? issues.join('\n') : '✅ No critical issues — optimization opportunity'}

### 📈 Usage Metrics

- P95 CPU Usage: ${service.p95CpuUsage}%
- P95 Memory Usage: ${service.p95MemoryUsage}%

---

> **Generated by [SaveYourMoney](https://github.com/mertkiziloglu/SaveYourMoney) — AI-Powered Resource Optimization** 🚀
`;
    }

    // ─── Public API ──────────────────────────────────────────

    return {
        configure,
        clearConfig,
        getConfig,
        isConfigured,
        getDefaultBranch,
        getLatestCommitSha,
        createBranch,
        commitFile,
        createPullRequest,
        createOptimizationPR,
    };
})();
