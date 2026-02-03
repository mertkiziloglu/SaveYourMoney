#!/bin/bash

# Azure DevOps to GCP Cloud Run Deployment Script
# This script deploys SaveYourMoney services from Azure Pipelines to GCP Cloud Run

set -e

echo "=========================================="
echo "🚀 Azure DevOps -> GCP Cloud Run Deploy"
echo "=========================================="
echo ""

# Environment variables (set by Azure Pipeline)
PROJECT_ID="${GCP_PROJECT_ID:-saveyourmoney-prod}"
REGION="${GCP_REGION:-us-central1}"
BUILD_ID="${BUILD_ID:-latest}"

echo "📋 Configuration:"
echo "  GCP Project: $PROJECT_ID"
echo "  Region: $REGION"
echo "  Build ID: $BUILD_ID"
echo ""

# Install gcloud CLI if not available
if ! command -v gcloud &> /dev/null; then
    echo "📥 Installing Google Cloud SDK..."
    curl https://sdk.cloud.google.com | bash
    exec -l $SHELL
    source $HOME/google-cloud-sdk/path.bash.inc
fi

# Authenticate with GCP using service account
echo "🔐 Authenticating with GCP..."
echo "$GCP_SERVICE_ACCOUNT_KEY" | base64 -d > ${HOME}/gcp-key.json
gcloud auth activate-service-account --key-file ${HOME}/gcp-key.json
gcloud config set project $PROJECT_ID
gcloud config set compute/region $REGION

echo "✅ Authentication successful!"
echo ""

# Configure Docker to use gcloud as credential helper
gcloud auth configure-docker gcr.io --quiet

echo "🐳 Pushing Docker images to GCR..."

# Push all Docker images
docker push gcr.io/$PROJECT_ID/analyzer-service:$BUILD_ID
docker push gcr.io/$PROJECT_ID/analyzer-service:latest

docker push gcr.io/$PROJECT_ID/code-generator-service:$BUILD_ID
docker push gcr.io/$PROJECT_ID/code-generator-service:latest

docker push gcr.io/$PROJECT_ID/cpu-hungry-service:$BUILD_ID
docker push gcr.io/$PROJECT_ID/cpu-hungry-service:latest

docker push gcr.io/$PROJECT_ID/memory-leaker-service:$BUILD_ID
docker push gcr.io/$PROJECT_ID/memory-leaker-service:latest

docker push gcr.io/$PROJECT_ID/db-connection-service:$BUILD_ID
docker push gcr.io/$PROJECT_ID/db-connection-service:latest

echo "✅ Docker images pushed successfully!"
echo ""

# Define services with their configurations
# Format: "service-name:port:memory:cpu:max-instances"
declare -a SERVICES=(
    "analyzer-service:8084:1Gi:1:10"
    "code-generator-service:8085:1Gi:1:10"
    "cpu-hungry-service:8081:512Mi:1:5"
    "memory-leaker-service:8082:512Mi:1:5"
    "db-connection-service:8083:512Mi:1:5"
)

echo "🚀 Deploying services to Cloud Run..."
echo ""

# Deploy each service
for service_config in "${SERVICES[@]}"; do
    IFS=':' read -r service port memory cpu max_instances <<< "$service_config"

    echo "──────────────────────────────────────"
    echo "📦 Deploying: $service"
    echo "──────────────────────────────────────"

    gcloud run deploy $service \
        --image gcr.io/$PROJECT_ID/$service:$BUILD_ID \
        --region $REGION \
        --platform managed \
        --allow-unauthenticated \
        --port $port \
        --memory $memory \
        --cpu $cpu \
        --min-instances 0 \
        --max-instances $max_instances \
        --set-env-vars "SERVER_PORT=$port,SPRING_PROFILES_ACTIVE=prod,GOOGLE_CLOUD_PROJECT=$PROJECT_ID" \
        --quiet

    # Get the service URL
    SERVICE_URL=$(gcloud run services describe $service \
        --platform managed \
        --region $REGION \
        --format 'value(status.url)')

    echo "✅ $service deployed successfully!"
    echo "   URL: $SERVICE_URL"
    echo ""
done

echo "=========================================="
echo "✅ All services deployed successfully!"
echo "=========================================="
echo ""

# List all deployed services
echo "📝 Deployed Services:"
gcloud run services list \
    --platform managed \
    --region $REGION \
    --format="table(metadata.name,status.url,status.latestReadyRevisionName)"

echo ""
echo "🎉 Deployment completed!"
echo ""

# Health check
echo "🏥 Running health checks..."
echo ""

for service_config in "${SERVICES[@]}"; do
    IFS=':' read -r service port memory cpu max_instances <<< "$service_config"

    SERVICE_URL=$(gcloud run services describe $service \
        --platform managed \
        --region $REGION \
        --format 'value(status.url)')

    echo "Checking $service..."

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $SERVICE_URL/api/health || echo "000")

    if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "404" ]; then
        echo "  ✅ $service is responding (HTTP $HTTP_STATUS)"
    else
        echo "  ⚠️  $service returned HTTP $HTTP_STATUS (may still be starting)"
    fi
done

echo ""
echo "=========================================="
echo "📊 Deployment Summary"
echo "=========================================="
echo "Build ID: $BUILD_ID"
echo "Region: $REGION"
echo "Project: $PROJECT_ID"
echo "Services Deployed: ${#SERVICES[@]}"
echo ""
echo "🎯 Next Steps:"
echo "  1. Check service logs: gcloud run services logs tail analyzer-service --region=$REGION"
echo "  2. View in console: https://console.cloud.google.com/run?project=$PROJECT_ID"
echo "  3. Test dashboard: Update dashboard config with new service URLs"
echo ""

# Cleanup
rm -f ${HOME}/gcp-key.json

echo "✨ Deployment script completed!"
