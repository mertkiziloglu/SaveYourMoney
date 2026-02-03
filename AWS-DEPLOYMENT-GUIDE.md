# 🚀 Amazon Web Services (AWS) Deployment Guide

**SaveYourMoney** projesini AWS'de deploy etmek için kapsamlı rehber.

---

## 📋 İçindekiler

1. [AWS Servisleri Özeti](#1-aws-servisleri-özeti)
2. [Ön Gereksinimler](#2-ön-gereksinimler)
3. [AWS CLI Kurulumu ve Konfigürasyonu](#3-aws-cli-kurulumu-ve-konfigürasyonu)
4. [ECS Fargate ile Deployment](#4-ecs-fargate-ile-deployment)
5. [CloudFormation ile Infrastructure as Code](#5-cloudformation-ile-infrastructure-as-code)
6. [RDS ve DynamoDB Entegrasyonu](#6-rds-ve-dynamodb-entegrasyonu)
7. [Application Load Balancer Kurulumu](#7-application-load-balancer-kurulumu)
8. [Auto Scaling Configuration](#8-auto-scaling-configuration)
9. [Monitoring ve Logging (CloudWatch)](#9-monitoring-ve-logging-cloudwatch)
10. [CI/CD with CodePipeline](#10-cicd-with-codepipeline)
11. [Maliyet Optimizasyonu](#11-maliyet-optimizasyonu)
12. [Tek Komutla Deployment](#12-tek-komutla-deployment)

---

## 1. AWS Servisleri Özeti

### Kullanılacak AWS Servisleri

| Servis | Kullanım Amacı | Fiyatlandırma |
|--------|----------------|---------------|
| **ECS Fargate** | Container orchestration (Kubernetes alternatifi) | $0.04/vCPU/saat |
| **ECR** | Docker image registry | $0.10/GB/ay |
| **RDS** | PostgreSQL database (H2 yerine) | $0.017/saat (t3.micro) |
| **DynamoDB** | NoSQL database (Firestore alternatifi) | $0.25/GB/ay + $1.25/milyon okuma |
| **ALB** | Application Load Balancer | $0.0225/saat |
| **CloudWatch** | Monitoring ve Logging | $0.50/GB ingested |
| **S3** | Dashboard static hosting | $0.023/GB/ay |
| **CloudFront** | CDN for dashboard | $0.085/GB transfer |
| **CloudFormation** | Infrastructure as Code | Ücretsiz |
| **CodePipeline** | CI/CD automation | $1/pipeline/ay |

**Tahmini Maliyet:** ~$30-50/ay (free tier dahilinde ~$10-20)

---

## 2. Ön Gereksinimler

### AWS Account
- AWS hesabınız var mı? → https://aws.amazon.com/free
- Credit card gerekli (free tier kullanabilirsiniz)

### Bilgisayarınızda Kurulu Olması Gerekenler
```bash
# Kontrol edin:
aws --version          # AWS CLI v2
docker --version       # Docker
java -version          # Java 17+
mvn -version           # Maven 3.8+
```

---

## 3. AWS CLI Kurulumu ve Konfigürasyonu

### 3.1 AWS CLI v2 Kurulumu

#### macOS:
```bash
# Homebrew ile
brew install awscli

# Alternatif: Manuel kurulum
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /
```

#### Windows:
```bash
# MSI installer indir ve çalıştır
https://awscli.amazonaws.com/AWSCLIV2.msi
```

#### Linux:
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

### 3.2 AWS Credentials Konfigürasyonu

```bash
# AWS configure çalıştır
aws configure

# Sorulara cevaplar:
# AWS Access Key ID: AKIAIOSFODNN7EXAMPLE
# AWS Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
# Default region name: us-east-1
# Default output format: json
```

**Access Key nasıl alınır:**
1. AWS Console → IAM → Users → [Your User]
2. Security credentials tab
3. "Create access key" butonu
4. Access Key ID ve Secret Access Key'i kopyalayın

### 3.3 Region Seçimi

```bash
# Önerilen region'lar (latency ve fiyat için):
export AWS_REGION=us-east-1      # En ucuz, Virginia
# export AWS_REGION=eu-central-1 # Frankfurt (Avrupa için)
# export AWS_REGION=ap-southeast-1 # Singapore (Asya için)

aws configure set region $AWS_REGION
```

---

## 4. ECS Fargate ile Deployment

### 4.1 ECS Cluster Oluşturma

```bash
# ECS cluster oluştur (Fargate - serverless)
aws ecs create-cluster \
  --cluster-name saveyourmoney-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE,weight=1,base=1 \
    capacityProvider=FARGATE_SPOT,weight=1

# Cluster'ı doğrula
aws ecs describe-clusters --clusters saveyourmoney-cluster
```

### 4.2 ECR Repository Oluşturma (Docker Registry)

```bash
# Her servis için ECR repository oluştur
SERVICES=("analyzer-service" "code-generator-service" "cpu-hungry-service" "memory-leaker-service" "db-connection-service")

for service in "${SERVICES[@]}"; do
  aws ecr create-repository \
    --repository-name saveyourmoney/$service \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256
done

# Repository listesini görüntüle
aws ecr describe-repositories
```

### 4.3 Docker Image Build & Push

```bash
# ECR'ye login ol
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $(aws sts get-caller-identity --query Account --output text).dkr.ecr.$AWS_REGION.amazonaws.com

# Account ID al
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Servisleri build ve push et
cd /Users/mertkiziloglu/Desktop/SaveYourMoney

# Analyzer Service
cd analyzer-service
mvn clean package -DskipTests
docker build -t saveyourmoney/analyzer-service:latest .
docker tag saveyourmoney/analyzer-service:latest \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/analyzer-service:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/analyzer-service:latest

cd ..

# Code Generator Service
cd code-generator-service
mvn clean package -DskipTests
docker build -t saveyourmoney/code-generator-service:latest .
docker tag saveyourmoney/code-generator-service:latest \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/code-generator-service:latest
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/code-generator-service:latest

cd ..

# Demo Services (tekrar et)
for service in cpu-hungry-service memory-leaker-service db-connection-service; do
  cd demo-services/$service
  mvn clean package -DskipTests
  docker build -t saveyourmoney/$service:latest .
  docker tag saveyourmoney/$service:latest \
    $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/$service:latest
  docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/saveyourmoney/$service:latest
  cd ../..
done
```

### 4.4 ECS Task Definition Oluşturma

**analyzer-service-task.json:**
```json
{
  "family": "analyzer-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::${AWS_ACCOUNT_ID}:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::${AWS_ACCOUNT_ID}:role/ecsTaskRole",
  "containerDefinitions": [
    {
      "name": "analyzer-service",
      "image": "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/saveyourmoney/analyzer-service:latest",
      "cpu": 1024,
      "memory": 2048,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8084,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "SERVER_PORT",
          "value": "8084"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/analyzer-service",
          "awslogs-region": "${AWS_REGION}",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8084/api/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

```bash
# Task definition oluştur
aws ecs register-task-definition \
  --cli-input-json file://aws-cloudformation/task-definitions/analyzer-service-task.json
```

### 4.5 ECS Service Oluşturma

```bash
# VPC ve Subnet bilgilerini al
export VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query "Vpcs[0].VpcId" --output text)
export SUBNET_1=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[0].SubnetId" --output text)
export SUBNET_2=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query "Subnets[1].SubnetId" --output text)

# Security Group oluştur
export SG_ID=$(aws ec2 create-security-group \
  --group-name saveyourmoney-sg \
  --description "Security group for SaveYourMoney services" \
  --vpc-id $VPC_ID \
  --query 'GroupId' --output text)

# Inbound rules ekle (HTTP traffic)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8084 \
  --cidr 0.0.0.0/0

# ECS Service oluştur
aws ecs create-service \
  --cluster saveyourmoney-cluster \
  --service-name analyzer-service \
  --task-definition analyzer-service \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_1,$SUBNET_2],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
  --enable-execute-command
```

---

## 5. CloudFormation ile Infrastructure as Code

### 5.1 CloudFormation Template (infrastructure.yaml)

**Tam CloudFormation template:**

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: 'SaveYourMoney - Complete Infrastructure Stack'

Parameters:
  Environment:
    Type: String
    Default: production
    AllowedValues:
      - development
      - staging
      - production
    Description: Deployment environment

  VpcCIDR:
    Type: String
    Default: 10.0.0.0/16
    Description: CIDR block for VPC

Resources:
  # VPC
  VPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: !Ref VpcCIDR
      EnableDnsHostnames: true
      EnableDnsSupport: true
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-VPC

  # Internet Gateway
  InternetGateway:
    Type: AWS::EC2::InternetGateway
    Properties:
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-IGW

  AttachGateway:
    Type: AWS::EC2::VPCGatewayAttachment
    Properties:
      VpcId: !Ref VPC
      InternetGatewayId: !Ref InternetGateway

  # Public Subnets
  PublicSubnet1:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref VPC
      CidrBlock: 10.0.1.0/24
      AvailabilityZone: !Select [0, !GetAZs '']
      MapPublicIpOnLaunch: true
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-Public-1

  PublicSubnet2:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref VPC
      CidrBlock: 10.0.2.0/24
      AvailabilityZone: !Select [1, !GetAZs '']
      MapPublicIpOnLaunch: true
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-Public-2

  # Route Table
  PublicRouteTable:
    Type: AWS::EC2::RouteTable
    Properties:
      VpcId: !Ref VPC
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-Public-RT

  PublicRoute:
    Type: AWS::EC2::Route
    DependsOn: AttachGateway
    Properties:
      RouteTableId: !Ref PublicRouteTable
      DestinationCidrBlock: 0.0.0.0/0
      GatewayId: !Ref InternetGateway

  SubnetRouteTableAssociation1:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      SubnetId: !Ref PublicSubnet1
      RouteTableId: !Ref PublicRouteTable

  SubnetRouteTableAssociation2:
    Type: AWS::EC2::SubnetRouteTableAssociation
    Properties:
      SubnetId: !Ref PublicSubnet2
      RouteTableId: !Ref PublicRouteTable

  # Security Groups
  ECSSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Security group for ECS tasks
      VpcId: !Ref VPC
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 8084
          ToPort: 8084
          SourceSecurityGroupId: !Ref ALBSecurityGroup
        - IpProtocol: tcp
          FromPort: 8085
          ToPort: 8085
          SourceSecurityGroupId: !Ref ALBSecurityGroup
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-ECS-SG

  ALBSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Security group for Application Load Balancer
      VpcId: !Ref VPC
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 80
          ToPort: 80
          CidrIp: 0.0.0.0/0
        - IpProtocol: tcp
          FromPort: 443
          ToPort: 443
          CidrIp: 0.0.0.0/0
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-ALB-SG

  # Application Load Balancer
  ApplicationLoadBalancer:
    Type: AWS::ElasticLoadBalancingV2::LoadBalancer
    Properties:
      Name: !Sub ${AWS::StackName}-ALB
      Subnets:
        - !Ref PublicSubnet1
        - !Ref PublicSubnet2
      SecurityGroups:
        - !Ref ALBSecurityGroup
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-ALB

  # Target Groups
  AnalyzerTargetGroup:
    Type: AWS::ElasticLoadBalancingV2::TargetGroup
    Properties:
      Name: analyzer-service-tg
      Port: 8084
      Protocol: HTTP
      VpcId: !Ref VPC
      TargetType: ip
      HealthCheckPath: /api/health
      HealthCheckIntervalSeconds: 30
      HealthCheckTimeoutSeconds: 5
      HealthyThresholdCount: 2
      UnhealthyThresholdCount: 3

  CodeGenTargetGroup:
    Type: AWS::ElasticLoadBalancingV2::TargetGroup
    Properties:
      Name: codegen-service-tg
      Port: 8085
      Protocol: HTTP
      VpcId: !Ref VPC
      TargetType: ip
      HealthCheckPath: /api/health
      HealthCheckIntervalSeconds: 30
      HealthCheckTimeoutSeconds: 5
      HealthyThresholdCount: 2
      UnhealthyThresholdCount: 3

  # ALB Listeners
  ALBListener:
    Type: AWS::ElasticLoadBalancingV2::Listener
    Properties:
      LoadBalancerArn: !Ref ApplicationLoadBalancer
      Port: 80
      Protocol: HTTP
      DefaultActions:
        - Type: forward
          TargetGroupArn: !Ref AnalyzerTargetGroup

  # Listener Rules
  CodeGenListenerRule:
    Type: AWS::ElasticLoadBalancingV2::ListenerRule
    Properties:
      ListenerArn: !Ref ALBListener
      Priority: 1
      Conditions:
        - Field: path-pattern
          Values:
            - /codegen/*
      Actions:
        - Type: forward
          TargetGroupArn: !Ref CodeGenTargetGroup

  # ECS Cluster
  ECSCluster:
    Type: AWS::ECS::Cluster
    Properties:
      ClusterName: !Sub ${AWS::StackName}-cluster
      CapacityProviders:
        - FARGATE
        - FARGATE_SPOT
      DefaultCapacityProviderStrategy:
        - CapacityProvider: FARGATE
          Weight: 1
          Base: 1
        - CapacityProvider: FARGATE_SPOT
          Weight: 1

  # CloudWatch Log Groups
  AnalyzerLogGroup:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: /ecs/analyzer-service
      RetentionInDays: 7

  CodeGenLogGroup:
    Type: AWS::Logs::LogGroup
    Properties:
      LogGroupName: /ecs/code-generator-service
      RetentionInDays: 7

  # IAM Roles
  ECSTaskExecutionRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service: ecs-tasks.amazonaws.com
            Action: sts:AssumeRole
      ManagedPolicyArns:
        - arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
      Policies:
        - PolicyName: ECRAccess
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  - ecr:GetAuthorizationToken
                  - ecr:BatchCheckLayerAvailability
                  - ecr:GetDownloadUrlForLayer
                  - ecr:BatchGetImage
                Resource: '*'

  ECSTaskRole:
    Type: AWS::IAM::Role
    Properties:
      AssumeRolePolicyDocument:
        Statement:
          - Effect: Allow
            Principal:
              Service: ecs-tasks.amazonaws.com
            Action: sts:AssumeRole
      Policies:
        - PolicyName: DynamoDBAccess
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  - dynamodb:*
                Resource: !GetAtt MetricsTable.Arn
        - PolicyName: CloudWatchAccess
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  - cloudwatch:PutMetricData
                  - logs:CreateLogStream
                  - logs:PutLogEvents
                Resource: '*'

  # DynamoDB Tables
  MetricsTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: !Sub ${AWS::StackName}-metrics
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: serviceName
          AttributeType: S
        - AttributeName: timestamp
          AttributeType: N
      KeySchema:
        - AttributeName: serviceName
          KeyType: HASH
        - AttributeName: timestamp
          KeyType: RANGE
      StreamSpecification:
        StreamViewType: NEW_AND_OLD_IMAGES
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-metrics

  RecommendationsTable:
    Type: AWS::DynamoDB::Table
    Properties:
      TableName: !Sub ${AWS::StackName}-recommendations
      BillingMode: PAY_PER_REQUEST
      AttributeDefinitions:
        - AttributeName: id
          AttributeType: S
        - AttributeName: serviceName
          AttributeType: S
      KeySchema:
        - AttributeName: id
          KeyType: HASH
      GlobalSecondaryIndexes:
        - IndexName: ServiceNameIndex
          KeySchema:
            - AttributeName: serviceName
              KeyType: HASH
          Projection:
            ProjectionType: ALL
      Tags:
        - Key: Name
          Value: !Sub ${AWS::StackName}-recommendations

  # ECS Task Definitions
  AnalyzerTaskDefinition:
    Type: AWS::ECS::TaskDefinition
    Properties:
      Family: analyzer-service
      NetworkMode: awsvpc
      RequiresCompatibilities:
        - FARGATE
      Cpu: '1024'
      Memory: '2048'
      ExecutionRoleArn: !GetAtt ECSTaskExecutionRole.Arn
      TaskRoleArn: !GetAtt ECSTaskRole.Arn
      ContainerDefinitions:
        - Name: analyzer-service
          Image: !Sub ${AWS::AccountId}.dkr.ecr.${AWS::Region}.amazonaws.com/saveyourmoney/analyzer-service:latest
          Essential: true
          PortMappings:
            - ContainerPort: 8084
              Protocol: tcp
          Environment:
            - Name: SPRING_PROFILES_ACTIVE
              Value: prod
            - Name: SERVER_PORT
              Value: '8084'
            - Name: AWS_REGION
              Value: !Ref AWS::Region
            - Name: DYNAMODB_TABLE_METRICS
              Value: !Ref MetricsTable
          LogConfiguration:
            LogDriver: awslogs
            Options:
              awslogs-group: !Ref AnalyzerLogGroup
              awslogs-region: !Ref AWS::Region
              awslogs-stream-prefix: ecs
          HealthCheck:
            Command:
              - CMD-SHELL
              - curl -f http://localhost:8084/api/health || exit 1
            Interval: 30
            Timeout: 5
            Retries: 3
            StartPeriod: 60

  # ECS Services
  AnalyzerService:
    Type: AWS::ECS::Service
    DependsOn: ALBListener
    Properties:
      ServiceName: analyzer-service
      Cluster: !Ref ECSCluster
      TaskDefinition: !Ref AnalyzerTaskDefinition
      DesiredCount: 2
      LaunchType: FARGATE
      NetworkConfiguration:
        AwsvpcConfiguration:
          AssignPublicIp: ENABLED
          Subnets:
            - !Ref PublicSubnet1
            - !Ref PublicSubnet2
          SecurityGroups:
            - !Ref ECSSecurityGroup
      LoadBalancers:
        - ContainerName: analyzer-service
          ContainerPort: 8084
          TargetGroupArn: !Ref AnalyzerTargetGroup

  # Auto Scaling
  AnalyzerAutoScalingTarget:
    Type: AWS::ApplicationAutoScaling::ScalableTarget
    Properties:
      MaxCapacity: 10
      MinCapacity: 2
      ResourceId: !Sub service/${ECSCluster}/${AnalyzerService.Name}
      RoleARN: !Sub arn:aws:iam::${AWS::AccountId}:role/aws-service-role/ecs.application-autoscaling.amazonaws.com/AWSServiceRoleForApplicationAutoScaling_ECSService
      ScalableDimension: ecs:service:DesiredCount
      ServiceNamespace: ecs

  AnalyzerScalingPolicy:
    Type: AWS::ApplicationAutoScaling::ScalingPolicy
    Properties:
      PolicyName: AnalyzerCPUScalingPolicy
      PolicyType: TargetTrackingScaling
      ScalingTargetId: !Ref AnalyzerAutoScalingTarget
      TargetTrackingScalingPolicyConfiguration:
        PredefinedMetricSpecification:
          PredefinedMetricType: ECSServiceAverageCPUUtilization
        TargetValue: 70.0
        ScaleInCooldown: 300
        ScaleOutCooldown: 60

Outputs:
  LoadBalancerURL:
    Description: URL of the Application Load Balancer
    Value: !GetAtt ApplicationLoadBalancer.DNSName
    Export:
      Name: !Sub ${AWS::StackName}-ALB-URL

  AnalyzerServiceURL:
    Description: Analyzer Service URL
    Value: !Sub http://${ApplicationLoadBalancer.DNSName}/api/health

  ECSClusterName:
    Description: ECS Cluster Name
    Value: !Ref ECSCluster

  MetricsTableName:
    Description: DynamoDB Metrics Table
    Value: !Ref MetricsTable

  RecommendationsTableName:
    Description: DynamoDB Recommendations Table
    Value: !Ref RecommendationsTable
```

### 5.2 CloudFormation Stack Oluşturma

```bash
# Stack oluştur
aws cloudformation create-stack \
  --stack-name saveyourmoney-production \
  --template-body file://aws-cloudformation/infrastructure.yaml \
  --capabilities CAPABILITY_IAM \
  --parameters \
    ParameterKey=Environment,ParameterValue=production

# Stack oluşturma durumunu izle
aws cloudformation wait stack-create-complete \
  --stack-name saveyourmoney-production

# Stack outputs'u görüntüle
aws cloudformation describe-stacks \
  --stack-name saveyourmoney-production \
  --query 'Stacks[0].Outputs'
```

---

## 6. RDS ve DynamoDB Entegrasyonu

### 6.1 DynamoDB (Firestore Alternatifi)

**Spring Boot application-prod.yml güncellemesi:**
```yaml
spring:
  cloud:
    aws:
      region:
        static: ${AWS_REGION:us-east-1}
      credentials:
        instanceProfile: true

# DynamoDB configuration
aws:
  dynamodb:
    endpoint: https://dynamodb.${AWS_REGION}.amazonaws.com
    table:
      metrics: saveyourmoney-metrics
      recommendations: saveyourmoney-recommendations
```

### 6.2 RDS PostgreSQL (H2 Alternatifi)

```bash
# RDS instance oluştur
aws rds create-db-instance \
  --db-instance-identifier saveyourmoney-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username admin \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --vpc-security-group-ids $SG_ID \
  --db-subnet-group-name default \
  --backup-retention-period 7 \
  --multi-az false \
  --publicly-accessible false

# RDS endpoint al
aws rds describe-db-instances \
  --db-instance-identifier saveyourmoney-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

---

## 7. Application Load Balancer Kurulumu

Load Balancer CloudFormation template'de zaten var. Manuel kurulum:

```bash
# ALB oluştur
aws elbv2 create-load-balancer \
  --name saveyourmoney-alb \
  --subnets $SUBNET_1 $SUBNET_2 \
  --security-groups $ALB_SG_ID \
  --scheme internet-facing \
  --type application

# Target group oluştur
aws elbv2 create-target-group \
  --name analyzer-tg \
  --protocol HTTP \
  --port 8084 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /api/health

# Listener oluştur
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$TG_ARN
```

---

## 8. Auto Scaling Configuration

Auto scaling CloudFormation template'de mevcut. Manuel test:

```bash
# Auto scaling hedef CPU 70%
# Min instances: 2
# Max instances: 10

# Load test ile test edin:
ab -n 10000 -c 100 http://ALB-URL/api/health
```

---

## 9. Monitoring ve Logging (CloudWatch)

### 9.1 CloudWatch Dashboard

```bash
# Dashboard oluştur
aws cloudwatch put-dashboard \
  --dashboard-name SaveYourMoney \
  --dashboard-body file://aws-cloudformation/cloudwatch-dashboard.json
```

**cloudwatch-dashboard.json:**
```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ECS", "CPUUtilization", {"stat": "Average"}],
          [".", "MemoryUtilization", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "ECS Performance"
      }
    }
  ]
}
```

### 9.2 CloudWatch Alarms

```bash
# CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name analyzer-service-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

---

## 10. CI/CD with CodePipeline

**CodePipeline CloudFormation:**

```yaml
# aws-cloudformation/cicd-pipeline.yaml
Resources:
  CodePipeline:
    Type: AWS::CodePipeline::Pipeline
    Properties:
      Name: SaveYourMoney-Pipeline
      RoleArn: !GetAtt CodePipelineRole.Arn
      ArtifactStore:
        Type: S3
        Location: !Ref PipelineBucket
      Stages:
        - Name: Source
          Actions:
            - Name: SourceAction
              ActionTypeId:
                Category: Source
                Owner: ThirdParty
                Provider: GitHub
                Version: 1
              Configuration:
                Owner: YOUR-GITHUB-USERNAME
                Repo: SaveYourMoney
                Branch: main
                OAuthToken: !Ref GitHubToken
              OutputArtifacts:
                - Name: SourceOutput

        - Name: Build
          Actions:
            - Name: BuildAction
              ActionTypeId:
                Category: Build
                Owner: AWS
                Provider: CodeBuild
                Version: 1
              InputArtifacts:
                - Name: SourceOutput
              OutputArtifacts:
                - Name: BuildOutput
              Configuration:
                ProjectName: !Ref CodeBuildProject

        - Name: Deploy
          Actions:
            - Name: DeployAction
              ActionTypeId:
                Category: Deploy
                Owner: AWS
                Provider: ECS
                Version: 1
              InputArtifacts:
                - Name: BuildOutput
              Configuration:
                ClusterName: !Ref ECSCluster
                ServiceName: analyzer-service
```

---

## 11. Maliyet Optimizasyonu

### 11.1 Fargate Spot Kullanımı

```bash
# Capacity provider'ları güncelle (daha ucuz)
aws ecs put-cluster-capacity-providers \
  --cluster saveyourmoney-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE_SPOT,weight=4,base=0 \
    capacityProvider=FARGATE,weight=1,base=1
```

### 11.2 Maliyet Tahmini

| Kaynak | Fiyat | Aylık Maliyet |
|--------|-------|---------------|
| **ECS Fargate** (2 tasks, 1vCPU, 2GB) | $0.04/vCPU/hr | ~$60 |
| **ALB** | $0.0225/hr | $16.20 |
| **DynamoDB** (1GB, 1M RRU) | $0.25/GB | $1.25 |
| **ECR** (10GB storage) | $0.10/GB | $1.00 |
| **CloudWatch Logs** (5GB) | $0.50/GB | $2.50 |
| **S3** (Dashboard, 1GB) | $0.023/GB | $0.02 |
| **Data Transfer** (10GB out) | $0.09/GB | $0.90 |
| **TOPLAM** | | **~$82/ay** |

**Free Tier ile:** ~$20-30/ay

---

## 12. Tek Komutla Deployment

**deploy-to-aws.sh:**

```bash
#!/bin/bash
set -e

echo "🚀 SaveYourMoney AWS Deployment"
echo "================================"

export AWS_REGION=${AWS_REGION:-us-east-1}
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# 1. CloudFormation stack oluştur
echo "📦 Creating CloudFormation stack..."
aws cloudformation create-stack \
  --stack-name saveyourmoney-prod \
  --template-body file://aws-cloudformation/infrastructure.yaml \
  --capabilities CAPABILITY_IAM

aws cloudformation wait stack-create-complete \
  --stack-name saveyourmoney-prod

# 2. Docker images build & push
echo "🐳 Building and pushing Docker images..."
./scripts/build-and-push-aws.sh

# 3. ECS services güncelle
echo "🚀 Deploying to ECS..."
aws ecs update-service \
  --cluster saveyourmoney-cluster \
  --service analyzer-service \
  --force-new-deployment

echo "✅ Deployment Complete!"
echo ""
echo "🌐 Load Balancer URL:"
aws cloudformation describe-stacks \
  --stack-name saveyourmoney-prod \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerURL`].OutputValue' \
  --output text
```

---

## 13. Cleanup (Kaynakları Silme)

```bash
# Tüm stack'i sil
aws cloudformation delete-stack --stack-name saveyourmoney-prod

# ECR images sil
aws ecr batch-delete-image \
  --repository-name saveyourmoney/analyzer-service \
  --image-ids imageTag=latest
```

---

## 📊 AWS vs GCP Karşılaştırma

| Özellik | AWS | GCP |
|---------|-----|-----|
| **Container Service** | ECS Fargate | Cloud Run |
| **Load Balancer** | ALB | Cloud Load Balancer |
| **NoSQL DB** | DynamoDB | Firestore |
| **Registry** | ECR | Container Registry |
| **IaC** | CloudFormation | Deployment Manager |
| **Monitoring** | CloudWatch | Cloud Monitoring |
| **Maliyet (aylık)** | ~$80 | ~$50 |
| **Free Tier** | 12 ay | Free quota |

---

## 🎉 Tamamlandı!

Projeniz artık AWS'de çalışıyor!

**Erişim:**
```
http://YOUR-ALB-URL/api/health
```

**Monitoring:**
```
https://console.aws.amazon.com/cloudwatch
https://console.aws.amazon.com/ecs
```

---

**Hazırlayan:** Claude Sonnet 4.5
**Tarih:** 2026-02-03
**Versiyon:** 1.0

☁️ **Built for AWS. Optimized for savings.** 💰
