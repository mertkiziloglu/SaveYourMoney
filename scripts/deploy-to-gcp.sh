#!/bin/bash

# SaveYourMoney - Automated GCP Deployment Script
# Usage: ./deploy-to-gcp.sh

set -e

echo "🚀 SaveYourMoney - GCP Deployment Starting..."
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}📋 Checking prerequisites...${NC}"

    command -v gcloud >/dev/null 2>&1 || { echo -e "${RED}❌ gcloud CLI not found. Install from https://cloud.google.com/sdk${NC}"; exit 1; }
    command -v docker >/dev/null 2>&1 || { echo -e "${RED}❌ Docker not found. Install Docker Desktop${NC}"; exit 1; }
    command -v mvn >/dev/null 2>&1 || { echo -e "${RED}❌ Maven not found. Install Maven${NC}"; exit 1; }
    command -v java >/dev/null 2>&1 || { echo -e "${RED}❌ Java not found. Install Java 17+${NC}"; exit 1; }

    echo -e "${GREEN}✅ All prerequisites installed${NC}"
}

# Get or create GCP project
setup_gcp_project() {
    echo ""
    echo -e "${YELLOW}🔧 Setting up GCP project...${NC}"

    # Get current project or create new
    PROJECT_ID=$(gcloud config get-value project 2>/dev/null)

    if [ -z "$PROJECT_ID" ]; then
        echo "No active project found. Creating new project..."
        PROJECT_ID="saveyourmoney-$(date +%s)"
        gcloud projects create $PROJECT_ID --name="SaveYourMoney"
        gcloud config set project $PROJECT_ID

        echo ""
        echo -e "${YELLOW}⚠️  Please enable billing for this project:${NC}"
        echo "   https://console.cloud.google.com/billing/linkedaccount?project=$PROJECT_ID"
        echo ""
        read -p "Press Enter after enabling billing..."
    fi

    export PROJECT_ID
    export REGION="us-central1"

    echo -e "${GREEN}✅ Project: $PROJECT_ID${NC}"
    echo -e "${GREEN}✅ Region: $REGION${NC}"
}

# Enable required APIs
enable_apis() {
    echo ""
    echo -e "${YELLOW}⚙️  Enabling required APIs (2-3 minutes)...${NC}"

    gcloud services enable \
        run.googleapis.com \
        cloudbuild.googleapis.com \
        containerregistry.googleapis.com \
        firestore.googleapis.com \
        monitoring.googleapis.com \
        logging.googleapis.com \
        storage-api.googleapis.com \
        --quiet

    echo -e "${GREEN}✅ APIs enabled${NC}"
}

# Setup Firestore
setup_firestore() {
    echo ""
    echo -e "${YELLOW}🗄️  Setting up Firestore...${NC}"

    # Create Firestore database if it doesn't exist
    gcloud firestore databases create --location=us-central --quiet 2>/dev/null || echo "Firestore already exists"

    echo -e "${GREEN}✅ Firestore ready${NC}"
}

# Build and deploy a service
deploy_service() {
    local service_name=$1
    local service_port=$2
    local service_dir=$3
    local memory=${4:-1Gi}
    local cpu=${5:-1}

    echo ""
    echo -e "${YELLOW}📦 Deploying $service_name...${NC}"

    cd "$service_dir"

    # Maven build
    echo "  Building with Maven..."
    ./mvnw clean package -DskipTests -q || mvn clean package -DskipTests -q

    # Docker build
    echo "  Building Docker image..."
    docker build -t gcr.io/$PROJECT_ID/$service_name:v1 . --quiet

    # Docker push
    echo "  Pushing to Container Registry..."
    docker push gcr.io/$PROJECT_ID/$service_name:v1 --quiet

    # Cloud Run deploy
    echo "  Deploying to Cloud Run..."
    gcloud run deploy $service_name \
        --image gcr.io/$PROJECT_ID/$service_name:v1 \
        --platform managed \
        --region $REGION \
        --allow-unauthenticated \
        --port $service_port \
        --memory $memory \
        --cpu $cpu \
        --min-instances 0 \
        --max-instances 10 \
        --set-env-vars "SERVER_PORT=$service_port,SPRING_PROFILES_ACTIVE=prod,GOOGLE_CLOUD_PROJECT=$PROJECT_ID" \
        --quiet

    cd - > /dev/null

    echo -e "${GREEN}✅ $service_name deployed${NC}"
}

