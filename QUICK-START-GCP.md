# 🚀 SaveYourMoney - GitHub'dan GCP'ye Quick Start

**5 adımda projenizi GCP'de çalıştırın!**

---

## 📋 Ön Gereksinimler

### 1. Google Cloud Platform Hesabı
- GCP hesabınız var mı? → https://console.cloud.google.com
- Billing etkinleştirilmiş mi? → https://console.cloud.google.com/billing

### 2. Bilgisayarınızda Kurulu Olması Gerekenler
```bash
# Kontrol edin:
java -version         # Java 17+ gerekli
mvn -version          # Maven 3.8+ gerekli
docker --version      # Docker gerekli
gcloud version        # Google Cloud SDK gerekli
```

**Kurulu değilse:**
```bash
# macOS için:
brew install openjdk@17
brew install maven
brew install --cask docker
brew install --cask google-cloud-sdk

# Windows için:
# https://cloud.google.com/sdk/docs/install adresinden indirin
```

---

## 🎯 ADIM 1: GitHub Repository Klonlama

```bash
# 1. Projenizi klonlayın
cd ~/Desktop
git clone https://github.com/YOUR-USERNAME/SaveYourMoney.git
cd SaveYourMoney

# 2. Proje yapısını kontrol edin
ls -la
# Görmeniz gerekenler:
# - analyzer-service/
# - code-generator-service/
# - demo-services/
# - dashboard-ui/
# - README.md
```

---

## 🎯 ADIM 2: GCP Projesi Kurulumu

### 2.1 GCP Authentication

```bash
# 1. GCP'ye login olun (browser açılacak)
gcloud auth login

# 2. Yeni proje oluşturun
export PROJECT_ID="saveyourmoney-$(date +%s)"
gcloud projects create $PROJECT_ID --name="SaveYourMoney"

# 3. Projeyi aktif edin
gcloud config set project $PROJECT_ID

# 4. Billing hesabınızı bulun
gcloud billing accounts list

# 5. Billing'i projeye bağlayın (BILLING_ACCOUNT_ID'yi yukarıdaki komuttan alın)
export BILLING_ACCOUNT_ID="YOUR-BILLING-ACCOUNT-ID"
gcloud billing projects link $PROJECT_ID --billing-account=$BILLING_ACCOUNT_ID

# 6. Region ayarlayın
gcloud config set compute/region us-central1
gcloud config set compute/zone us-central1-a
```

### 2.2 GCP API'lerini Etkinleştirme

```bash
# Gerekli tüm API'leri etkinleştirin (2-3 dakika sürer)
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  containerregistry.googleapis.com \
  firestore.googleapis.com \
  monitoring.googleapis.com \
  logging.googleapis.com \
  storage-api.googleapis.com

# Kontrol edin:
gcloud services list --enabled | grep -E 'run|build|firestore'
```

---

## 🎯 ADIM 3: Firestore Veritabanı Kurulumu

### 3.1 Firestore Database Oluşturma

```bash
# 1. Firestore database oluşturun (Native mode)
gcloud firestore databases create --location=us-central

# 2. Firebase config dosyasını oluşturun
cat > firebase-setup/firebase-config.json <<'EOF'
{
  "projectId": "REPLACE_WITH_YOUR_PROJECT_ID",
  "storageBucket": "REPLACE_WITH_YOUR_PROJECT_ID.appspot.com"
}
EOF

# 3. Project ID'yi değiştirin
sed -i "" "s/REPLACE_WITH_YOUR_PROJECT_ID/$PROJECT_ID/g" firebase-setup/firebase-config.json

# 4. Service account key oluşturun (Firebase Admin SDK için)
gcloud iam service-accounts create firebase-admin \
  --display-name="Firebase Admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:firebase-admin@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/datastore.user"

gcloud iam service-accounts keys create firebase-setup/serviceAccountKey.json \
  --iam-account=firebase-admin@$PROJECT_ID.iam.gserviceaccount.com

echo "✅ Firestore database oluşturuldu!"
```

### 3.2 Mock Data Yükleme (Opsiyonel - Test için)

```bash
# Firebase Admin SDK kullanarak mock data yükleyin
cd firebase-setup
npm install firebase-admin
node upload-mock-data.js

cd ..
```

---

## 🎯 ADIM 4: Servisleri GCP Cloud Run'a Deploy Etme

### 4.1 Docker Authentication

