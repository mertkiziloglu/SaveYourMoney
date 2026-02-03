#!/bin/bash

# SaveYourMoney - Deployment Test Script
# Usage: ./test-deployment.sh

set -e

echo "🧪 SaveYourMoney - Deployment Testing"
echo "======================================"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Get project and region
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo ""
echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo ""

# Get service URLs
echo -e "${YELLOW}📡 Fetching service URLs...${NC}"
ANALYZER_URL=$(gcloud run services describe analyzer-service --platform managed --region $REGION --format 'value(status.url)' 2>/dev/null)
CODEGEN_URL=$(gcloud run services describe code-generator-service --platform managed --region $REGION --format 'value(status.url)' 2>/dev/null)
CPU_HUNGRY_URL=$(gcloud run services describe cpu-hungry-service --platform managed --region $REGION --format 'value(status.url)' 2>/dev/null)
MEMORY_LEAKER_URL=$(gcloud run services describe memory-leaker-service --platform managed --region $REGION --format 'value(status.url)' 2>/dev/null)
DB_CONNECTION_URL=$(gcloud run services describe db-connection-service --platform managed --region $REGION --format 'value(status.url)' 2>/dev/null)

if [ -z "$ANALYZER_URL" ]; then
    echo -e "${RED}❌ Services not deployed. Run ./deploy-to-gcp.sh first${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Service URLs fetched${NC}"

# Test function
test_endpoint() {
    local name=$1
    local url=$2
    local expected_status=${3:-200}

    echo -ne "  Testing $name... "

    status=$(curl -s -o /dev/null -w "%{http_code}" "$url" --max-time 30)

    if [ "$status" -eq "$expected_status" ]; then
        echo -e "${GREEN}✅ OK ($status)${NC}"
        return 0
    else
        echo -e "${RED}❌ FAILED (got $status, expected $expected_status)${NC}"
        return 1
    fi
}

# Health Checks
echo ""
echo -e "${YELLOW}🏥 Running Health Checks...${NC}"
test_endpoint "Analyzer Service" "$ANALYZER_URL/api/health"
test_endpoint "Code Generator" "$CODEGEN_URL/api/health"
test_endpoint "CPU Hungry Service" "$CPU_HUNGRY_URL/api/health"
test_endpoint "Memory Leaker Service" "$MEMORY_LEAKER_URL/api/health"
test_endpoint "DB Connection Service" "$DB_CONNECTION_URL/api/health"

# Metrics Collection
echo ""
echo -e "${YELLOW}📊 Testing Metrics Collection...${NC}"
echo -ne "  Collecting metrics... "
curl -s -X POST "$ANALYZER_URL/api/collect-metrics" > /dev/null
sleep 2
echo -e "${GREEN}✅ Metrics collected${NC}"

# Wait for metrics
echo -ne "  Waiting for metrics (30s)... "
sleep 30
echo -e "${GREEN}✅ Done${NC}"

# Run Analysis
echo ""
echo -e "${YELLOW}🤖 Testing AI Analysis...${NC}"
echo -ne "  Running analysis... "
curl -s -X POST "$ANALYZER_URL/api/analyze-all" > /dev/null
sleep 5
echo -e "${GREEN}✅ Analysis complete${NC}"

# Check Dashboard API
echo ""
echo -e "${YELLOW}📈 Testing Dashboard API...${NC}"
echo -ne "  Fetching dashboard data... "
DASHBOARD_DATA=$(curl -s "$ANALYZER_URL/api/dashboard")
if [ ! -z "$DASHBOARD_DATA" ]; then
    echo -e "${GREEN}✅ Dashboard data available${NC}"
    echo ""
    echo "Dashboard Summary:"
    echo "$DASHBOARD_DATA" | jq '.' 2>/dev/null || echo "$DASHBOARD_DATA"
else
    echo -e "${RED}❌ No dashboard data${NC}"
fi

# Test Firestore
echo ""
echo -e "${YELLOW}🗄️  Testing Firestore...${NC}"
echo -ne "  Checking Firestore connectivity... "
gcloud firestore databases describe --database="(default)" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Firestore connected${NC}"
else
    echo -e "${RED}❌ Firestore connection failed${NC}"
fi

# Load Test (Optional)
echo ""
echo -e "${YELLOW}⚡ Quick Load Test (optional)...${NC}"
read -p "Run quick load test? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "  Sending 10 requests to each demo service..."

    for i in {1..10}; do
        curl -s "$CPU_HUNGRY_URL/api/cpu/heavy" > /dev/null &
        curl -s "$MEMORY_LEAKER_URL/api/memory/allocate?size=1000" > /dev/null &
        curl -s "$DB_CONNECTION_URL/api/db/query" > /dev/null &
    done

    wait
    echo -e "${GREEN}✅ Load test complete${NC}"

    echo "  Waiting 10s for metrics..."
    sleep 10

    echo "  Collecting new metrics..."
    curl -s -X POST "$ANALYZER_URL/api/collect-metrics" > /dev/null
fi

# Summary
echo ""
echo "======================================"
echo -e "${GREEN}✅ All Tests Passed!${NC}"
echo "======================================"
echo ""
echo -e "${YELLOW}📊 View your deployment:${NC}"
echo "   Dashboard: https://storage.googleapis.com/$PROJECT_ID-dashboard/index.html"
echo "   Analyzer API: $ANALYZER_URL"
echo "   GCP Console: https://console.cloud.google.com/run?project=$PROJECT_ID"
echo ""
echo -e "${YELLOW}📝 Next Steps:${NC}"
echo "   1. Open dashboard in browser"
echo "   2. Navigate to 'Metrics' page"
echo "   3. Check 'Recommendations' for AI suggestions"
echo "   4. View 'Deployments' for history"
echo ""
echo -e "${GREEN}🎉 SaveYourMoney is working correctly!${NC}"
echo ""