# Deploy dashboard
deploy_dashboard() {
    echo ""
    echo -e "${YELLOW}🎨 Deploying Dashboard UI...${NC}"

    # Create storage bucket
    gsutil mb -p $PROJECT_ID -c STANDARD -l $REGION gs://$PROJECT_ID-dashboard 2>/dev/null || echo "Bucket already exists"

    # Get service URLs
    ANALYZER_URL=$(gcloud run services describe analyzer-service --platform managed --region $REGION --format 'value(status.url)')
    CODEGEN_URL=$(gcloud run services describe code-generator-service --platform managed --region $REGION --format 'value(status.url)')
    CPU_HUNGRY_URL=$(gcloud run services describe cpu-hungry-service --platform managed --region $REGION --format 'value(status.url)')
    MEMORY_LEAKER_URL=$(gcloud run services describe memory-leaker-service --platform managed --region $REGION --format 'value(status.url)')
    DB_CONNECTION_URL=$(gcloud run services describe db-connection-service --platform managed --region $REGION --format 'value(status.url)')

    # Create config file
    cat > dashboard-ui/public/config/config.js <<EOF
const API_CONFIG = {
  ANALYZER_SERVICE_URL: '$ANALYZER_URL',
  CODE_GENERATOR_URL: '$CODEGEN_URL',
  CPU_HUNGRY_URL: '$CPU_HUNGRY_URL',
  MEMORY_LEAKER_URL: '$MEMORY_LEAKER_URL',
  DB_CONNECTION_URL: '$DB_CONNECTION_URL',
  FIREBASE_PROJECT_ID: '$PROJECT_ID'
};
EOF

    # Upload files
    gsutil -m rsync -r dashboard-ui/public gs://$PROJECT_ID-dashboard/ -q

    # Make public
    gsutil iam ch allUsers:objectViewer gs://$PROJECT_ID-dashboard
    gsutil web set -m index.html -e 404.html gs://$PROJECT_ID-dashboard

    echo -e "${GREEN}✅ Dashboard deployed${NC}"
}

# Main deployment flow
main() {
    echo ""
    echo "🎯 Starting automated deployment..."
    echo ""

    check_prerequisites
    setup_gcp_project
    enable_apis
    setup_firestore

    # Configure Docker for GCP
    echo ""
    echo -e "${YELLOW}🐳 Configuring Docker...${NC}"
    gcloud auth configure-docker --quiet

    # Deploy backend services
    echo ""
    echo -e "${YELLOW}🚀 Deploying Backend Services...${NC}"

    deploy_service "analyzer-service" "8084" "analyzer-service" "1Gi" "1"
    deploy_service "code-generator-service" "8085" "code-generator-service" "1Gi" "1"

    # Deploy demo services
    echo ""
    echo -e "${YELLOW}🚀 Deploying Demo Services...${NC}"

    deploy_service "cpu-hungry-service" "8081" "demo-services/cpu-hungry-service" "512Mi" "0.5"
    deploy_service "memory-leaker-service" "8082" "demo-services/memory-leaker-service" "512Mi" "0.5"
    deploy_service "db-connection-service" "8083" "demo-services/db-connection-service" "512Mi" "0.5"

    # Deploy dashboard
    deploy_dashboard

    # Print summary
    echo ""
    echo "================================================"
    echo -e "${GREEN}✅ Deployment Complete!${NC}"
    echo "================================================"
    echo ""
    echo -e "${YELLOW}📝 Service URLs:${NC}"
    gcloud run services list --platform managed --region $REGION --format="table(metadata.name,status.url)"

    echo ""
    echo -e "${YELLOW}🎨 Dashboard URL:${NC}"
    echo "   https://storage.googleapis.com/$PROJECT_ID-dashboard/index.html"

    echo ""
    echo -e "${YELLOW}📊 GCP Console URLs:${NC}"
    echo "   Cloud Run: https://console.cloud.google.com/run?project=$PROJECT_ID"
    echo "   Firestore: https://console.cloud.google.com/firestore?project=$PROJECT_ID"
    echo "   Monitoring: https://console.cloud.google.com/monitoring?project=$PROJECT_ID"
    echo "   Logs: https://console.cloud.google.com/logs?project=$PROJECT_ID"

    echo ""
    echo -e "${YELLOW}🧪 Test Commands:${NC}"
    ANALYZER_URL=$(gcloud run services describe analyzer-service --platform managed --region $REGION --format 'value(status.url)')
    echo "   Health Check: curl $ANALYZER_URL/api/health"
    echo "   Collect Metrics: curl -X POST $ANALYZER_URL/api/collect-metrics"
    echo "   Run Analysis: curl -X POST $ANALYZER_URL/api/analyze-all"
    echo "   View Dashboard: curl $ANALYZER_URL/api/dashboard | jq"

    echo ""
    echo -e "${GREEN}🎉 SaveYourMoney is now running on GCP!${NC}"
    echo ""
}

# Run main function
main