```bash
# Docker'ı GCP Container Registry'ye bağlayın
gcloud auth configure-docker
```

### 4.2 Automated Deployment Script

**Seçenek 1: Tek Komutla Deploy (ÖNERİLEN)**

```bash
# Deploy scriptine çalıştırma izni verin
chmod +x scripts/deploy-to-gcp.sh

# Tüm servisleri deploy edin (10-15 dakika)
./scripts/deploy-to-gcp.sh
```

**Seçenek 2: Manuel Deploy (Adım Adım)**

```bash
# Her servisi ayrı ayrı deploy edin:

# 1. Analyzer Service
cd analyzer-service
mvn clean package -DskipTests
docker build -t gcr.io/$PROJECT_ID/analyzer-service:v1 .
docker push gcr.io/$PROJECT_ID/analyzer-service:v1

gcloud run deploy analyzer-service \
  --image gcr.io/$PROJECT_ID/analyzer-service:v1 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8084 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 10 \
  --set-env-vars "SERVER_PORT=8084,SPRING_PROFILES_ACTIVE=prod,GOOGLE_CLOUD_PROJECT=$PROJECT_ID"

cd ..

# 2. Code Generator Service
cd code-generator-service
mvn clean package -DskipTests
docker build -t gcr.io/$PROJECT_ID/code-generator-service:v1 .
docker push gcr.io/$PROJECT_ID/code-generator-service:v1

gcloud run deploy code-generator-service \
  --image gcr.io/$PROJECT_ID/code-generator-service:v1 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8085 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 10 \
  --set-env-vars "SERVER_PORT=8085,SPRING_PROFILES_ACTIVE=prod,GOOGLE_CLOUD_PROJECT=$PROJECT_ID"

cd ..

# 3. Demo Services (CPU Hungry)
cd demo-services/cpu-hungry-service
mvn clean package -DskipTests
docker build -t gcr.io/$PROJECT_ID/cpu-hungry-service:v1 .
docker push gcr.io/$PROJECT_ID/cpu-hungry-service:v1

gcloud run deploy cpu-hungry-service \
  --image gcr.io/$PROJECT_ID/cpu-hungry-service:v1 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8081 \
  --memory 512Mi \
  --cpu 0.5 \
  --min-instances 0 \
  --max-instances 5 \
  --set-env-vars "SERVER_PORT=8081,SPRING_PROFILES_ACTIVE=prod"

cd ../..

# 4. Demo Services (Memory Leaker)
cd demo-services/memory-leaker-service
mvn clean package -DskipTests
docker build -t gcr.io/$PROJECT_ID/memory-leaker-service:v1 .
docker push gcr.io/$PROJECT_ID/memory-leaker-service:v1

gcloud run deploy memory-leaker-service \
  --image gcr.io/$PROJECT_ID/memory-leaker-service:v1 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8082 \
  --memory 512Mi \
  --cpu 0.5 \
  --min-instances 0 \
  --max-instances 5 \
  --set-env-vars "SERVER_PORT=8082,SPRING_PROFILES_ACTIVE=prod"

cd ../..

# 5. Demo Services (DB Connection)
cd demo-services/db-connection-service
mvn clean package -DskipTests
docker build -t gcr.io/$PROJECT_ID/db-connection-service:v1 .
docker push gcr.io/$PROJECT_ID/db-connection-service:v1

gcloud run deploy db-connection-service \
  --image gcr.io/$PROJECT_ID/db-connection-service:v1 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8083 \
  --memory 512Mi \
  --cpu 0.5 \
  --min-instances 0 \
  --max-instances 5 \
  --set-env-vars "SERVER_PORT=8083,SPRING_PROFILES_ACTIVE=prod"

cd ../..
```

### 4.3 Servis URL'lerini Kaydedin

```bash
# Tüm servislerin URL'lerini listeleyin
echo "📝 Service URLs:"
gcloud run services list --platform managed --region us-central1

# URL'leri environment variable olarak kaydedin
export ANALYZER_URL=$(gcloud run services describe analyzer-service --platform managed --region us-central1 --format 'value(status.url)')
export CODEGEN_URL=$(gcloud run services describe code-generator-service --platform managed --region us-central1 --format 'value(status.url)')
export CPU_HUNGRY_URL=$(gcloud run services describe cpu-hungry-service --platform managed --region us-central1 --format 'value(status.url)')
export MEMORY_LEAKER_URL=$(gcloud run services describe memory-leaker-service --platform managed --region us-central1 --format 'value(status.url)')
export DB_CONNECTION_URL=$(gcloud run services describe db-connection-service --platform managed --region us-central1 --format 'value(status.url)')

echo "Analyzer Service: $ANALYZER_URL"
echo "Code Generator: $CODEGEN_URL"
echo "CPU Hungry: $CPU_HUNGRY_URL"
echo "Memory Leaker: $MEMORY_LEAKER_URL"
echo "DB Connection: $DB_CONNECTION_URL"
```

