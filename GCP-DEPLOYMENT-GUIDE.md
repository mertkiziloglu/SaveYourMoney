# 🚀 Google Cloud Platform (GCP) Deployment Guide

**SaveYourMoney** projesini Google Cloud Platform'da deploy etmek için kapsamlı rehber.

---

## 📋 İçindekiler

1. [GCP Console ile UI Üzerinden Kurulum](#1-gcp-console-ile-ui-üzerinden-kurulum)
2. [VS Code + GitHub Copilot ile Hızlı Deployment](#2-vs-code--github-copilot-ile-hızlı-deployment)
3. [Cloud Run ile Deployment](#3-cloud-run-ile-deployment)
4. [Google Kubernetes Engine (GKE) ile Deployment](#4-google-kubernetes-engine-gke-ile-deployment)
5. [Firestore Veritabanı Kurulumu](#5-firestore-veritabanı-kurulumu)
6. [Monitoring ve Logging](#6-monitoring-ve-logging)
7. [Maliyet Optimizasyonu](#7-maliyet-optimizasyonu)

---

## 1. GCP Console ile UI Üzerinden Kurulum

### 1.1 Proje Oluşturma

1. **Google Cloud Console'a gidin**: https://console.cloud.google.com
2. Üst menüden **"Select a project"** → **"New Project"** tıklayın
3. Proje bilgilerini doldurun:
   - **Project name**: `saveyourmoney-prod`
   - **Organization**: (Varsa seçin)
   - **Location**: (Varsa seçin)
4. **"CREATE"** butonuna tıklayın
5. Proje oluşturulduktan sonra üst menüden projeyi seçin

### 1.2 Gerekli API'leri Etkinleştirme

1. **Navigation Menu** (☰) → **APIs & Services** → **Library**
2. Aşağıdaki API'leri arayıp **"ENABLE"** butonuna tıklayın:
   - ✅ **Cloud Run API**
   - ✅ **Cloud Build API**
   - ✅ **Container Registry API** (veya Artifact Registry)
   - ✅ **Cloud Firestore API**
   - ✅ **Cloud Logging API**
   - ✅ **Cloud Monitoring API**
   - ✅ **Kubernetes Engine API** (GKE kullanacaksanız)

### 1.3 Billing Hesabı Bağlama

1. **Navigation Menu** → **Billing**
2. Billing hesabınızı seçin veya yeni oluşturun
3. Projeye billing hesabını bağlayın

---

## 2. VS Code + GitHub Copilot ile Hızlı Deployment

### 2.1 VS Code Extensions Kurulumu

VS Code'da aşağıdaki extension'ları yükleyin:

```
1. Google Cloud Code (Google tarafından)
2. GitHub Copilot
3. GitHub Copilot Chat
4. Docker
5. Kubernetes
6. Cloud Code - YAML Editing
```

**Kurulum Adımları:**
1. VS Code'u açın
2. Extensions panel (Cmd+Shift+X / Ctrl+Shift+X)
3. Yukarıdaki extension'ları arayıp **"Install"** butonuna tıklayın

### 2.2 Google Cloud SDK (gcloud) Kurulumu

#### macOS için:
```bash
# Homebrew ile kurulum
brew install --cask google-cloud-sdk

# Alternatif: Manuel kurulum
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
```

#### Windows için:
1. https://cloud.google.com/sdk/docs/install adresine gidin
2. **Google Cloud CLI installer** indirilir
3. İndirilen dosyayı çalıştırın ve kurulum sihirbazını takip edin

#### Linux için:
```bash
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
```

### 2.3 GCP Authentication (VS Code Terminal'den)

```bash
# GCP'ye login olun
gcloud auth login

# Projenizi ayarlayın
gcloud config set project saveyourmoney-prod

# Bölgenizi ayarlayın (örnek: us-central1)
gcloud config set compute/region us-central1
gcloud config set compute/zone us-central1-a

# Application Default Credentials ayarlayın
gcloud auth application-default login

# Docker için authentication
gcloud auth configure-docker
```

### 2.4 VS Code'da Cloud Code Kullanımı

1. **Command Palette** açın (Cmd+Shift+P / Ctrl+Shift+P)
2. **"Cloud Code: Sign In"** yazın ve Enter'a basın
3. Browser'da açılan pencerede Google hesabınızla giriş yapın
4. **Command Palette** → **"Cloud Code: Select Project"**
5. `saveyourmoney-prod` projesini seçin

### 2.5 GitHub Copilot Agent Mode ile GCP Deployment

VS Code terminal'de Copilot Chat'i kullanarak:

```
@terminal gcloud projemi oluştur ve Cloud Run için hazırla
@terminal 6 Spring Boot servisimi containerize et ve Cloud Run'a deploy et
@terminal GCP'de Firestore veritabanı oluştur ve connection string al
@terminal Cloud Monitoring için dashboard oluştur
```

**Örnek Workflow:**

```bash
# Terminal'i açın (Ctrl+` veya Cmd+`)
# GitHub Copilot Chat'e şunu yazın:

# 1. Docker image'larını build et
@terminal tüm servisleri Docker image olarak build et ve GCP Container Registry'ye push et

# 2. Cloud Run'a deploy et
@terminal analyzer-service'i Cloud Run'a deploy et, port 8084

# 3. Environment variables ayarla
@terminal Cloud Run servislerine environment variable ekle
```

---

## 3. Cloud Run ile Deployment

Cloud Run, serverless container çalıştırma platformudur. Kubernetes cluster yönetmek istemiyorsanız en iyi seçenektir.

### 3.1 Dockerfile Hazırlama

Her servis için Dockerfile zaten var. Örnek:

```dockerfile
# analyzer-service/Dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 Cloud Run Deployment (UI Üzerinden)

#### Adım 1: Servisleri Build Edin
```bash
# SaveYourMoney dizininde
cd analyzer-service
./mvnw clean package -DskipTests

cd ../code-generator-service
./mvnw clean package -DskipTests

cd ../cpu-hungry-service
./mvnw clean package -DskipTests

cd ../memory-leaker-service
./mvnw clean package -DskipTests

cd ../db-connection-service
./mvnw clean package -DskipTests
```

#### Adım 2: Docker Images Build & Push
```bash
# GCP Project ID'nizi alın
export PROJECT_ID=$(gcloud config get-value project)

# Her servis için Docker image build ve push
cd analyzer-service
docker build -t gcr.io/$PROJECT_ID/analyzer-service:v1 .
docker push gcr.io/$PROJECT_ID/analyzer-service:v1

cd ../code-generator-service
docker build -t gcr.io/$PROJECT_ID/code-generator-service:v1 .
docker push gcr.io/$PROJECT_ID/code-generator-service:v1

cd ../cpu-hungry-service
docker build -t gcr.io/$PROJECT_ID/cpu-hungry-service:v1 .
docker push gcr.io/$PROJECT_ID/cpu-hungry-service:v1

cd ../memory-leaker-service
docker build -t gcr.io/$PROJECT_ID/memory-leaker-service:v1 .
docker push gcr.io/$PROJECT_ID/memory-leaker-service:v1

cd ../db-connection-service
docker build -t gcr.io/$PROJECT_ID/db-connection-service:v1 .
docker push gcr.io/$PROJECT_ID/db-connection-service:v1
```

#### Adım 3: Cloud Run'da Servis Oluşturma (UI)

1. **GCP Console** → **Cloud Run**
2. **"CREATE SERVICE"** butonuna tıklayın
3. Servis ayarlarını doldurun:

**Analyzer Service için:**
- **Container image URL**: `gcr.io/saveyourmoney-prod/analyzer-service:v1`
- **Service name**: `analyzer-service`
- **Region**: `us-central1` (veya size en yakın)
- **Authentication**: ✅ Allow unauthenticated invocations (geliştirme için)
- **Container port**: `8084`
- **Memory**: `1 GiB`
- **CPU**: `1 vCPU`
- **Min instances**: `0`
- **Max instances**: `10`

**Environment Variables:**
```
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8084
```

4. **"CREATE"** butonuna tıklayın
5. Deploy tamamlandığında servis URL'ini kopyalayın (örn: `https://analyzer-service-xxxxx.run.app`)

**Diğer servisler için aynı adımları tekrarlayın:**
- `code-generator-service` (port: 8085)
- `cpu-hungry-service` (port: 8081)
- `memory-leaker-service` (port: 8082)
- `db-connection-service` (port: 8083)

### 3.3 Cloud Run Deployment (Terminal ile - HIZLI)

```bash
# Terminal'de tüm servisleri tek komutla deploy edin
cd /Users/mertkiziloglu/Desktop/SaveYourMoney

# Deploy scripti
./scripts/deploy-cloud-run.sh
```

**deploy-cloud-run.sh dosyası:**
```bash
#!/bin/bash

PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo "🚀 Deploying SaveYourMoney to Cloud Run..."
echo "Project: $PROJECT_ID"
echo "Region: $REGION"

# Servisleri tanımla
declare -A services
services=(
  ["analyzer-service"]="8084"
  ["code-generator-service"]="8085"
  ["cpu-hungry-service"]="8081"
  ["memory-leaker-service"]="8082"
  ["db-connection-service"]="8083"
)

# Her servisi build, push ve deploy et
for service in "${!services[@]}"; do
  port="${services[$service]}"

  echo ""
  echo "📦 Building $service..."
  cd "$service"
  ./mvnw clean package -DskipTests

  echo "🐳 Building Docker image..."
  docker build -t gcr.io/$PROJECT_ID/$service:v1 .

  echo "☁️  Pushing to Container Registry..."
  docker push gcr.io/$PROJECT_ID/$service:v1

  echo "🚀 Deploying to Cloud Run..."
  gcloud run deploy $service \
    --image gcr.io/$PROJECT_ID/$service:v1 \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --port $port \
    --memory 1Gi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 10 \
    --set-env-vars "SERVER_PORT=$port,SPRING_PROFILES_ACTIVE=prod"

  cd ..
done

echo ""
echo "✅ All services deployed!"
echo ""
echo "📝 Service URLs:"
gcloud run services list --platform managed --region $REGION
```

### 3.4 Dashboard UI Deployment

```bash
# Dashboard için static hosting (Firebase Hosting veya Cloud Storage)

# Option 1: Firebase Hosting
cd dashboard-ui/public
firebase init hosting
firebase deploy --only hosting

# Option 2: Cloud Storage + Cloud CDN
gsutil mb -p $PROJECT_ID -c STANDARD -l us-central1 gs://$PROJECT_ID-dashboard
gsutil -m cp -r public/* gs://$PROJECT_ID-dashboard/
gsutil iam ch allUsers:objectViewer gs://$PROJECT_ID-dashboard
gsutil web set -m index.html -e 404.html gs://$PROJECT_ID-dashboard
```

---

## 4. Google Kubernetes Engine (GKE) ile Deployment

Daha gelişmiş senaryolar ve production ortamı için GKE kullanın.

### 4.1 GKE Cluster Oluşturma (UI)

1. **GCP Console** → **Kubernetes Engine** → **Clusters**
2. **"CREATE CLUSTER"** → **"GKE Standard"** seçin
3. Cluster ayarları:
   - **Name**: `saveyourmoney-cluster`
   - **Location type**: `Zonal`
   - **Zone**: `us-central1-a`
   - **Node pool**:
     - **Machine type**: `e2-medium` (2 vCPU, 4GB RAM)
     - **Number of nodes**: `3`
   - **Networking**:
     - ✅ Enable VPC-native
4. **"CREATE"** butonuna tıklayın (5-10 dakika sürer)

### 4.2 GKE Cluster Oluşturma (Terminal)

```bash
# Cluster oluştur
gcloud container clusters create saveyourmoney-cluster \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type e2-medium \
  --enable-autoscaling \
  --min-nodes 1 \
  --max-nodes 5 \
  --enable-autorepair \
  --enable-autoupgrade

# kubectl'i cluster'a bağla
gcloud container clusters get-credentials saveyourmoney-cluster --zone us-central1-a
```

### 4.3 Kubernetes Manifests

**kubernetes/deployment.yaml** (Örnek - Analyzer Service):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: analyzer-service
  namespace: default
spec:
  replicas: 2
  selector:
    matchLabels:
      app: analyzer-service
  template:
    metadata:
      labels:
        app: analyzer-service
    spec:
      containers:
      - name: analyzer-service
        image: gcr.io/saveyourmoney-prod/analyzer-service:v1
        ports:
        - containerPort: 8084
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SERVER_PORT
          value: "8084"
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8084
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: analyzer-service
spec:
  type: LoadBalancer
  selector:
    app: analyzer-service
  ports:
  - port: 80
    targetPort: 8084
    protocol: TCP
```

### 4.4 Deploy to GKE

```bash
# Tüm servisleri deploy et
kubectl apply -f kubernetes/

# Deployment durumunu kontrol et
kubectl get deployments
kubectl get pods
kubectl get services

# External IP'leri al
kubectl get services -o wide

# Logs
kubectl logs -f deployment/analyzer-service
```

---

## 5. Firestore Veritabanı Kurulumu

### 5.1 Firestore Oluşturma (UI)

1. **GCP Console** → **Firestore**
2. **"CREATE DATABASE"** butonuna tıklayın
3. Database ayarları:
   - **Mode**: `Native mode` (daha güçlü)
   - **Location**: `us-central` (veya region seçin)
4. **"CREATE DATABASE"** tıklayın

### 5.2 Firestore Security Rules

**Firestore Rules** sekmesinde:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Public read, authenticated write
    match /{document=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }

    // Metrics collection
    match /metrics/{metricId} {
      allow read, write: if true;
    }

    // Recommendations collection
    match /recommendations/{recId} {
      allow read, write: if true;
    }
  }
}
```

### 5.3 Backend'e Firestore Bağlama

**Spring Boot servislere Firestore dependency ekleyin:**

`pom.xml`:
```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-firestore</artifactId>
    <version>3.16.0</version>
</dependency>
```

**application-prod.yml**:
```yaml
spring:
  cloud:
    gcp:
      project-id: saveyourmoney-prod
      firestore:
        enabled: true
```

---

## 6. Monitoring ve Logging

### 6.1 Cloud Logging

GCP otomatik olarak tüm Cloud Run/GKE loglarını toplar.

**Logs görüntüleme (UI):**
1. **GCP Console** → **Logging** → **Logs Explorer**
2. Query:
```
resource.type="cloud_run_revision"
resource.labels.service_name="analyzer-service"
severity>=WARNING
```

### 6.2 Cloud Monitoring Dashboard

1. **GCP Console** → **Monitoring** → **Dashboards**
2. **"+ CREATE DASHBOARD"** tıklayın
3. Dashboard adı: `SaveYourMoney Monitoring`
4. **"+ ADD CHART"** tıklayarak metrikler ekleyin:
   - **CPU Usage**: `run.googleapis.com/container/cpu/utilizations`
   - **Memory Usage**: `run.googleapis.com/container/memory/utilizations`
   - **Request Count**: `run.googleapis.com/request_count`
   - **Request Latency**: `run.googleapis.com/request_latencies`

### 6.3 Alerting Politikaları

1. **Monitoring** → **Alerting** → **"+ CREATE POLICY"**
2. Alert kuralı:
   - **Condition**: CPU utilization > 80%
   - **Duration**: 5 minutes
   - **Notification**: Email
3. **"SAVE"** tıklayın

---

## 7. Maliyet Optimizasyonu

### 7.1 Cloud Run Maliyet Optimizasyonu

```bash
# Min instances = 0 (cold start var ama ücretsiz)
gcloud run services update analyzer-service \
  --min-instances 0 \
  --region us-central1

# CPU throttling (sadece request sırasında CPU kullan)
gcloud run services update analyzer-service \
  --cpu-throttling \
  --region us-central1

# Memory optimizasyonu
gcloud run services update analyzer-service \
  --memory 512Mi \
  --region us-central1
```

### 7.2 GKE Maliyet Optimizasyonu

```bash
# Autopilot mode kullanın (daha ucuz, otomatik optimize)
gcloud container clusters create saveyourmoney-autopilot \
  --enable-autoscaling \
  --autoscaling-profile optimize-utilization \
  --region us-central1 \
  --release-channel regular

# Preemptible nodes (spot instance)
gcloud container node-pools create preemptible-pool \
  --cluster saveyourmoney-cluster \
  --zone us-central1-a \
  --preemptible \
  --num-nodes 2 \
  --machine-type e2-medium
```

### 7.3 Budget Alerts

1. **Billing** → **Budgets & alerts**
2. **"CREATE BUDGET"** tıklayın
3. Budget ayarları:
   - **Name**: `SaveYourMoney Monthly Budget`
   - **Budget amount**: `$50/month`
   - **Alert threshold**: 50%, 75%, 90%, 100%
4. **"FINISH"** tıklayın

---

## 8. VS Code + Copilot: Komple Automation Script

### 8.1 Tek Komutla GCP'ye Deploy

**deploy-to-gcp.sh** (VS Code terminal'de çalıştırın):
```bash
#!/bin/bash

# GitHub Copilot ile oluşturulmuş deployment scripti
# Kullanım: ./deploy-to-gcp.sh

set -e

echo "🚀 SaveYourMoney GCP Deployment başlıyor..."

# 1. Project setup
PROJECT_ID="saveyourmoney-prod"
REGION="us-central1"
ZONE="us-central1-a"

echo "📋 Project: $PROJECT_ID"
echo "🌍 Region: $REGION"

# 2. GCP authentication
echo "🔐 Authenticating..."
gcloud auth login --quiet
gcloud config set project $PROJECT_ID
gcloud config set compute/region $REGION

# 3. Enable APIs
echo "⚙️  Enabling APIs..."
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  containerregistry.googleapis.com \
  firestore.googleapis.com \
  monitoring.googleapis.com

# 4. Build & Deploy services
echo "🏗️  Building and deploying services..."

SERVICES=("analyzer-service:8084" "code-generator-service:8085" "cpu-hungry-service:8081" "memory-leaker-service:8082" "db-connection-service:8083")

for service_info in "${SERVICES[@]}"; do
  IFS=':' read -r service port <<< "$service_info"

  echo ""
  echo "📦 Processing $service..."

  cd "$service"

  # Maven build
  ./mvnw clean package -DskipTests

  # Docker build & push
  docker build -t gcr.io/$PROJECT_ID/$service:latest .
  docker push gcr.io/$PROJECT_ID/$service:latest

  # Cloud Run deploy
  gcloud run deploy $service \
    --image gcr.io/$PROJECT_ID/$service:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --port $port \
    --memory 1Gi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 10 \
    --set-env-vars "SERVER_PORT=$port,SPRING_PROFILES_ACTIVE=prod" \
    --quiet

  cd ..
done

# 5. Deploy dashboard UI
echo ""
echo "🎨 Deploying dashboard..."
cd dashboard-ui/public
gsutil -m rsync -r . gs://$PROJECT_ID-dashboard/
gsutil web set -m index.html gs://$PROJECT_ID-dashboard
gsutil iam ch allUsers:objectViewer gs://$PROJECT_ID-dashboard

# 6. Create Firestore database
echo ""
echo "🗄️  Setting up Firestore..."
gcloud firestore databases create --region=$REGION --quiet || echo "Firestore already exists"

# 7. Setup monitoring
echo ""
echo "📊 Setting up monitoring..."
# (Monitoring dashboard JSON'ı import edin)

echo ""
echo "✅ Deployment tamamlandı!"
echo ""
echo "📝 Service URLs:"
gcloud run services list --platform managed --region $REGION --format="table(metadata.name,status.url)"

echo ""
echo "🎨 Dashboard URL:"
echo "https://storage.googleapis.com/$PROJECT_ID-dashboard/index.html"
```

### 8.2 VS Code ile Kullanım

1. **Terminal'i açın**: `Ctrl+` ` veya `Cmd+` `
2. Script'i çalıştırılabilir yapın:
   ```bash
   chmod +x deploy-to-gcp.sh
   ```
3. Deploy edin:
   ```bash
   ./deploy-to-gcp.sh
   ```

### 8.3 GitHub Copilot Chat ile Etkileşimli Deploy

VS Code'da Copilot Chat'i açın (`Cmd+Shift+I` / `Ctrl+Shift+I`) ve:

```
@terminal SaveYourMoney projesini GCP Cloud Run'a deploy et.
Tüm 5 backend servisini ve dashboard'u deploy et.
```

Copilot otomatik olarak yukarıdaki script'i oluşturacak ve çalıştıracaktır.

---

## 9. Troubleshooting

### 9.1 Cloud Run Deployment Hataları

**Error: "Permission denied"**
```bash
# Solution:
gcloud auth application-default login
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="user:YOUR_EMAIL@gmail.com" \
  --role="roles/run.admin"
```

**Error: "Image not found"**
```bash
# Solution: Docker image'ı Container Registry'ye push ettiğinizden emin olun
docker push gcr.io/$PROJECT_ID/analyzer-service:v1
```

### 9.2 GKE Connection Issues

```bash
# Cluster credentials'ı yeniden alın
gcloud container clusters get-credentials saveyourmoney-cluster \
  --zone us-central1-a

# kubectl config'i kontrol edin
kubectl config current-context
```

### 9.3 Firestore Connection Issues

```bash
# Application Default Credentials set edin
gcloud auth application-default login

# Service account oluşturun ve key indirin
gcloud iam service-accounts create saveyourmoney-sa
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:saveyourmoney-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/datastore.user"
gcloud iam service-accounts keys create key.json \
  --iam-account=saveyourmoney-sa@$PROJECT_ID.iam.gserviceaccount.com
```

---

## 10. Özet: Hızlı Başlangıç Checklist

- [ ] GCP Project oluştur
- [ ] Billing hesabı bağla
- [ ] Gerekli API'leri aktifleştir
- [ ] gcloud CLI kur ve authenticate ol
- [ ] VS Code Cloud Code extension'ı kur
- [ ] Docker image'ları build et
- [ ] Container Registry'ye push et
- [ ] Cloud Run'a deploy et (veya GKE)
- [ ] Firestore database oluştur
- [ ] Dashboard'u static hosting'e deploy et
- [ ] Monitoring dashboard kur
- [ ] Budget alert ayarla

---

## 📚 Ek Kaynaklar

- **GCP Documentation**: https://cloud.google.com/docs
- **Cloud Run Docs**: https://cloud.google.com/run/docs
- **GKE Docs**: https://cloud.google.com/kubernetes-engine/docs
- **Firestore Docs**: https://cloud.google.com/firestore/docs
- **VS Code Cloud Code**: https://cloud.google.com/code/docs/vscode

---

## 💡 Pro Tips

1. **Free Tier Kullanın**: Cloud Run ilk 2 milyon request ücretsiz
2. **Autopilot GKE**: Cluster yönetimi istemiyorsanız Autopilot mode
3. **Cloud Build**: CI/CD için GitHub'dan otomatik deploy
4. **Secret Manager**: API key'leri güvenli saklamak için
5. **Cloud CDN**: Dashboard için global CDN kullanın

---

**Hazırlayan**: Claude Sonnet 4.5
**Tarih**: 2026-02-03
**Versiyon**: 1.0
