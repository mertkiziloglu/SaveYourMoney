#!/bin/bash

# SaveYourMoney - Automated AWS Deployment Script
# Usage: ./deploy-to-aws.sh

set -e

echo "🚀 SaveYourMoney - AWS Deployment"
echo "=================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
export AWS_REGION=${AWS_REGION:-us-east-1}
export STACK_NAME=${STACK_NAME:-saveyourmoney-production}
export ENVIRONMENT=${ENVIRONMENT:-production}

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}📋 Checking prerequisites...${NC}"

    command -v aws >/dev/null 2>&1 || { echo -e "${RED}❌ AWS CLI not found${NC}"; exit 1; }
    command -v docker >/dev/null 2>&1 || { echo -e "${RED}❌ Docker not found${NC}"; exit 1; }
    command -v mvn >/dev/null 2>&1 || { echo -e "${RED}❌ Maven not found${NC}"; exit 1; }

    # Verify AWS credentials
    aws sts get-caller-identity > /dev/null 2>&1 || { echo -e "${RED}❌ AWS credentials not configured${NC}"; exit 1; }

    echo -e "${GREEN}✅ All prerequisites met${NC}"
}

# Get AWS Account ID
get_account_id() {
    export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    echo -e "${GREEN}AWS Account: $AWS_ACCOUNT_ID${NC}"
    echo -e "${GREEN}AWS Region: $AWS_REGION${NC}"
}

# Create ECR repositories
create_ecr_repos() {
    echo ""
    echo -e "${YELLOW}📦 Creating ECR repositories...${NC}"

    SERVICES=("analyzer-service" "code-generator-service" "cpu-hungry-service" "memory-leaker-service" "db-connection-service")

    for service in "${SERVICES[@]}"; do
        echo "  Creating saveyourmoney/$service..."
        aws ecr create-repository \
            --repository-name saveyourmoney/$service \
            --image-scanning-configuration scanOnPush=true \
            --encryption-configuration encryptionType=AES256 \
            --region $AWS_REGION 2>/dev/null || echo "    Repository already exists"
    done

    echo -e "${GREEN}✅ ECR repositories ready${NC}"
}

# Build and push Docker images
build_and_push() {
    echo ""
    echo -e "${YELLOW}🐳 Building and pushing Docker images...${NC}"

    # ECR login
    aws ecr get-login-password --region $AWS_REGION | \
        docker login --username AWS --password-stdin \
        $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

    # Build and push analyzer-service
    echo "  Building analyzer-service..."
    cd analyzer-service
    mvn clean package -DskipTests -q
    docker build -t saveyourmoney/analyzer-service:latest . --quiet
    docker tag saveyourmoney/analyzer-service:latest \
        $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/analyzer-service:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/analyzer-service:latest --quiet
    cd ..

    # Build and push code-generator-service
    echo "  Building code-generator-service..."
    cd code-generator-service
    mvn clean package -DskipTests -q
    docker build -t saveyourmoney/code-generator-service:latest . --quiet
    docker tag saveyourmoney/code-generator-service:latest \
        $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/code-generator-service:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/code-generator-service:latest --quiet
    cd ..

    # Build and push demo services
    for service in cpu-hungry-service memory-leaker-service db-connection-service; do
        echo "  Building $service..."
        cd demo-services/$service
        mvn clean package -DskipTests -q
        docker build -t saveyourmoney/$service:latest . --quiet
        docker tag saveyourmoney/$service:latest \
            $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/$service:latest
        docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/$service:latest --quiet
        cd ../..
    done

    echo -e "${GREEN}✅ All images pushed to ECR${NC}"
}

# Deploy CloudFormation stack
deploy_stack() {
    echo ""
    echo -e "${YELLOW}☁️  Deploying CloudFormation stack...${NC}"

    # Check if stack exists
    if aws cloudformation describe-stacks --stack-name $STACK_NAME --region $AWS_REGION 2>/dev/null; then
        echo "  Updating existing stack..."
        aws cloudformation update-stack \
            --stack-name $STACK_NAME \
            --template-body file://aws-cloudformation/infrastructure.yaml \
            --capabilities CAPABILITY_NAMED_IAM \
            --parameters \
                ParameterKey=Environment,ParameterValue=$ENVIRONMENT \
            --region $AWS_REGION || echo "No updates to be performed"

        echo "  Waiting for stack update to complete..."
        aws cloudformation wait stack-update-complete \
            --stack-name $STACK_NAME \
            --region $AWS_REGION 2>/dev/null || true
    else
        echo "  Creating new stack..."
        aws cloudformation create-stack \
            --stack-name $STACK_NAME \
            --template-body file://aws-cloudformation/infrastructure.yaml \
            --capabilities CAPABILITY_NAMED_IAM \
            --parameters \
                ParameterKey=Environment,ParameterValue=$ENVIRONMENT \
            --region $AWS_REGION

        echo "  Waiting for stack creation to complete (5-10 minutes)..."
        aws cloudformation wait stack-create-complete \
            --stack-name $STACK_NAME \
            --region $AWS_REGION
    fi

    echo -e "${GREEN}✅ CloudFormation stack deployed${NC}"
}

