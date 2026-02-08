# SaveYourMoney — Dashboard Developer Context

> Bu dosya dashboard-ui üzerinde çalışan ajanlar/geliştiriciler için yazılmıştır.
> Backend tamamen ayakta ve çalışıyor. Aşağıda tüm API endpoint'leri, dosya yapısı ve dikkat edilecek noktalar bulunur.

---

## Çalışan Servisler (localhost)

| Servis | Port | Status |
|--------|------|--------|
| analyzer-service | `8084` | ✅ UP |
| code-generator-service | `8085` | ✅ UP |
| cpu-hungry-service | `8081` | ✅ UP |
| memory-leaker-service | `8082` | ✅ UP |
| db-connection-service | `8083` | ✅ UP |
| **dashboard-ui** | `3000` | ✅ (python3 http.server) |

---

## Dashboard Dosya Yapısı

```
dashboard-ui/public/
├── index.html              ← Ana dashboard sayfası
├── deployments.html        ← Deployment yönetimi
├── cost-analysis.html      ← Maliyet analizi
├── recommendations.html    ← Öneriler sayfası
├── styles/
│   ├── main.css            ← Ana stiller
│   └── (diğer css)
└── services/
    ├── api.js              ← Backend API çağrıları (tüm fetch'ler burada)
    ├── app.js              ← Ana uygulama mantığı + ChartManager çağrıları
    ├── charts.js           ← Chart.js ile grafik render fonksiyonları
    ├── deployments.js      ← Deployment sayfası mantığı
    ├── anomaly-monitor.js  ← Anomali izleme
    ├── app-firestore.js    ← Firestore entegrasyonu (KULLANILMIYOR)
    └── firestore-api.js    ← Firestore API (KULLANILMIYOR)
```

> **NOT:** `app-firestore.js` ve `firestore-api.js` dosyaları ölü koddur — Firestore kaldırıldı. Bunlara dokunmayın.

---

## API Base URL

```javascript
// dashboard-ui/public/services/api.js, satır 3
const API_BASE_URL = 'http://localhost:8084/api';
const CODE_GEN_URL = 'http://localhost:8085/api';
```

---

## Backend API Endpoint'leri (Tam Liste)

### Çalışan Endpoint'ler ✅

| Method | URL | Açıklama | Response Formatı |
|--------|-----|----------|-----------------|
| `GET` | `/api/health` | Health check | `{"status":"UP","service":"analyzer-service"}` |
| `GET` | `/api/dashboard` | Dashboard özet verisi | Aşağıya bak |
| `POST` | `/api/analyze/{serviceName}` | Tek servis analizi | `ResourceRecommendation` objesi |
| `POST` | `/api/analyze-all` | 3 servisin tamamı | `Map<serviceName, ResourceRecommendation>` |
| `GET` | `/api/analysis-history/{serviceName}` | Analiz geçmişi | `List<AnalysisResult>` |
| `GET` | `/api/latest-analysis/{serviceName}` | Son analiz | `AnalysisResult` veya 404 |
| `GET` | `/api/metrics/{serviceName}?limit=N` | Servis metrikleri | `{"serviceName":"..", "snapshotCount":N, ...}` |
| `POST` | `/api/collect-metrics` | Manuel metrik toplama | `{"status":"success","message":"..."}` |
| `GET` | `/api/predict-costs/{serviceName}?daysAhead=N` | Maliyet tahmini (ML) | `CostForecast` objesi |
| `GET` | `/api/classify-workload/{serviceName}` | Workload sınıflandırma (ML) | `WorkloadProfile` objesi |
| `GET` | `/api/ai/insights/{serviceName}` | AI analiz raporu | Karma JSON (aşağıya bak) |
| `GET` | `/api/ai/overview` | Tüm servislerin AI özeti | Karma JSON |

### ⚠️ Var Olmayan Endpoint'ler (404 Döner)

Bu endpoint'ler `api.js`'te tanımlı ama **backend'de kontrolör yok**:

| URL | Durum |
|-----|-------|
| `/api/anomalies/active` | ❌ 404 — Controller oluşturulmadı |
| `/api/anomalies/{serviceName}` | ❌ 404 |
| `/api/anomalies/stats` | ❌ 404 |
| `/api/anomalies/{id}/resolve` | ❌ 404 |
| `/api/anomalies/timeline/{serviceName}` | ❌ 404 |