---

## 🎯 ADIM 5: Dashboard UI Deployment

### 5.1 Firebase Hosting ile Deploy (ÖNERİLEN)

```bash
# 1. Firebase CLI kurulumu
npm install -g firebase-tools

# 2. Firebase login
firebase login

# 3. Firebase project'i başlatın
cd dashboard-ui
firebase init hosting

# Sorulara cevaplar:
# - "Select a default Firebase project" → Mevcut projenizi seçin veya yeni oluşturun
# - "What do you want to use as your public directory?" → public
# - "Configure as a single-page app?" → No
# - "Set up automatic builds with GitHub?" → No

# 4. Dashboard config'i güncelleyin (API URL'lerini değiştirin)
cat > public/config/config.js <<EOF
const API_CONFIG = {
  ANALYZER_SERVICE_URL: '$ANALYZER_URL',
  CODE_GENERATOR_URL: '$CODEGEN_URL',
  CPU_HUNGRY_URL: '$CPU_HUNGRY_URL',
  MEMORY_LEAKER_URL: '$MEMORY_LEAKER_URL',
  DB_CONNECTION_URL: '$DB_CONNECTION_URL',
  FIREBASE_PROJECT_ID: '$PROJECT_ID'
};
EOF

# 5. Deploy edin
firebase deploy --only hosting

# Dashboard URL'i alacaksınız:
# https://YOUR-PROJECT.web.app
```

### 5.2 Cloud Storage ile Deploy (Alternatif)

```bash
# 1. Storage bucket oluşturun
gsutil mb -p $PROJECT_ID -c STANDARD -l us-central1 gs://$PROJECT_ID-dashboard

# 2. Dashboard dosyalarını yükleyin
cd dashboard-ui/public

# 3. API config dosyasını güncelleyin
cat > config/config.js <<EOF
const API_CONFIG = {
  ANALYZER_SERVICE_URL: '$ANALYZER_URL',
  CODE_GENERATOR_URL: '$CODEGEN_URL',
  CPU_HUNGRY_URL: '$CPU_HUNGRY_URL',
  MEMORY_LEAKER_URL: '$MEMORY_LEAKER_URL',
  DB_CONNECTION_URL: '$DB_CONNECTION_URL',
  FIREBASE_PROJECT_ID: '$PROJECT_ID'
};
EOF

# 4. Dosyaları upload edin
gsutil -m cp -r . gs://$PROJECT_ID-dashboard/

# 5. Public access açın
gsutil iam ch allUsers:objectViewer gs://$PROJECT_ID-dashboard

# 6. Web hosting ayarları
gsutil web set -m index.html -e 404.html gs://$PROJECT_ID-dashboard

echo "Dashboard URL: https://storage.googleapis.com/$PROJECT_ID-dashboard/index.html"

cd ../..
```

---

## 🎯 ADIM 6: Test ve Doğrulama

### 6.1 Health Checks

```bash
# Tüm servislerin sağlığını kontrol edin
echo "Testing Analyzer Service..."
curl $ANALYZER_URL/api/health

echo "Testing Code Generator Service..."
curl $CODEGEN_URL/api/health

echo "Testing CPU Hungry Service..."
curl $CPU_HUNGRY_URL/api/health

echo "Testing Memory Leaker Service..."
curl $MEMORY_LEAKER_URL/api/health

echo "Testing DB Connection Service..."
curl $DB_CONNECTION_URL/api/health
```

### 6.2 Metrics Collection

```bash
# 1. Metrics toplamaya başlayın
curl -X POST $ANALYZER_URL/api/collect-metrics

# 2. Birkaç dakika bekleyin (30-60 saniye)
sleep 60

# 3. Analiz çalıştırın
curl -X POST $ANALYZER_URL/api/analyze-all

# 4. Dashboard'u kontrol edin
curl $ANALYZER_URL/api/dashboard | jq
```