# Update ECS services
update_services() {
    echo ""
    echo -e "${YELLOW}🚀 Updating ECS services...${NC}"

    # Get cluster name
    CLUSTER_NAME=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --query 'Stacks[0].Outputs[?OutputKey==`ECSClusterName`].OutputValue' \
        --output text \
        --region $AWS_REGION)

    # Force new deployment for analyzer-service
    echo "  Updating analyzer-service..."
    aws ecs update-service \
        --cluster $CLUSTER_NAME \
        --service analyzer-service \
        --force-new-deployment \
        --region $AWS_REGION > /dev/null

    # Force new deployment for code-generator-service
    echo "  Updating code-generator-service..."
    aws ecs update-service \
        --cluster $CLUSTER_NAME \
        --service code-generator-service \
        --force-new-deployment \
        --region $AWS_REGION > /dev/null

    echo -e "${GREEN}✅ ECS services updated${NC}"
}

# Wait for services to be stable
wait_for_services() {
    echo ""
    echo -e "${YELLOW}⏳ Waiting for services to stabilize...${NC}"

    CLUSTER_NAME=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --query 'Stacks[0].Outputs[?OutputKey==`ECSClusterName`].OutputValue' \
        --output text \
        --region $AWS_REGION)

    aws ecs wait services-stable \
        --cluster $CLUSTER_NAME \
        --services analyzer-service code-generator-service \
        --region $AWS_REGION

    echo -e "${GREEN}✅ Services are stable${NC}"
}

# Display deployment info
show_deployment_info() {
    echo ""
    echo "=================================="
    echo -e "${GREEN}✅ Deployment Complete!${NC}"
    echo "=================================="
    echo ""

    # Get outputs
    ALB_URL=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
        --output text \
        --region $AWS_REGION)

    CLUSTER_NAME=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --query 'Stacks[0].Outputs[?OutputKey==`ECSClusterName`].OutputValue' \
        --output text \
        --region $AWS_REGION)

    METRICS_TABLE=$(aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --query 'Stacks[0].Outputs[?OutputKey==`MetricsTableName`].OutputValue' \
        --output text \
        --region $AWS_REGION)

    echo -e "${YELLOW}📊 Service URLs:${NC}"
    echo "   Analyzer Service: http://$ALB_URL/api/health"
    echo "   Code Generator:   http://$ALB_URL/codegen/api/health"
    echo "   Swagger UI:       http://$ALB_URL/swagger-ui.html"
    echo ""

    echo -e "${YELLOW}🔧 AWS Resources:${NC}"
    echo "   ECS Cluster:      $CLUSTER_NAME"
    echo "   DynamoDB Table:   $METRICS_TABLE"
    echo "   Region:           $AWS_REGION"
    echo ""

    echo -e "${YELLOW}📝 AWS Console Links:${NC}"
    echo "   ECS Services:     https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/$CLUSTER_NAME/services"
    echo "   CloudWatch:       https://console.aws.amazon.com/cloudwatch/home?region=$AWS_REGION"
    echo "   DynamoDB:         https://console.aws.amazon.com/dynamodb/home?region=$AWS_REGION"
    echo ""

    echo -e "${YELLOW}🧪 Test Commands:${NC}"
    echo "   Health Check:     curl http://$ALB_URL/api/health"
    echo "   Dashboard:        curl http://$ALB_URL/api/dashboard"
    echo ""
}

# Main execution
main() {
    echo ""
    echo "Starting deployment to AWS..."
    echo ""

    check_prerequisites
    get_account_id
    create_ecr_repos
    build_and_push
    deploy_stack
    update_services
    wait_for_services
    show_deployment_info

    echo -e "${GREEN}🎉 SaveYourMoney is now running on AWS!${NC}"
    echo ""
}

# Run main
main