> Dashboard'daki anomali ile ilgili bölümler bu yüzden boş görünür. Anomaly servisi henüz oluşturulmadı.

---

## API Response Örnekleri

### `GET /api/dashboard`

```json
{
  "servicesAnalyzed": 3,
  "totalMonthlySavings": 104.0,
  "totalAnnualSavings": 1248.0,
  "cpuHungryService": {
    "id": 7,
    "serviceName": "cpu-hungry-service",
    "currentCpuRequest": "100m",
    "currentCpuLimit": "200m",
    "currentMemoryRequest": "256Mi",
    "currentMemoryLimit": "512Mi",
    "recommendedCpuRequest": "100m",
    "recommendedMemoryRequest": "256Mi",
    "estimatedMonthlySavings": 36.5,
    "confidenceScore": 0.9,
    "p95CpuUsage": 0.071,
    "p95MemoryUsage": 0.421,
    "cpuThrottlingDetected": false,
    "memoryLeakDetected": false,
    "connectionPoolExhaustion": true
  },
  "memoryLeakerService": { ... },
  "dbConnectionService": { ... }
}
```

### `GET /api/ai/insights/{serviceName}`

```json
{
  "costPrediction": {
    "serviceName": "cpu-hungry-service",
    "forecastDays": 30,
    "currentMonthlyCost": 85.0,
    "predictedMonthlyCost": 89.25,
    "trend": "INCREASING",
    "dailyForecasts": [ ... ]
  },
  "workloadProfile": {
    "serviceName": "cpu-hungry-service",
    "pattern": "CPU_INTENSIVE",
    "workloadType": "COMPUTE_BOUND",
    "recommendedStrategy": "HPA with CPU-based scaling",
    "estimatedSavings": 15.0
  },
  "resourceRecommendation": { ... },
  "summary": {
    "serviceName": "cpu-hungry-service",
    "aiModelsUsed": "Holt-Winters Forecasting, K-Means Classification, Statistical Anomaly Detection"
  }
}
```

---

## Bilinen Sorunlar

1. **Anomaly endpoint'leri yok** — `/api/anomalies/*` backend'de oluşturulmadı. Dashboard'daki anomali bölümleri boş kalır.

2. **`ChartManager.renderCostComparison is not a function`** — `charts.js`'teki bazı fonksiyon isimleri `app.js`'ten çağrılanlarla uyuşmuyor olabilir. `charts.js`'i kontrol edin.

3. **CORS** — Dashboard `localhost:3000`'den API'ye `localhost:8084`'e çağrı yapar. Tarayıcıda CORS hatası görmezsiniz çünkü Spring Boot'un default ayarı buna izin verir. Ama production'da (farklı domain) `CorsConfig.java` gerekir.

4. **Rate Limiting** — Analyzer service'te Resilience4j rate limiting aktif. Çok hızlı art arda istek atarsanız **429 Too Many Requests** alırsınız:
   - Standard: 20 req/sec
   - Analysis: 5 req/10 sec
   - Auth: 10 req/min

5. **AI endpoints URL farkı** — `api.js`'te `/api/ai-insights/` yok. Backend'deki gerçek URL: `/api/ai/insights/{serviceName}` ve `/api/ai/overview`. Frontend'den çağırmak için `api.js`'e eklenmeli.

---

## Teknoloji Stacki

- **HTML**: Vanilla HTML5
- **CSS**: Custom CSS (TailwindCSS YOK — user rule: Bootstrap/Foundation/Bulma yasak)
- **JS**: Vanilla JavaScript, Chart.js (CDN)
- **Backend**: Spring Boot 3.2.2, Java 17+ (şu an JDK 25 ile çalışıyor)
- **Database**: H2 in-memory (her restart'ta sıfırlanır)

---

## Dashboard'u Yeniden Başlatma

Dashboard'u güncelledikten sonra yenilemeniz yeterlidir — hot reload yoktur. Dosyaları kaydedin ve tarayıcıda F5 yapın.

```bash
# Dashboard server zaten port 3000'de çalışıyor
# Eğer durmuşsa tekrar başlatın:
cd /Users/mertkiziloglu/Desktop/SaveYourMoney/dashboard-ui/public
python3 -m http.server 3000
```