### 6.3 Load Testing (Opsiyonel)

```bash
# JMeter ile load test çalıştırın
cd jmeter-tests

# URL'leri güncelleyin
sed -i "" "s|localhost:8081|${CPU_HUNGRY_URL#https://}|g" scripts/cpu-hungry-service.jmx
sed -i "" "s|localhost:8082|${MEMORY_LEAKER_URL#https://}|g" scripts/memory-leaker-service.jmx
sed -i "" "s|localhost:8083|${DB_CONNECTION_URL#https://}|g" scripts/db-connection-service.jmx

# Load test çalıştırın
./run-all-tests.sh

cd ..
```

---

## 🎯 ADIM 7: Monitoring ve Logging

### 7.1 Cloud Logging

```bash
# Logs'ları görüntüleyin
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=analyzer-service" --limit 50 --format json

# Alternatif: GCP Console'da
# https://console.cloud.google.com/logs
```

### 7.2 Cloud Monitoring Dashboard

```bash
# GCP Console'da Monitoring Dashboard açın
echo "Monitoring Dashboard: https://console.cloud.google.com/monitoring/dashboards?project=$PROJECT_ID"
```

---

## ✅ Tamamlandı Checklist

- [ ] GCP projesi oluşturuldu
- [ ] Billing etkinleştirildi
- [ ] API'ler etkinleştirildi
- [ ] Firestore database oluşturuldu
- [ ] Analyzer Service deploy edildi
- [ ] Code Generator Service deploy edildi
- [ ] 3 Demo Service deploy edildi
- [ ] Dashboard UI deploy edildi
- [ ] Health checks geçildi
- [ ] Metrics collection çalışıyor
- [ ] Dashboard'da data görünüyor

---

## 🎉 Başarılı! Projeniz Çalışıyor

### URL'ler:
```bash
echo "🎨 Dashboard: https://YOUR-PROJECT.web.app"
echo "🔍 Analyzer API: $ANALYZER_URL"
echo "🤖 Code Generator: $CODEGEN_URL"
echo "📊 Monitoring: https://console.cloud.google.com/monitoring?project=$PROJECT_ID"
echo "📝 Logs: https://console.cloud.google.com/logs?project=$PROJECT_ID"
```

### Demo Flow:
1. Dashboard'u açın
2. "Overview" sayfasında servisleri görün
3. "Metrics" sayfasında gerçek zamanlı metrikleri izleyin
4. "Recommendations" sayfasında AI önerilerini görün
5. "Deployments" sayfasında deployment history'yi kontrol edin

---

## 🆘 Troubleshooting

### Problem: "Permission denied" hatası
```bash
# Solution:
gcloud auth application-default login
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="user:$(gcloud config get-value account)" \
  --role="roles/owner"
```

### Problem: Docker build başarısız
```bash
# Solution: Docker daemon'ın çalıştığından emin olun
docker ps

# macOS'ta Docker Desktop'ı başlatın
open -a Docker
```

### Problem: Maven build hatası
```bash
# Solution: Java versiyonunu kontrol edin
java -version  # 17+ olmalı

# macOS'ta doğru Java'yı seçin
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Problem: Firestore connection hatası
```bash
# Solution: Service account key'i kontrol edin
ls -la firebase-setup/serviceAccountKey.json

# Yeniden oluşturun:
gcloud iam service-accounts keys create firebase-setup/serviceAccountKey.json \
  --iam-account=firebase-admin@$PROJECT_ID.iam.gserviceaccount.com
```

---

## 💰 Maliyet Tahmini

### Free Tier Dahilinde (İlk Kullanım):
- Cloud Run: İlk 2M request ücretsiz
- Firestore: 50K read, 20K write/day ücretsiz
- Cloud Storage: 5GB ücretsiz
- **Toplam Maliyet**: $0-5/ay (free tier dahilinde)

### Production Kullanım:
- Cloud Run: ~$10-20/ay (1M request için)
- Firestore: ~$5-10/ay
- Cloud Storage: ~$1/ay
- **Toplam Maliyet**: ~$15-30/ay

---

**🎯 Projeniz hazır! Şimdi demo yapabilirsiniz!** 🚀

**Son Adım:** Presentation için ekran görüntüleri alın:
1. Dashboard overview
2. Metrics charts
3. Recommendations page
4. Cost savings analysis
5. GCP Monitoring dashboard
