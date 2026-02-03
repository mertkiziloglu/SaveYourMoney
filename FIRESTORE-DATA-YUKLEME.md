# 🔥 Firestore Data Yükleme Adımları

SaveYourMoney projesi için Firestore'a dummy data yükleme rehberi.

---

## 📋 İçindekiler

1. [Ön Gereksinimler](#ön-gereksinimler)
2. [GCP Service Account Key İndirme](#1-gcp-service-account-key-i̇ndirme)
3. [Node Modules Kurulumu](#2-node-modules-kurulumu)
4. [Bağlantı Testi](#3-bağlantı-testi)
5. [Mock Data Yükleme](#4-mock-data-yükleme)
6. [Firestore Console'da Doğrulama](#5-firestore-consoleda-doğrulama)
7. [Data Temizleme (Opsiyonel)](#6-data-temizleme-opsiyonel)
8. [Troubleshooting](#troubleshooting)

---

## Ön Gereksinimler

✅ **Node.js 18+** kurulu olmalı
✅ **npm** kurulu olmalı
✅ **GCP Projesi** oluşturulmuş olmalı
✅ **Firestore Database** oluşturulmuş olmalı (Native mode)
✅ **Firestore API** enabled olmalı

**Kontrol edin:**
```bash
node --version  # v18.0.0 veya üstü
npm --version   # 8.0.0 veya üstü
```

---

## 1. GCP Service Account Key İndirme

### Adım 1.1: GCP Console'a Gidin

1. Tarayıcınızda açın: https://console.cloud.google.com
2. Projenizi seçin (üst menüden)

### Adım 1.2: Service Account Oluşturun (veya Mevcut Olanı Kullanın)

**Yeni Service Account Oluşturma:**

1. Sol menüden **IAM & Admin** → **Service Accounts** seçin
2. **+ CREATE SERVICE ACCOUNT** butonuna tıklayın
3. Service account detaylarını doldurun:
   - **Service account name**: `firebase-admin`
   - **Service account ID**: `firebase-admin` (otomatik oluşur)
   - **Service account description**: `Firebase Admin SDK for SaveYourMoney`
4. **CREATE AND CONTINUE** tıklayın

5. **Grant this service account access to project** bölümünde:
   - **Select a role** → **Cloud Datastore** → **Cloud Datastore User** seçin
   - **+ ADD ANOTHER ROLE** tıklayın
   - **Select a role** → **Firebase** → **Firebase Admin** seçin
6. **CONTINUE** tıklayın
7. **DONE** tıklayın

### Adım 1.3: JSON Key Oluşturun

1. Oluşturduğunuz service account'a tıklayın
2. **KEYS** tab'ına tıklayın
3. **ADD KEY** → **Create new key** seçin
4. **Key type**: **JSON** seçili olmalı
5. **CREATE** butonuna tıklayın

✅ JSON dosyası otomatik olarak indirilecek (örn: `saveyourmoney-prod-123456-a1b2c3d4e5f6.json`)

### Adım 1.4: Key Dosyasını Doğru Yere Taşıyın

```bash
# İndirilen dosyayı firebase-setup klasörüne taşıyın ve yeniden adlandırın
mv ~/Downloads/saveyourmoney-prod-*.json /Users/mertkiziloglu/Desktop/SaveYourMoney/firebase-setup/serviceAccountKey.json
```

**Veya Manuel:**
1. İndirilen JSON dosyasını kopyalayın
2. `/Users/mertkiziloglu/Desktop/SaveYourMoney/firebase-setup/` klasörüne yapıştırın
3. Dosyayı **`serviceAccountKey.json`** olarak yeniden adlandırın

**⚠️ ÖNEMLİ:**
- Bu dosya **asla GitHub'a pushlanmamalı!** (zaten .gitignore'da)
- Bu dosya **private key** içerir, güvenli saklayın

---

## 2. Node Modules Kurulumu

Terminal açın ve şu komutları çalıştırın:

```bash
# Firebase-setup klasörüne gidin
cd /Users/mertkiziloglu/Desktop/SaveYourMoney/firebase-setup

# Node modules kurun
npm install
```

**Beklenen Çıktı:**
```
added 150 packages, and audited 151 packages in 15s

22 packages are looking for funding
  run `npm fund` for details

found 0 vulnerabilities
```

**✅ Kurulum başarılı!**

---

## 3. Bağlantı Testi

Firestore bağlantısını test edin:

```bash
npm run test-connection
```

**Beklenen Çıktı:**
```
✅ Firebase Admin initialized successfully!
📦 Project ID: saveyourmoney-prod-123456
🔗 Firestore connection is working!
✅ Connection test passed!
```

**❌ Eğer Hata Alırsanız:**

**Hata: "serviceAccountKey.json not found"**
```
❌ Error: serviceAccountKey.json not found!

Please follow these steps:
1. Go to GCP Console: https://console.cloud.google.com
2. Navigate to: IAM & Admin → Service Accounts
3. Click on your service account (or create one)
4. Go to "Keys" tab → "Add Key" → "Create new key"
5. Select "JSON" format
6. Save the downloaded file as "serviceAccountKey.json" in firebase-setup/ directory
```

**Çözüm:** Adım 1'deki talimatları tekrar kontrol edin.

**Hata: "Permission denied"**
```
Error: 7 PERMISSION_DENIED: Missing or insufficient permissions.
```

**Çözüm:** Service account'a **Cloud Datastore User** rolü eklediğinizden emin olun.

---

## 4. Mock Data Yükleme

Firestore'a dummy data yükleyin:

```bash
npm run insert-mock
```

**Yükleme İşlemi Başladı:**
```
🚀 Loading dummy data into Firestore...

📋 Loading services...
  ✅ Loaded 5 services
📊 Loading metrics...
  ✅ Loaded 150 metrics across 3 services
💡 Loading recommendations...
  ✅ Loaded 2 recommendations
🚀 Loading deployments...
  ✅ Loaded 3 deployments
💰 Loading cost tracking...
  ✅ Loaded 8 cost tracking records
🚨 Loading alerts...
  ✅ Loaded 3 alerts

✅ All dummy data loaded successfully!

📊 Summary:
  - Services: 5 documents
  - Metrics: 150 documents
  - Recommendations: 2 documents
  - Deployments: 3 documents
  - Cost Tracking: 8 documents
  - Alerts: 3 documents
  TOTAL: 171 documents
```

**⏱️ Tahmini Süre:** 30-60 saniye

**✅ Yükleme Tamamlandı!**

---

## 5. Firestore Console'da Doğrulama

### Adım 5.1: Firestore Console'a Gidin

1. Tarayıcınızda açın: https://console.cloud.google.com/firestore/data
2. Projenizi seçin (eğer seçili değilse)

### Adım 5.2: Collection'ları Kontrol Edin

Şu collection'ları görmelisiniz:

#### ✅ **services** (5 documents)
```
├── analyzer-service
├── code-generator-service
├── cpu-hungry-service
├── memory-leaker-service
└── db-connection-service
```

**Bir servise tıklayın ve şu alanları görmelisiniz:**
- `serviceName`: "analyzer-service"
- `serviceType`: "WEB_API"
- `status`: "running"
- `currentConfig`: { cpu, memory, replicas }
- `monitoring`: { enabled: true }

#### ✅ **metrics** (150 documents)

Rastgele bir dokümana tıklayın:
- `serviceName`: "analyzer-service"
- `timestamp`: Timestamp
- `cpuUsage`: 0.45
- `memoryUsageMb`: 512
- `requestLatencyP95`: 120

#### ✅ **recommendations** (2 documents)

Bir tavsiyeye tıklayın:
- `serviceName`: "cpu-hungry-service"
- `recommendationId`: "rec_20260203_001"
- `confidenceScore`: 0.92
- `severity`: "high"
- `kubernetesResources`: { cpuRequest, cpuLimit, ... }
- `costAnalysis`: { currentMonthlyCost, recommendedMonthlyCost, ... }

#### ✅ **deployments** (3 documents)

Bir deployment'a tıklayın:
- `deploymentId`: "deploy_20260201_001"
- `serviceName`: "analyzer-service"
- `status`: "completed"
- `pullRequest`: { url, prNumber, status }
- `performanceImpact`: { improvement }

#### ✅ **cost-tracking** (8 documents)

Bir maliyet kaydına tıklayın:
- `date`: Timestamp
- `totalMonthlyCost`: 245.80
- `services`: { analyzer-service, code-generator-service, ... }
- `trends`: { dailyChange, weeklyChange }

#### ✅ **alerts** (3 documents)

Bir alert'e tıklayın:
- `alertId`: "alert_20260203_001"
- `serviceName`: "memory-leaker-service"
- `alertType`: "MEMORY_LEAK"
- `severity`: "critical"
- `status`: "open"

---

## 6. Data Temizleme (Opsiyonel)

Eğer tüm veriyi silip yeniden yüklemek isterseniz:

### Adım 6.1: Tüm Veriyi Temizle

```bash
npm run clear-data
```

**Çıktı:**
```
🗑️  Clearing all Firestore collections...

Deleting services...
  ✅ Deleted 5 documents from services
Deleting metrics...
  ✅ Deleted 150 documents from metrics
Deleting recommendations...
  ✅ Deleted 2 documents from recommendations
Deleting deployments...
  ✅ Deleted 3 documents from deployments
Deleting cost-tracking...
  ✅ Deleted 8 documents from cost-tracking
Deleting alerts...
  ✅ Deleted 3 documents from alerts

✅ All collections cleared successfully!
```

### Adım 6.2: Yeniden Yükle

```bash
npm run insert-mock
```

---

## Troubleshooting

### Problem 1: "Cannot find module 'firebase-admin'"

**Hata:**
```
Error: Cannot find module 'firebase-admin'
```

**Çözüm:**
```bash
cd /Users/mertkiziloglu/Desktop/SaveYourMoney/firebase-setup
npm install
```

---

### Problem 2: "serviceAccountKey.json not found"

**Hata:**
```
❌ Error: serviceAccountKey.json not found!
```

**Çözüm:**
1. GCP Console'dan JSON key indirdiğinizden emin olun
2. Dosyanın adının **tam olarak** `serviceAccountKey.json` olduğundan emin olun
3. Dosyanın `firebase-setup/` klasöründe olduğundan emin olun

**Kontrol edin:**
```bash
ls -la firebase-setup/serviceAccountKey.json
```

---

### Problem 3: "Permission denied" / "7 PERMISSION_DENIED"

**Hata:**
```
Error: 7 PERMISSION_DENIED: Missing or insufficient permissions.
```

**Çözüm:**

1. GCP Console → **IAM & Admin** → **Service Accounts**
2. Service account'ınızı bulun (`firebase-admin@...`)
3. **Edit** (kalem ikonu) tıklayın
4. **+ ADD ANOTHER ROLE** tıklayın
5. Şu rolleri ekleyin:
   - **Cloud Datastore User**
   - **Firebase Admin**
6. **SAVE** tıklayın
7. Birkaç dakika bekleyin (permission propagation için)
8. Tekrar deneyin

---

### Problem 4: Firestore Database Bulunamadı

**Hata:**
```
Error: Firestore database does not exist
```

**Çözüm:**

1. GCP Console → **Firestore** gidin
2. Eğer database yoksa **CREATE DATABASE** tıklayın
3. **Select Native mode** seçin
4. **Location** seçin (örn: `us-central`)
5. **CREATE DATABASE** tıklayın
6. Database oluşana kadar bekleyin (1-2 dakika)
7. Tekrar deneyin

---

### Problem 5: "Quota exceeded" hatası

**Hata:**
```
Error: Quota exceeded for service
```

**Çözüm:**

1. GCP Console → **IAM & Admin** → **Quotas** gidin
2. Firestore quota'larını kontrol edin
3. Free tier kullanıyorsanız:
   - 50K read/day
   - 20K write/day
   - 20K delete/day
4. Eğer quota'yı aştıysanız, yarın tekrar deneyin veya billing upgrade yapın

---

### Problem 6: Network timeout hatası

**Hata:**
```
Error: ECONNREFUSED or Timeout
```

**Çözüm:**

1. İnternet bağlantınızı kontrol edin
2. Firewall/VPN kullanıyorsanız devre dışı bırakın
3. Proxy ayarlarınızı kontrol edin
4. Tekrar deneyin

---

## 📊 Yüklenen Data Özeti

| Collection | Doküman Sayısı | Açıklama |
|------------|----------------|----------|
| **services** | 5 | Servis metadata ve konfigürasyonları |
| **metrics** | 150 | Time-series metrik verileri (3 servis × 50 metrik) |
| **recommendations** | 2 | AI tarafından oluşturulan optimizasyon tavsiyeleri |
| **deployments** | 3 | Deployment geçmişi ve PR bilgileri |
| **cost-tracking** | 8 | Son 8 günün maliyet analizi |
| **alerts** | 3 | Performance alert'leri |
| **TOPLAM** | **171** | **Production-ready örnek data** |

---

## 🎯 Data Kullanım Örnekleri

### Dashboard'dan Data Okuma

Dashboard'unuz (`dashboard-ui/public/`) Firestore'dan şu şekilde data okuyacak:

```javascript
// Firebase initialization (already in your code)
firebase.initializeApp({
  projectId: "saveyourmoney-prod-123456"
});

const db = firebase.firestore();

// Get all services
const servicesSnapshot = await db.collection('services').get();
servicesSnapshot.docs.forEach(doc => {
  console.log(doc.id, doc.data());
});

// Get latest metrics
const metricsSnapshot = await db.collection('metrics')
  .where('serviceName', '==', 'analyzer-service')
  .orderBy('timestamp', 'desc')
  .limit(50)
  .get();
```

### Backend'den Data Yazma

Spring Boot servisleriniz Firestore'a şu şekilde data yazacak:

```java
@Autowired
private Firestore firestore;

// Save metric
CollectionReference metricsRef = firestore.collection("metrics");
Map<String, Object> metric = new HashMap<>();
metric.put("serviceName", "analyzer-service");
metric.put("cpuUsage", 0.45);
metric.put("timestamp", FieldValue.serverTimestamp());

metricsRef.add(metric).get();
```

---

## ✅ Başarılı Kurulum Kontrolü

Tüm adımlar başarıyla tamamlandıysa:

✅ `npm run test-connection` komutu başarılı
✅ `npm run insert-mock` komutu 171 doküman yükledi
✅ Firestore Console'da 6 collection görünüyor
✅ Her collection'da beklenen sayıda doküman var
✅ Dashboard açıldığında Firestore'dan data okuyor

---

## 🚀 Sonraki Adımlar

1. **Dashboard'u Test Edin**: `dashboard-ui/public/index.html` dosyasını açın
2. **API'leri Test Edin**: Analyzer service endpoints'lerini test edin
3. **Presentation Hazırlayın**: Firestore data'yı demo'da gösterin

---

## 📞 Destek

Sorun yaşarsanız:

1. Hata mesajını dikkatlice okuyun
2. Troubleshooting bölümünü kontrol edin
3. GCP Console'da Firestore Rules'ı kontrol edin
4. Firestore API'nin enabled olduğundan emin olun

---

**Hazırlayan**: Claude Sonnet 4.5
**Tarih**: 2026-02-03
**Versiyon**: 1.0
