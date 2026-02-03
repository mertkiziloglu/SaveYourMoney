# 🚀 GCP UI Üzerinden Manuel Deployment Rehberi

**SaveYourMoney** projesini **gcloud CLI veya Cloud Shell KULLANMADAN**, tamamen **GCP Console UI** üzerinden deploy etme rehberi.

---

## 📋 İçindekiler

1. [Ön Hazırlık](#1-ön-hazırlık)
2. [GCP Projesi Kurulumu](#2-gcp-projesi-kurulumu)
3. [GitHub Repository Hazırlama](#3-github-repository-hazırlama)
4. [Cloud Build ile Otomatik Docker Build](#4-cloud-build-ile-otomatik-docker-build)
5. [Cloud Run Deployment (UI)](#5-cloud-run-deployment-ui)
6. [Firestore Database Kurulumu](#6-firestore-database-kurulumu)
7. [Dashboard UI Deployment](#7-dashboard-ui-deployment)
8. [Monitoring ve Logging](#8-monitoring-ve-logging)
9. [Test ve Doğrulama](#9-test-ve-doğrulama)

---

## 1. Ön Hazırlık

### 1.1 Gereksinimler

- ✅ Google hesabı (Gmail)
- ✅ Kredi kartı (Free tier için gerekli - $300 ücretsiz kredi alacaksınız)
- ✅ GitHub hesabı
- ✅ Web browser (Chrome önerilir)

### 1.2 Bilgisayarınızda Kurulu Olması Gerekenler

- ✅ **Java 17+** (Maven build için)
- ✅ **Maven 3.8+** (veya projede bulunan `mvnw` wrapper'ı kullanın)
- ✅ **Git** (kod yüklemek için)

**Kurulu mu kontrol edin:**
```bash
java -version    # Java 17+ olmalı
mvn -version     # Maven 3.8+ olmalı
git --version    # Git kurulu olmalı
```

---

## 2. GCP Projesi Kurulumu

### 2.1 Google Cloud Console'a Giriş

1. **Tarayıcınızda** bu adresi açın: https://console.cloud.google.com
2. Google hesabınızla **Sign in** yapın
3. İlk kez kullanıyorsanız **Terms of Service**'i kabul edin
4. **$300 Free Credit** için kredi kartı bilgilerinizi girin (otomatik ücretlendirme yapılmaz)

### 2.2 Yeni Proje Oluşturma

1. Üst menüde **proje seçici** (Project dropdown) tıklayın
2. Açılan pencerede **"NEW PROJECT"** butonuna tıklayın
3. Proje bilgilerini doldurun:
   - **Project name**: `saveyourmoney-prod` (veya istediğiniz isim)
   - **Project ID**: Otomatik oluşacak (örn: `saveyourmoney-prod-123456`)
   - **Organization**: Yoksa boş bırakın
   - **Location**: Yoksa boş bırakın
4. **"CREATE"** butonuna tıklayın
5. **Proje oluşturuluyor** bildirimi gelecek (30 saniye sürer)
6. Oluşturulduktan sonra üst menüden **yeni projenizi seçin**

**📝 ÖNEMLİ**: Project ID'nizi not edin (örn: `saveyourmoney-prod-123456`). Sonra lazım olacak!

### 2.3 Billing Hesabı Bağlama

1. Sol menüden **☰ (Navigation menu)** → **Billing** tıklayın
2. **"LINK A BILLING ACCOUNT"** butonuna tıklayın
3. Billing hesabınızı seçin (veya yeni oluşturun)
4. **"SET ACCOUNT"** tıklayın

### 2.4 Gerekli API'leri Etkinleştirme

Her bir API için aşağıdaki adımları tekrarlayın:

1. Sol menüden **☰ → APIs & Services → Library** seçin
2. Arama kutusuna API adını yazın
3. API'yi seçin
4. **"ENABLE"** butonuna tıklayın
5. API etkinleşene kadar bekleyin (10-30 saniye)

**Etkinleştirilmesi gereken API'ler:**

| API Adı | Arama Terimi | Neden Gerekli |
|---------|--------------|---------------|
| Cloud Run API | `cloud run` | Servisleri çalıştırmak için |
| Cloud Build API | `cloud build` | Docker image build için |
| Artifact Registry API | `artifact registry` | Docker image saklamak için |
| Cloud Firestore API | `firestore` | Database için |
| Cloud Logging API | `cloud logging` | Log toplamak için |
| Cloud Monitoring API | `cloud monitoring` | Monitoring için |
| Cloud Storage API | `cloud storage` | Dashboard hosting için |

**✅ Tamamlandı mı kontrol edin:**
- Sol menüden **☰ → APIs & Services → Enabled APIs** gidin
- 7 API'yi görmelisiniz

---

## 3. GitHub Repository Hazırlama

### 3.1 Projenizi GitHub'a Yükleme

Eğer proje henüz GitHub'da değilse:

```bash
# Proje dizinine gidin
cd /Users/mertkiziloglu/Desktop/SaveYourMoney

# Git repository başlatın
git init

# Tüm dosyaları ekleyin
git add .

# İlk commit
git commit -m "Initial commit - SaveYourMoney project"

# GitHub'da yeni repository oluşturun (https://github.com/new)
# Repository adı: SaveYourMoney
# Public veya Private seçin

# Remote ekleyin (YOUR-USERNAME'i kendi kullanıcı adınızla değiştirin)
git remote add origin https://github.com/YOUR-USERNAME/SaveYourMoney.git

# Push edin
git branch -M main
git push -u origin main
```

### 3.2 GitHub ile GCP Bağlantısı (UI)

1. **GCP Console** → Sol menü **☰ → Cloud Build → Triggers**
2. İlk kez kullanıyorsanız **"CONNECT REPOSITORY"** butonuna tıklayın
3. **Source** olarak **GitHub** seçin
4. **Authenticate** butonuna tıklayın
5. GitHub'da açılan pencerede **"Authorize Google Cloud Build"** onaylayın
6. **"Select repository"** sayfasında repository'nizi seçin
   - **YOUR-USERNAME/SaveYourMoney** seçin
7. **"CONNECT"** tıklayın
8. **"CREATE A TRIGGER"** (şimdilik Skip edin, sonra oluşturacağız)

---

## 4. Cloud Build ile Otomatik Docker Build

Her servis için Cloud Build trigger oluşturacağız. Bu, kod değiştiğinde otomatik olarak Docker image oluşturacak.

### 4.1 Analyzer Service için Cloud Build Trigger

1. **GCP Console** → **☰ → Cloud Build → Triggers**
2. **"CREATE TRIGGER"** butonuna tıklayın
3. Trigger ayarlarını doldurun:

**Name**: `analyzer-service-trigger`

**Region**: `global`

**Event**: `Push to a branch` seçin

**Source - Repository**: `YOUR-USERNAME/SaveYourMoney` seçin

**Source - Branch**: `^main$` (main branch'e push olunca tetiklensin)

**Build Configuration**: `Cloud Build configuration file (yaml or json)` seçin

**Cloud Build configuration file location**: `/analyzer-service/cloudbuild.yaml`

4. **"CREATE"** butonuna tıklayın

### 4.2 Cloud Build Config Dosyalarını Oluşturma

Her servis için `cloudbuild.yaml` dosyası oluşturun:

**analyzer-service/cloudbuild.yaml** oluşturun:

```yaml
steps:
  # Step 1: Maven build
  - name: 'maven:3.9.6-eclipse-temurin-17'
    entrypoint: 'mvn'
    args: ['clean', 'package', '-DskipTests']
    dir: 'analyzer-service'

  # Step 2: Docker build
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'build'
      - '-t'
      - 'gcr.io/$PROJECT_ID/analyzer-service:$SHORT_SHA'
      - '-t'
      - 'gcr.io/$PROJECT_ID/analyzer-service:latest'
      - '.'
    dir: 'analyzer-service'

  # Step 3: Docker push
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'push'
      - '--all-tags'
      - 'gcr.io/$PROJECT_ID/analyzer-service'

images:
  - 'gcr.io/$PROJECT_ID/analyzer-service:latest'
  - 'gcr.io/$PROJECT_ID/analyzer-service:$SHORT_SHA'

options:
  machineType: 'E2_HIGHCPU_8'
  logging: CLOUD_LOGGING_ONLY
```

### 4.3 Tüm Servisler için Cloud Build Config

Aynı şekilde diğer servisler için de `cloudbuild.yaml` oluşturun:

**code-generator-service/cloudbuild.yaml**
**demo-services/cpu-hungry-service/cloudbuild.yaml**
**demo-services/memory-leaker-service/cloudbuild.yaml**
**demo-services/db-connection-service/cloudbuild.yaml**

(İçerik aynı, sadece servis adını değiştirin)

### 4.4 Manuel Build Tetikleme (UI)

Trigger'ları oluşturduktan sonra manuel olarak çalıştırın:

1. **Cloud Build → Triggers** sayfasında
2. Her trigger'ın yanında **"RUN"** butonuna tıklayın
3. **"RUN TRIGGER"** onaylayın
4. **Cloud Build → History** sayfasından build durumunu izleyin
5. Her build **5-10 dakika** sürecek
6. Tamamlandığında yeşil ✓ göreceksiniz

**✅ Build tamamlandı mı kontrol edin:**
- **☰ → Artifact Registry → Repositories** gidin
- `gcr.io` repository'sini seçin
- 5 servis image'ını görmelisiniz

---

## 5. Cloud Run Deployment (UI)

Her servis için Cloud Run servisi oluşturacağız.

### 5.1 Analyzer Service Deployment

1. **GCP Console** → **☰ → Cloud Run**
2. **"CREATE SERVICE"** butonuna tıklayın
3. Deployment ayarları:

**Container Image URL:**
- **"SELECT"** butonuna tıklayın
- **Artifact Registry** → `gcr.io` → `analyzer-service` → `latest` seçin
- **"SELECT"** tıklayın

**Service name**: `analyzer-service`

**Region**: `us-central1` (size yakın bölge seçin)

**CPU allocation and pricing**:
- ✅ **CPU is only allocated during request processing** (maliyet için)

**Autoscaling**:
- **Minimum number of instances**: `0`
- **Maximum number of instances**: `10`

**Authentication**:
- ✅ **Allow unauthenticated invocations** (Test için - Production'da değiştirin!)

4. **"CONTAINER, NETWORKING, SECURITY"** tab'ına tıklayın

**Container Port**: `8084`

**Memory**: `1 GiB`

**CPU**: `1`

**Environment variables** bölümünde **"+ ADD VARIABLE"** tıklayın:

```
Name: SERVER_PORT              Value: 8084
Name: SPRING_PROFILES_ACTIVE   Value: prod
Name: GOOGLE_CLOUD_PROJECT     Value: [PROJECT_ID'nizi yazın]
```

5. **"CREATE"** butonuna tıklayın

⏳ **Deploy tamamlanana kadar bekleyin** (2-3 dakika)

✅ Tamamlandığında servis URL'i göreceksiniz: `https://analyzer-service-xxxxx-uc.a.run.app`

**📝 URL'i kopyalayın ve kaydedin!**

### 5.2 Diğer Servisleri Deploy Etme

Aynı adımları her servis için tekrarlayın:

| Servis Adı | Image | Container Port | Memory | CPU | Environment Variables |
|------------|-------|----------------|--------|-----|----------------------|
| **code-generator-service** | `gcr.io/.../code-generator-service:latest` | 8085 | 1 GiB | 1 | SERVER_PORT=8085<br>SPRING_PROFILES_ACTIVE=prod<br>GOOGLE_CLOUD_PROJECT=[PROJECT_ID] |
| **cpu-hungry-service** | `gcr.io/.../cpu-hungry-service:latest` | 8081 | 512 MiB | 0.5 | SERVER_PORT=8081<br>SPRING_PROFILES_ACTIVE=prod |
| **memory-leaker-service** | `gcr.io/.../memory-leaker-service:latest` | 8082 | 512 MiB | 0.5 | SERVER_PORT=8082<br>SPRING_PROFILES_ACTIVE=prod |
| **db-connection-service** | `gcr.io/.../db-connection-service:latest` | 8083 | 512 MiB | 0.5 | SERVER_PORT=8083<br>SPRING_PROFILES_ACTIVE=prod |

### 5.3 Servis URL'lerini Toplama

**Cloud Run** sayfasında tüm servislerin URL'lerini kopyalayın:

```
ANALYZER_SERVICE:      https://analyzer-service-xxxxx-uc.a.run.app
CODE_GENERATOR:        https://code-generator-service-xxxxx-uc.a.run.app
CPU_HUNGRY:            https://cpu-hungry-service-xxxxx-uc.a.run.app
MEMORY_LEAKER:         https://memory-leaker-service-xxxxx-uc.a.run.app
DB_CONNECTION:         https://db-connection-service-xxxxx-uc.a.run.app
```

**📝 Bu URL'leri not defterine kaydedin!**

---

## 6. Firestore Database Kurulumu

### 6.1 Firestore Database Oluşturma (UI)

1. **GCP Console** → **☰ → Firestore**
2. **"CREATE DATABASE"** butonuna tıklayın
3. Database ayarları:
   - **Select Native mode**: Seçili bırakın
   - **"Continue"** tıklayın
4. **Location type**: `Multi-region` (Production için) veya `Region` (Test için)
5. **Location**: `nam5 (United States)` veya `us-central`
6. **"CREATE DATABASE"** butonuna tıklayın

⏳ Database oluşturuluyor (30 saniye)

### 6.2 Firestore Security Rules Ayarlama

1. Firestore sayfasında **"Rules"** tab'ına tıklayın
2. Aşağıdaki kuralları kopyalayıp yapıştırın:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Test için - tüm okuma/yazma açık
    // Production'da mutlaka güvenlik kuralları ekleyin!
    match /{document=**} {
      allow read, write: if true;
    }

    // Metrics koleksiyonu
    match /metrics/{metricId} {
      allow read, write: if true;
    }

    // Recommendations koleksiyonu
    match /recommendations/{recId} {
      allow read, write: if true;
    }

    // Deployments koleksiyonu
    match /deployments/{deploymentId} {
      allow read, write: if true;
    }
  }
}
```

3. **"PUBLISH"** butonuna tıklayın

### 6.3 Mock Data Yükleme (Opsiyonel)

Firebase Admin SDK ile test verisi yüklemek için:

1. **Firestore → Data** tab'ında
2. **"+ Start collection"** tıklayın
3. **Collection ID**: `metrics`
4. **Document ID**: Auto-ID
5. **Field** ekleyin:
   - `serviceName` (string): `cpu-hungry-service`
   - `cpuUsage` (number): `0.75`
   - `memoryUsage` (number): `512`
   - `timestamp` (timestamp): Şimdi
6. **"Save"** tıklayın

---

## 7. Dashboard UI Deployment

Dashboard'u Firebase Hosting veya Cloud Storage ile deploy edeceğiz.

### 7.1 Firebase Console ile Dashboard Deployment

#### Adım 1: Firebase Projesi Oluşturma

1. **Firebase Console** açın: https://console.firebase.google.com
2. **"Add project"** tıklayın
3. **"Select a project"** → Mevcut GCP projenizi seçin (`saveyourmoney-prod`)
4. **"Continue"** tıklayın
5. **Google Analytics**: İstiyorsanız etkinleştirin (opsiyonel)
6. **"Add Firebase"** tıklayın

#### Adım 2: Firebase Hosting Etkinleştirme

1. Sol menüden **"Build"** → **"Hosting"** seçin
2. **"Get started"** butonuna tıklayın
3. Firebase CLI kurulum adımlarını gösterecek (bu adımları atlayın, UI'dan yapacağız)

#### Adım 3: Dashboard Config Dosyasını Güncelleme

Yerel bilgisayarınızda `dashboard-ui/public/config/config.js` dosyasını düzenleyin:

```javascript
const API_CONFIG = {
  ANALYZER_SERVICE_URL: 'https://analyzer-service-xxxxx-uc.a.run.app',
  CODE_GENERATOR_URL: 'https://code-generator-service-xxxxx-uc.a.run.app',
  CPU_HUNGRY_URL: 'https://cpu-hungry-service-xxxxx-uc.a.run.app',
  MEMORY_LEAKER_URL: 'https://memory-leaker-service-xxxxx-uc.a.run.app',
  DB_CONNECTION_URL: 'https://db-connection-service-xxxxx-uc.a.run.app',
  FIREBASE_PROJECT_ID: 'saveyourmoney-prod-123456'  // Kendi project ID'nizi yazın
};
```

**📝 Servis URL'lerini yukarıda kaydettiğiniz gerçek URL'lerle değiştirin!**

#### Adım 4: Dashboard Dosyalarını Cloud Storage'a Yükleme

Firebase Hosting için CLI gerektiğinden, alternatif olarak Cloud Storage kullanacağız:

1. **GCP Console** → **☰ → Cloud Storage → Buckets**
2. **"CREATE"** butonuna tıklayın
3. Bucket ayarları:
   - **Name**: `[PROJECT_ID]-dashboard` (örn: `saveyourmoney-prod-123456-dashboard`)
   - **Location type**: `Region`
   - **Location**: `us-central1`
   - **Default storage class**: `Standard`
   - **Prevent public access**: **UNCHECKED** (public erişim istiyoruz)
   - **Access control**: `Fine-grained`
4. **"CREATE"** tıklayın

#### Adım 5: Dashboard Dosyalarını Upload Etme

1. Oluşturduğunuz bucket'ı açın
2. **"UPLOAD FILES"** butonuna tıklayın
3. `dashboard-ui/public/` klasöründeki **TÜM DOSYALARI** seçin ve upload edin
   - index.html
   - pages/ klasörü
   - styles/ klasörü
   - services/ klasörü
   - config/ klasörü
   - assets/ klasörü

4. Upload tamamlandıktan sonra **public erişim** açın:
   - Bucket sayfasında **"PERMISSIONS"** tab'ına tıklayın
   - **"GRANT ACCESS"** butonuna tıklayın
   - **New principals**: `allUsers`
   - **Role**: `Storage Object Viewer`
   - **"SAVE"** tıklayın

5. **Web site ayarları** yapın:
   - Bucket sayfasında **"CONFIGURATION"** tab'ına tıklayın
   - **"Edit website configuration"** tıklayın
   - **Index (main) page**: `index.html`
   - **Error page**: `index.html`
   - **"SAVE"** tıklayın

**✅ Dashboard URL'iniz**:
```
https://storage.googleapis.com/[PROJECT_ID]-dashboard/index.html
```

Örnek: `https://storage.googleapis.com/saveyourmoney-prod-123456-dashboard/index.html`

---

## 8. Monitoring ve Logging

### 8.1 Cloud Monitoring Dashboard Oluşturma

1. **GCP Console** → **☰ → Monitoring → Dashboards**
2. **"+ CREATE DASHBOARD"** butonuna tıklayın
3. **Dashboard name**: `SaveYourMoney Monitoring`
4. **"+ ADD WIDGET"** → **"Line"** seçin

**Widget 1: CPU Usage**
- **Title**: `CPU Usage - All Services`
- **Resource type**: `Cloud Run Revision`
- **Metric**: `Container CPU Utilization`
- **Filter**: (Boş bırakın - tüm servisleri gösterecek)
- **Aggregator**: `mean`
- **ADD WIDGET**

**Widget 2: Memory Usage**
- **"+ ADD WIDGET"** → **"Line"**
- **Title**: `Memory Usage - All Services`
- **Resource type**: `Cloud Run Revision`
- **Metric**: `Container Memory Utilization`
- **ADD WIDGET**

**Widget 3: Request Count**
- **"+ ADD WIDGET"** → **"Scorecard"**
- **Title**: `Total Requests (Last Hour)`
- **Resource type**: `Cloud Run Revision`
- **Metric**: `Request Count`
- **Aggregator**: `sum`
- **ADD WIDGET**

**Widget 4: Error Rate**
- **"+ ADD WIDGET"** → **"Line"**
- **Title**: `Error Rate`
- **Resource type**: `Cloud Run Revision`
- **Metric**: `Request Count`
- **Filter**: `response_code_class = 5xx`
- **ADD WIDGET**

5. **"SAVE"** butonuna tıklayın

### 8.2 Log Explorer Kullanımı

1. **GCP Console** → **☰ → Logging → Logs Explorer**
2. **Query builder** kullanarak log filtreleme:

**Analyzer Service loglarını görüntüleme:**
```
resource.type="cloud_run_revision"
resource.labels.service_name="analyzer-service"
severity>=WARNING
```

**Tüm servislerin error logları:**
```
resource.type="cloud_run_revision"
severity>=ERROR
```

3. **"RUN QUERY"** tıklayın

### 8.3 Alerting Policy Oluşturma

1. **☰ → Monitoring → Alerting**
2. **"+ CREATE POLICY"** tıklayın
3. Alert ayarları:

**Condition:**
- **"ADD CONDITION"** tıklayın
- **Target**: Cloud Run Revision
- **Metric**: `Container CPU Utilization`
- **Threshold**: `0.8` (80%)
- **For**: `5 minutes`
- **NEXT**

**Notifications:**
- **"MANAGE NOTIFICATION CHANNELS"** tıklayın
- **"ADD NEW"** → **Email** seçin
- Email adresinizi girin
- **SAVE**

**Alert name**: `High CPU Usage Alert`

4. **"CREATE POLICY"** tıklayın

---

## 9. Test ve Doğrulama

### 9.1 Health Check Testleri

Her servisin URL'ine `/api/health` ekleyerek tarayıcıda test edin:

```
https://analyzer-service-xxxxx-uc.a.run.app/api/health
https://code-generator-service-xxxxx-uc.a.run.app/api/health
https://cpu-hungry-service-xxxxx-uc.a.run.app/api/health
https://memory-leaker-service-xxxxx-uc.a.run.app/api/health
https://db-connection-service-xxxxx-uc.a.run.app/api/health
```

**Beklenen cevap**:
```json
{
  "status": "UP"
}
```

### 9.2 Dashboard Test

1. Dashboard URL'inizi tarayıcıda açın
2. **Overview** sayfasında 5 servisi görmelisiniz
3. **Metrics** sayfasında gerçek zamanlı metrikleri kontrol edin
4. **Deployments** sayfasında deployment history'yi görün

### 9.3 API Test

Tarayıcıda veya Postman ile test edin:

**Metrics toplamayı başlat:**
```
POST https://analyzer-service-xxxxx-uc.a.run.app/api/collect-metrics
```

**30 saniye bekleyin, sonra analiz çalıştırın:**
```
POST https://analyzer-service-xxxxx-uc.a.run.app/api/analyze-all
```

**Dashboard datayı görüntüle:**
```
GET https://analyzer-service-xxxxx-uc.a.run.app/api/dashboard
```

---

## 📋 Deployment Checklist

Tüm adımları tamamladınız mı kontrol edin:

- [ ] GCP projesi oluşturuldu
- [ ] Billing hesabı bağlandı
- [ ] 7 API etkinleştirildi
- [ ] GitHub repository bağlandı
- [ ] 5 Cloud Build trigger oluşturuldu
- [ ] 5 Docker image build edildi
- [ ] 5 Cloud Run servisi deploy edildi
- [ ] Servis URL'leri kaydedildi
- [ ] Firestore database oluşturuldu
- [ ] Firestore security rules ayarlandı
- [ ] Dashboard Cloud Storage'a yüklendi
- [ ] Dashboard config dosyası güncellendi
- [ ] Monitoring dashboard oluşturuldu
- [ ] Alert policy oluşturuldu
- [ ] Health check testleri başarılı
- [ ] Dashboard açılıyor ve data gösteriyor

---

## 🎉 Başarılı! Projeniz Çalışıyor

### 📝 URL'leriniz:

```
Dashboard:           https://storage.googleapis.com/[PROJECT_ID]-dashboard/index.html
Analyzer API:        https://analyzer-service-xxxxx-uc.a.run.app
Code Generator:      https://code-generator-service-xxxxx-uc.a.run.app
CPU Hungry:          https://cpu-hungry-service-xxxxx-uc.a.run.app
Memory Leaker:       https://memory-leaker-service-xxxxx-uc.a.run.app
DB Connection:       https://db-connection-service-xxxxx-uc.a.run.app
Monitoring:          https://console.cloud.google.com/monitoring
Logs:                https://console.cloud.google.com/logs
Firestore:           https://console.cloud.google.com/firestore
```

---

## 🆘 Troubleshooting

### Problem: "Permission denied" hatası

**Çözüm:**
1. **☰ → IAM & Admin → IAM**
2. Email adresinizi bulun
3. **Edit** (kalem ikonu) tıklayın
4. **+ ADD ANOTHER ROLE** tıklayın
5. `Cloud Run Admin` ve `Cloud Build Editor` rollerini ekleyin
6. **SAVE**

### Problem: Docker build başarısız

**Çözüm:**
1. **☰ → Cloud Build → History**
2. Başarısız build'e tıklayın
3. **Logs** tab'ında hata mesajını okuyun
4. Genellikle `pom.xml` veya Dockerfile hatası olur
5. GitHub'da düzeltin ve push edin (otomatik yeniden build olacak)

### Problem: Cloud Run servis başlamıyor

**Çözüm:**
1. **Cloud Run** sayfasında servisi açın
2. **LOGS** tab'ına tıklayın
3. Error loglarını kontrol edin
4. Genellikle port veya environment variable hatası olur
5. **EDIT & DEPLOY NEW REVISION** ile düzeltin

### Problem: Dashboard boş görünüyor

**Çözüm:**
1. `dashboard-ui/public/config/config.js` dosyasını kontrol edin
2. Servis URL'lerinin doğru olduğundan emin olun
3. Tarayıcı Console'da (F12) hata mesajlarını kontrol edin
4. CORS hatası varsa Cloud Run servislerine CORS ayarı ekleyin

### Problem: Firestore connection hatası

**Çözüm:**
1. **Firestore → Rules** kontrol edin
2. `allow read, write: if true;` olduğundan emin olun
3. Cloud Run servislerinde `GOOGLE_CLOUD_PROJECT` env variable kontrolü yapın

---

## 💰 Maliyet Tahmini

### Free Tier Dahilinde Kullanım:

- **Cloud Run**: İlk 2 milyon request/ay ücretsiz
- **Firestore**: 50K read, 20K write/gün ücretsiz
- **Cloud Storage**: 5GB storage ücretsiz
- **Cloud Build**: İlk 120 build dakikası/gün ücretsiz
- **Cloud Logging**: İlk 50GB/ay ücretsiz

**Toplam Maliyet (Free Tier)**: **$0-5/ay**

### Production Kullanım Tahmini:

| Hizmet | Kullanım | Aylık Maliyet |
|--------|----------|---------------|
| Cloud Run (5 servis) | 1M request/ay | $10-15 |
| Firestore | 1M read/write | $5-8 |
| Cloud Storage | 10GB | $0.50 |
| Cloud Build | 100 build | $5 |
| Cloud Monitoring | Standart | $3 |
| **TOPLAM** | | **$23-31/ay** |

---

## 🚀 Sonraki Adımlar

1. **CI/CD Pipeline**: GitHub Actions ile otomatik test + deploy
2. **Custom Domain**: Cloud Run servislerine özel domain bağlayın
3. **HTTPS Sertifikası**: Otomatik Let's Encrypt
4. **Secret Manager**: API key'leri güvenli saklayın
5. **Cloud CDN**: Dashboard için global CDN
6. **Load Balancer**: Birden fazla region için
7. **Backup**: Firestore otomatik backup

---

**Hazırlayan**: Claude Sonnet 4.5
**Tarih**: 2026-02-03
**Versiyon**: 2.0 - UI Only (gcloud CLI gerektirmez)

---

## 📞 Destek

Sorun yaşıyorsanız:
1. GCP Console → **☰ → Support** → **Create Case**
2. Community: https://stackoverflow.com/questions/tagged/google-cloud-platform
3. Documentation: https://cloud.google.com/docs
