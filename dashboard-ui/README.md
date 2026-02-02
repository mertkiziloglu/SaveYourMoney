# SaveYourMoney Dashboard

## Overview

Modern web dashboard for visualizing AI-powered resource optimization recommendations and cost savings.

---

## Features

- 📊 **Real-time Metrics** - Live service health and performance metrics
- 💰 **Cost Savings Calculator** - Monthly and annual savings visualization
- 🎯 **Issue Detection** - Automatic detection of CPU throttling, memory leaks, connection pool exhaustion
- 📈 **Before/After Comparison** - Visual charts showing current vs optimized resources
- 🚀 **One-Click Actions** - Generate code and create Azure DevOps PRs directly from dashboard
- 🎨 **Modern UI** - Responsive design with gradient backgrounds and smooth animations

---

## Quick Start

### 1. Start Backend Services

```bash
# Terminal 1 - Analyzer Service (Port 8084)
cd ~/Desktop/SaveYourMoney/analyzer-service
mvn spring-boot:run

# Terminal 2 - Code Generator Service (Port 8085)
cd ~/Desktop/SaveYourMoney/code-generator-service
mvn spring-boot:run

# Terminal 3, 4, 5 - Demo Services
cd ~/Desktop/SaveYourMoney/demo-services/cpu-hungry-service
mvn spring-boot:run

cd ~/Desktop/SaveYourMoney/demo-services/memory-leaker-service
mvn spring-boot:run

cd ~/Desktop/SaveYourMoney/demo-services/db-connection-service
mvn spring-boot:run
```

### 2. Serve Dashboard

```bash
cd ~/Desktop/SaveYourMoney/dashboard-ui/public

# Option 1: Python HTTP Server
python3 -m http.server 8080

# Option 2: Node.js HTTP Server
npx http-server -p 8080

# Option 3: PHP Built-in Server
php -S localhost:8080
```

### 3. Open in Browser

```
http://localhost:8080
```

---

## Dashboard Sections

### Summary Cards

- **Services Analyzed** - Total number of services being monitored
- **Monthly Savings** - Estimated cost savings per month
- **Annual Savings** - Estimated cost savings per year
- **Issues Detected** - Total critical issues found

### Service Cards

Each service (CPU-Hungry, Memory-Leaker, DB-Connection) shows:

1. **Status Badge** - Current health status
2. **Key Metrics** - P95 CPU/Memory usage, response times, confidence score
3. **Issues** - Detected problems (throttling, leaks, exhaustion)
4. **Changes Table** - Before/After comparison of resource configurations
5. **Chart** - Visual comparison of current vs recommended resources
6. **Actions** - Generate code or create Pull Request buttons

---

## API Endpoints Used

Dashboard connects to:

**Analyzer Service (Port 8084):**
- `GET /api/dashboard` - Dashboard summary
- `POST /api/analyze/{serviceName}` - Analyze single service
- `POST /api/analyze-all` - Analyze all services
- `GET /api/latest-analysis/{serviceName}` - Get latest analysis
- `GET /api/metrics/{serviceName}` - Get raw metrics

**Code Generator Service (Port 8085):**
- `POST /api/generate` - Generate configuration files
- `POST /api/generate-and-pr` - Generate + create Azure DevOps PR

---

## Usage

### Auto-Refresh

Dashboard automatically refreshes every 30 seconds to show latest data.

### Manual Refresh

Click the **🔄 Refresh** button to reload data immediately.

### Analyze Services

**Option 1: Analyze All**
```
Click "🤖 Analyze All Services" button
```

**Option 2: Analyze Individual**
```
Click "Analyze" button on specific service card
```

### Generate Code

1. Ensure service is analyzed (click Analyze button)
2. Click **📄 Generate Code** on service card
3. Files are generated and saved to `./output/` directory

### Create Pull Request

1. Ensure service is analyzed
2. Click **🚀 Create Pull Request** on service card
3. PR is created in Azure DevOps
4. Browser opens to PR URL automatically

---

## Screenshots

### Dashboard Overview
- Modern gradient background
- 4 summary cards with key metrics
- 3 service cards with detailed analysis

### Service Card
- Status indicator (Analyzing / Issues / Optimized)
- Metrics grid (P95 usage, response time, confidence)
- Issues badges (CPU Throttling, Memory Leak, etc.)
- Changes table (Before → After comparison)
- Chart visualization (Bar chart or Doughnut chart)
- Action buttons (Generate Code, Create PR)

---

## Technology Stack

- **HTML5** - Semantic markup
- **CSS3** - Modern styling with gradients, animations
- **Vanilla JavaScript** - No framework dependencies
- **Chart.js** - Data visualization
- **Fetch API** - REST API calls

---

## Configuration

Edit `services/api.js` to change API URLs:

```javascript
const API_BASE_URL = 'http://localhost:8084/api';  // Analyzer
const CODE_GEN_URL = 'http://localhost:8085/api';  // Code Generator
```

---

## Browser Support

- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

---

## Troubleshooting

### Dashboard shows "Failed to load"

**Cause:** Backend services not running

**Solution:**
```bash
# Check if services are running
curl http://localhost:8084/api/dashboard
curl http://localhost:8085/api/health

# Start missing services
cd analyzer-service && mvn spring-boot:run
cd code-generator-service && mvn spring-boot:run
```

### CORS errors in console

**Cause:** Dashboard running on different port than API

**Solution:** Backend services already have CORS enabled. Make sure you're accessing dashboard via `http://localhost:8080`

### Charts not displaying

**Cause:** Chart.js not loaded

**Solution:** Check internet connection. Chart.js is loaded via CDN:
```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
```

### "No analysis data yet"

**Cause:** Services haven't been analyzed

**Solution:**
1. Run JMeter tests to generate metrics
2. Wait 30 seconds for metrics collection
3. Click "Analyze" button

---

## Demo Workflow

### Complete End-to-End Demo:

```bash
# 1. Start all services (5 terminals)
# ... (see Quick Start above)

# 2. Run JMeter tests (generate metrics)
cd ~/Desktop/SaveYourMoney/jmeter-tests
./run-all-tests.sh

# 3. Wait for metrics collection (30 seconds)
sleep 30

# 4. Open Dashboard
open http://localhost:8080

# 5. Click "Analyze All Services"
# ... wait for analysis to complete

# 6. Review recommendations in each service card

# 7. Click "Create Pull Request" for any service
# ... PR opens in Azure DevOps
```

---

## Customization

### Change Colors

Edit `public/styles/main.css`:

```css
:root {
    --primary-color: #4f46e5;     /* Purple */
    --success-color: #10b981;     /* Green */
    --warning-color: #f59e0b;     /* Orange */
    --danger-color: #ef4444;      /* Red */
}
```

### Add New Metrics

Edit `services/app.js` in `updateServiceCard()` function to add more metrics.

---

## File Structure

```
dashboard-ui/
├── public/
│   ├── index.html           # Main HTML
│   ├── styles/
│   │   └── main.css        # Styling
│   └── services/
│       ├── api.js          # API client
│       ├── charts.js       # Chart.js logic
│       └── app.js          # Main app logic
└── README.md
```

---

**Built for SaveYourMoney Hackathon 2025** 🚀💰
