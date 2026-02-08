# SaveYourMoney — GCP Console Deployment & Gemini Entegrasyonu

> Bu rehber **gcloud CLI kullanmadan**, tamamen **GCP Console UI** üzerinden projeyi deploy etmeyi ve Gemini API'yi analyzer service'e bağlamayı adım adım anlatır.

---

## İçindekiler

1. [Ön Hazırlık](#1-ön-hazırlık)
2. [GCP Projesi Oluşturma](#2-gcp-projesi-oluşturma)
3. [Gerekli API'leri Açma](#3-gerekli-apileri-açma)
4. [Artifact Registry ile Docker Repository](#4-artifact-registry-ile-docker-repository)
5. [Cloud Build ile Image Oluşturma](#5-cloud-build-ile-image-oluşturma)
6. [Cloud Run'a Deploy Etme](#6-cloud-runa-deploy-etme)
7. [Gemini API Entegrasyonu](#7-gemini-api-entegrasyonu)
8. [Dashboard UI'ın Analyzer'a Bağlanması](#8-dashboard-uının-analyzera-bağlanması)
9. [Secret Manager ile Güvenlik](#9-secret-manager-ile-güvenlik)
10. [Demo & Test](#10-demo--test)

---

## 1. Ön Hazırlık

### Gerekli Hesaplar
- **Google Cloud hesabı** (billing aktif olmalı)
- **GitHub hesabı** (repo burada olacak)

### Yerel Gereksinimler
- Docker Desktop (image build için — alternatif olarak Cloud Build kullanılacak)
- Git (repo push için)

### Mimari Özet

```
┌─────────────────────────────────────────────────────────┐
│                    Google Cloud Platform                  │
│                                                          │
│  ┌──────────────┐    ┌──────────────┐                   │
│  │ Dashboard UI │───▶│   Analyzer   │◀── Gemini API     │
│  │ (Cloud Run)  │    │ (Cloud Run)  │                   │
│  │  port 8080   │    │  port 8084   │                   │
│  └──────────────┘    └──────┬───────┘                   │
│                             │                            │
│              ┌──────────────┼──────────────┐            │
│              ▼              ▼              ▼            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │ CPU-Hungry   │ │ Memory-Leak  │ │ DB-Connection │   │
│  │ (Cloud Run)  │ │ (Cloud Run)  │ │ (Cloud Run)   │   │
│  │  port 8081   │ │  port 8082   │ │  port 8083    │   │
│  └──────────────┘ └──────────────┘ └──────────────┘   │
└─────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> Cloud Run, container'ın `/PORT` environment variable'ındaki portu dinlemesini bekler. Her serviste `PORT` env var'ını doğru ayarlamak kritiktir.

---

## 2. GCP Projesi Oluşturma

1. [console.cloud.google.com](https://console.cloud.google.com) adresine git
2. Sol üstteki **proje seçici** dropdown'a tıkla → **"New Project"**
3. Ayarlar:
   - **Project Name:** `SaveYourMoney`
   - **Project ID:** `saveyourmoney-hackathon` (benzersiz olmalı, yoksa son ek ekle)
   - **Billing Account:** Aktif billing hesabını seç
4. **"Create"** butonuna bas
5. Oluşturulunca sol üstte projenin seçili olduğundan emin ol

---

## 3. Gerekli API'leri Açma

Sol menüden **"APIs & Services" → "Enable APIs and Services"** seçeneğine git.

Aşağıdaki API'leri tek tek ara ve **"Enable"** butonuna bas:

| API | Arama Terimi | Ne İçin |
|-----|-------------|---------|
| Cloud Run Admin API | `Cloud Run` | Servisleri deploy etmek |
| Artifact Registry API | `Artifact Registry` | Docker image'ları depolamak |
| Cloud Build API | `Cloud Build` | GitHub'dan image build etmek |
| Secret Manager API | `Secret Manager` | API key ve JWT secret saklamak |
| Generative Language API | `Generative Language` | Gemini API kullanmak |

> [!TIP]
> Her API'yi aradıktan sonra **"Enable"** butonuna bas ve sayfanın yüklenmesini bekle. Aktif olduğunda başlık altında "API enabled" yazısı görünür.

---

## 4. Artifact Registry ile Docker Repository

Docker image'larını depolamak için bir repository oluştur:

1. Sol menü → **"Artifact Registry"**
2. Üstte **"+ CREATE REPOSITORY"** butonuna tıkla
3. Ayarlar:
   - **Name:** `saveyourmoney`
   - **Format:** `Docker`
   - **Mode:** `Standard`
   - **Location type:** `Region`
   - **Region:** `europe-west1` (veya sana en yakın region)
4. **"Create"** butonuna bas

> [!NOTE]
> Oluşan repository URL'i şu formatta olacak:
> `europe-west1-docker.pkg.dev/PROJECT_ID/saveyourmoney`
> Bu URL'i ileride image push ederken kullanacaksın.

---

## 5. Cloud Build ile Image Oluşturma

Bu adımda her servis için Docker image'ı build edeceksin. GitHub reposunu Cloud Build'e bağlayacaksın.

### 5a. GitHub Reposunu Bağlama

1. Sol menü → **"Cloud Build" → "Repositories (2nd Gen)"**
2. **"Create Host Connection"** butonuna tıkla
3. **GitHub** seç → **"Connect"** butonuna bas
4. GitHub'a yönlendirileceksin, **"Authorize Google Cloud Build"** onayla
5. Hangi repository'lere erişim vereceğini seç:
   - **"Only select repositories"** → `SaveYourMoney` reposunu seç
6. **"Install"** butonuna bas
7. GCP'ye dönünce bağlantı listesinde repoyu gör
8. **"Link Repository"** → GitHub connection ve repository seç → **"Link"**

### 5b. Her Servis İçin Build Trigger Oluşturma (Manuel Build)

Her servis için tek seferlik build yapacaksın. 6 servis var:

| # | Servis | Dockerfile Yolu | Port |
|---|--------|-----------------|------|
| 1 | analyzer-service | `analyzer-service/Dockerfile` | 8084 |
| 2 | dashboard-ui | `dashboard-ui/Dockerfile` | 8080 |
| 3 | cpu-hungry-service | `demo-services/cpu-hungry-service/Dockerfile` | 8081 |
| 4 | memory-leaker-service | `demo-services/memory-leaker-service/Dockerfile` | 8082 |
| 5 | db-connection-service | `demo-services/db-connection-service/Dockerfile` | 8083 |
| 6 | code-generator-service | `code-generator-service/Dockerfile` | 8085 |

**Her servis için şu adımları tekrarla:**

1. Sol menü → **"Cloud Build" → "Triggers"**
2. **"+ CREATE TRIGGER"** tıkla
3. Ayarlar:
   - **Name:** `build-analyzer-service` (servis adına göre değiştir)
   - **Event:** `Manual invocation`
   - **Source:** Bağladığın GitHub reposunu seç
   - **Branch:** `main`
   - **Configuration:** `Dockerfile`
   - **Dockerfile directory:** `analyzer-service` (servisin klasörü — tablodaki "Dockerfile Yolu"nun klasör kısmı)
   - **Image name:** `europe-west1-docker.pkg.dev/PROJECT_ID/saveyourmoney/analyzer-service:latest`

> [!CAUTION]
> **`PROJECT_ID`'yi kendi proje ID'nle değiştirmeyi unutma!** Örnek: `saveyourmoney-hackathon`

4. **"Create"** butonuna bas
5. Oluşan trigger'ın yanındaki **"RUN"** butonuna bas
6. Build loglarını izle — yeşil ✓ görünene kadar bekle (5-10 dk)

**Her servis için bu süreci tekrarla.** Toplamda 6 trigger ve 6 build olacak.

> [!TIP]
> Daha hızlı yol: Projenin kök dizinine aşağıdaki `cloudbuild.yaml` ekleyip tek komutla hepsini build edebilirsin. Ama UI'dan yapmak istiyorsan yukarıdaki yolu izle.

---

## 6. Cloud Run'a Deploy Etme

Image'lar Artifact Registry'de hazır olduktan sonra Cloud Run'a deploy et.

### Deployment Sırası

> [!IMPORTANT]
> Servislerin deploy sırası önemlidir. Önce demo servisleri, sonra analyzer, en son dashboard deploy edilmelidir. Çünkü analyzer demo servislerin URL'lerine ihtiyaç duyar, dashboard da analyzer'ın URL'ine ihtiyaç duyar.

```
1. cpu-hungry-service      (bağımsız — ilk deploy)
2. memory-leaker-service   (bağımsız — ilk deploy)
3. db-connection-service   (bağımsız — ilk deploy)
4. analyzer-service        (demo URL'lerine ihtiyaç duyar)
5. code-generator-service  (bağımsız)
6. dashboard-ui            (analyzer URL'ine ihtiyaç duyar)
```

### 6a. Demo Servisleri Deploy Etme (3 adet)

Her demo servis için:

1. Sol menü → **"Cloud Run"**
2. **"+ CREATE SERVICE"** tıkla
3. **"Deploy one revision from an existing container image"** seç
4. **"SELECT"** → **"Artifact Registry"** → image'ı seç (ör: `cpu-hungry-service:latest`)
5. Ayarlar:

| Alan | Değer |
|------|-------|
| **Service name** | `cpu-hungry-service` |
| **Region** | `europe-west1` |
| **Authentication** | `Allow unauthenticated invocations` |
| **CPU allocation** | `CPU is only allocated during request processing` |
| **Min instances** | `0` |
| **Max instances** | `2` |
| **Memory** | `512 MiB` |
| **CPU** | `1` |

6. **"Container, Volumes, Networking, Security"** bölümünü aç:
   - **Container port:** İlgili serviste port numarasını yaz:
     - cpu-hungry: `8081`
     - memory-leaker: `8082`
     - db-connection: `8083`
   - **Environment variables:**
     - `SPRING_PROFILES_ACTIVE` = `dev`

7. **"Create"** butonuna bas
8. Deploy tamamlanınca URL'i **not al** — şu formatta olacak:
   ```
   https://cpu-hungry-service-XXXXX-ew.a.run.app
   ```

> [!IMPORTANT]
> **Her 3 demo servisin URL'ini not al!** Analyzer service deploy ederken bu URL'lere ihtiyacın olacak.

### 6b. Analyzer Service Deploy Etme

1. **"Cloud Run" → "+ CREATE SERVICE"**
2. Image: `analyzer-service:latest` (Artifact Registry'den)
3. Ayarlar:

| Alan | Değer |
|------|-------|
| **Service name** | `analyzer-service` |
| **Region** | `europe-west1` |
| **Authentication** | `Allow unauthenticated invocations` |
| **Min instances** | `1` (cold start'ı önlemek için) |
| **Max instances** | `5` |
| **Memory** | `1 GiB` |
| **CPU** | `1` |

4. **Container port:** `8084`

5. **Environment Variables:**

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `APP_API_KEY` | (güçlü bir key üret, ör: `sym-prod-key-2026`) |
| `APP_SECURITY_JWT_SECRET` | (32+ karakter secret, ör: `sym-jwt-prod-secret-2026-very-long-key`) |
| `ANALYZER_SERVICES_CPU_HUNGRY_URL` | `https://cpu-hungry-service-XXXXX-ew.a.run.app` |
| `ANALYZER_SERVICES_MEMORY_LEAKER_URL` | `https://memory-leaker-service-XXXXX-ew.a.run.app` |
| `ANALYZER_SERVICES_DB_CONNECTION_URL` | `https://db-connection-service-XXXXX-ew.a.run.app` |
| `GEMINI_API_KEY` | (Adım 7'de alacağın key) |

> [!CAUTION]
> `XXXXX` kısmını gerçek Cloud Run URL'leriyle değiştir! Her demo servisin URL'ini Adım 6a'da not almıştın.

6. **"Create"** butonuna bas
7. Deploy URL'ini **not al**

### 6c. Dashboard UI Deploy Etme

Dashboard, nginx ile çalışır ve `/api/` isteklerini analyzer'a yönlendirir. Cloud Run'da her servis kendi URL'ine sahip olduğu için nginx proxy yerine, dashboard'un JavaScript kodundaki API base URL'ini değiştirmek gerekir.

**Alternatif yaklaşım (önerilen):** Dashboard frontend'indeki API çağrıları doğrudan analyzer service URL'ine yapılacak.

1. Önce **dashboard kodunda API URL'ini güncelle:**

   `dashboard-ui/public/services/api.js` dosyasında (veya API çağrılarının yapıldığı yerde) base URL'i analyzer'ın Cloud Run URL'i ile değiştir:

   ```javascript
   const API_BASE = 'https://analyzer-service-XXXXX-ew.a.run.app/api';
   ```

2. Değişikliği commit'le ve Cloud Build'den dashboard image'ını yeniden build et

3. **"Cloud Run" → "+ CREATE SERVICE"**
4. Image: `dashboard-ui:latest`
5. Ayarlar:

| Alan | Değer |
|------|-------|
| **Service name** | `dashboard-ui` |
| **Region** | `europe-west1` |
| **Authentication** | `Allow unauthenticated invocations` |
| **Min instances** | `0` |
| **Max instances** | `3` |
| **Memory** | `256 MiB` |
| **CPU** | `1` |
| **Container port** | `8080` |

6. **"Create"** butonuna bas
7. Dashboard URL'i şu formatta: `https://dashboard-ui-XXXXX-ew.a.run.app`

---

## 7. Gemini API Entegrasyonu

Bu bölüm analyzer service'e Gemini 2.0 Flash'ı bağlayarak AI-powered analiz raporları üretmesini sağlar.

### 7a. Gemini API Key Alma

1. [aistudio.google.com/apikey](https://aistudio.google.com/apikey) adresine git
2. **"Create API key"** butonuna tıkla
3. Proje olarak **SaveYourMoney** projesini seç
4. Oluşan API key'i **kopyala ve güvenli bir yere kaydet**

> [!CAUTION]
> API key'ini asla commit'leme! Environment variable olarak kullan.

### 7b. Analyzer Service'e Gemini Client Ekleme

Analyzer projesine aşağıdaki değişiklikleri yap:

#### 1. `pom.xml`'e Bağımlılık Ekle

```xml
<!-- Gemini API Client -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-vertexai</artifactId>
    <version>1.14.0</version>
</dependency>
```

> [!NOTE]
> Alternatif olarak REST API ile de çağırabilirsin (aşağıdaki servis sınıfı REST yaklaşımını kullanır — ek dependency gerektirmez).

#### 2. Yeni Servis Sınıfı: `GeminiInsightService.java`

`analyzer-service/src/main/java/com/hackathon/analyzer/service/` altına:

```java
package com.hackathon.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Gemini 2.0 Flash ile AI-powered optimizasyon raporu üretir.
 * REST API kullanır — ek SDK bağımlılığı gerektirmez.
 */
@Slf4j
@Service
public class GeminiInsightService {

    private final WebClient webClient;
    private final String apiKey;

    public GeminiInsightService(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = apiKey;
    }

    /**
     * Servis metrikleri hakkında Gemini'den optimizasyon raporu iste.
     */
    public String generateInsight(String serviceName,
                                   double cpuUsage,
                                   double memoryUsageMb,
                                   double monthlyCost,
                                   double confidenceScore) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key not configured — skipping AI insight");
            return "Gemini API key is not configured. "
                 + "Set GEMINI_API_KEY environment variable to enable AI insights.";
        }

        String prompt = buildPrompt(serviceName, cpuUsage, memoryUsageMb,
                                     monthlyCost, confidenceScore);

        try {
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                ),
                "generationConfig", Map.of(
                    "temperature", 0.3,
                    "maxOutputTokens", 1024,
                    "topP", 0.8
                )
            );

            String response = webClient.post()
                .uri("/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    log.error("Gemini API call failed: {}", e.getMessage());
                    return Mono.just("{\"error\":\"" + e.getMessage() + "\"}");
                })
                .block();

            return extractTextFromResponse(response);

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return "AI insight temporarily unavailable: " + e.getMessage();
        }
    }

    private String buildPrompt(String serviceName, double cpu,
                                double memory, double cost, double confidence) {
        return String.format("""
            Sen bir Kubernetes kaynak optimizasyonu uzmanısın.
            Aşağıdaki mikroservis metriklerini analiz et ve Türkçe olarak
            kısa bir optimizasyon raporu yaz (maksimum 5 madde):

            Servis Adı: %s
            CPU Kullanımı: %.1f%%
            Memory Kullanımı: %.1f MB
            Aylık Maliyet: $%.2f
            Güven Skoru: %.0f%%

            Rapor formatı:
            1. Mevcut Durum Özeti (1 cümle)
            2. Tespit Edilen Sorunlar (varsa)
            3. Optimizasyon Önerileri (somut CPU/Memory değerleriyle)
            4. Tahmini Tasarruf
            5. Öncelik Seviyesi (Düşük/Orta/Yüksek/Kritik)
            """, serviceName, cpu, memory, cost, confidence * 100);
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            // Basit JSON parsing — candidates[0].content.parts[0].text
            int textStart = jsonResponse.indexOf("\"text\":");
            if (textStart == -1) return "Unable to parse Gemini response";

            int valueStart = jsonResponse.indexOf("\"", textStart + 7) + 1;
            int valueEnd = jsonResponse.indexOf("\"", valueStart);

            // Handle escaped quotes
            while (valueEnd > 0 && jsonResponse.charAt(valueEnd - 1) == '\\') {
                valueEnd = jsonResponse.indexOf("\"", valueEnd + 1);
            }

            return jsonResponse.substring(valueStart, valueEnd)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return jsonResponse;
        }
    }
}
```

#### 3. Controller'a Gemini Endpoint Ekle

`AnalyzerController.java`'daki mevcut `getAIInsights` metoduna Gemini çağrısını ekle:

```java
// GeminiInsightService'i inject et (constructor'a ekle):
private final GeminiInsightService geminiInsightService;

// getAIInsights metodunun sonuna (return'den önce) ekle:
try {
    String geminiReport = geminiInsightService.generateInsight(
        serviceName,
        latestAnalysis.map(a -> a.getConfidenceScore() * 100).orElse(0.0),
        0.0, // memory — metricsCollector'dan çekilebilir
        latestAnalysis.map(AnalysisResult::getEstimatedMonthlySavings).orElse(0.0),
        latestAnalysis.map(AnalysisResult::getConfidenceScore).orElse(0.0)
    );
    insights.put("geminiInsight", geminiReport);
} catch (Exception e) {
    log.error("Gemini insight failed: {}", e.getMessage());
    insights.put("geminiInsight", "AI insight unavailable");
}
```

#### 4. `application.yml`'a Config Ekle

```yaml
# Gemini API Configuration
gemini:
  api-key: ${GEMINI_API_KEY:}
```

#### 5. Cloud Run'da Environment Variable Güncelle

1. **Cloud Run** → `analyzer-service` → **"Edit & Deploy New Revision"**
2. **"Variables & Secrets"** sekmesi
3. **"+ Add Variable":**
   - **Name:** `GEMINI_API_KEY`
   - **Value:** Adım 7a'da aldığın API key
4. **"Deploy"** butonuna bas

### 7c. Gemini Entegrasyonu Doğrulama

Deploy tamamlandıktan sonra:

```
GET https://analyzer-service-XXXXX-ew.a.run.app/api/ai-insights/cpu-hungry-service
```

Response'da `geminiInsight` alanında Gemini'nin ürettiği Türkçe raporu görmelisin.

---

## 8. Dashboard UI'ın Analyzer'a Bağlanması

Cloud Run'da her servis farklı URL'de çalışır. Dashboard'un API çağrılarını doğru URL'e yönlendirmek için:

### Seçenek A: JavaScript'te API Base URL Değiştirme (Basit)

`dashboard-ui/public/services/api.js` dosyasında:

```javascript
// Yerel geliştirme
// const API_BASE = '/api';

// Cloud Run production
const API_BASE = 'https://analyzer-service-XXXXX-ew.a.run.app/api';
```

### Seçenek B: Nginx Proxy ile (Cloud Run Konteyner İçi)

Dashboard Dockerfile'ındaki nginx config'de proxy_pass'ı güncelleyebilirsin, ama Cloud Run'da container-to-container networking farklı çalıştığı için **Seçenek A önerilir**.

### CORS Sorunu Çözümü

Dashboard farklı bir domain'den analyzer'a istek atacağı için CORS ayarı gerekir. Analyzer service'e CORS config'i ekle:

```java
// config/CorsConfig.java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://dashboard-ui-XXXXX-ew.a.run.app")
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }
}
```

---

## 9. Secret Manager ile Güvenlik

Production'da environment variable yerine Secret Manager kullan:

1. Sol menü → **"Secret Manager"**
2. **"+ CREATE SECRET"** butonuna tıkla
3. 3 secret oluştur:

| Secret Name | Value |
|-------------|-------|
| `sym-api-key` | `sym-prod-key-2026` |
| `sym-jwt-secret` | `sym-jwt-prod-secret-2026-very-long-key` |
| `sym-gemini-key` | Gemini API key'in |

4. Her secret için **"Version"** otomatik oluşturulacak

### Cloud Run'da Secret'ları Bağlama

1. **Cloud Run** → `analyzer-service` → **"Edit & Deploy New Revision"**
2. **"Variables & Secrets"** → **"Reference a Secret"**
3. Ayarlar:

| Environment Variable | Secret | Version |
|---------------------|--------|---------|
| `APP_API_KEY` | `sym-api-key` | `latest` |
| `APP_SECURITY_JWT_SECRET` | `sym-jwt-secret` | `latest` |
| `GEMINI_API_KEY` | `sym-gemini-key` | `latest` |

4. **"Deploy"** butonuna bas

> [!NOTE]
> Cloud Run service account'ına **Secret Manager Secret Accessor** rolü verilmesi gerekebilir:
> **IAM & Admin** → Service account'ı bul → **"Grant Access"** → Role: `Secret Manager Secret Accessor`

---

## 10. Demo & Test

### Hızlı Doğrulama Checklist

Deploya tamamlandıktan sonra tarayıcıda şu URL'leri kontrol et:

| Test | URL | Beklenen |
|------|-----|----------|
| Analyzer Health | `https://analyzer-service-XXX.run.app/api/health` | `{"status":"UP"}` |
| Dashboard | `https://dashboard-ui-XXX.run.app` | Dashboard sayfası |
| Swagger Docs | `https://analyzer-service-XXX.run.app/swagger-ui.html` | API dokümantasyonu |
| Demo CPU | `https://cpu-hungry-service-XXX.run.app/actuator/health` | `{"status":"UP"}` |
| Gemini Insight | `https://analyzer-service-XXX.run.app/api/ai-insights/cpu-hungry-service` | JSON + `geminiInsight` alanı |

### Hackathon Demo Senaryosu

1. **Dashboard'u aç** → 3 servisi gör
2. **"Analyze All Services"** butonuna bas → optimizasyon önerilerini göster
3. **Swagger UI'ı aç** → API'yi canlı demo yap
4. **Gemini insight endpoint'ini çağır** → AI-powered Türkçe raporu göster
5. **Cost comparison chart'ını göster** → tasarruf potansiyelini görselleştir

### Maliyet Tahmini

| Servis | Tahmini Aylık Maliyet |
|--------|----------------------|
| 6x Cloud Run (min=0, idle) | ~$0 (free tier) |
| Artifact Registry (6 image) | ~$0.50 |
| Gemini API (hackathon usage) | ~$0 (free tier: 60 req/min) |
| **Toplam** | **~$0.50/ay** |

> [!TIP]
> Google Cloud free tier Cloud Run'da ayda 2 milyon request ve 360.000 vCPU-saniye sunuyor. Hackathon demo'su bu limitlerin çok altında kalacaktır.

---

## Sorun Giderme

| Sorun | Çözüm |
|-------|-------|
| Cloud Run "Container failed to start" | Container port'un doğru ayarlandığından emin ol |
| 502 Bad Gateway | Min instances'ı 1 yap (cold start sorunu) |
| CORS hatası | CorsConfig.java ekle, dashboard URL'ini allowedOrigins'e yaz |
| Gemini 403 | API key'in doğru olduğundan ve Generative Language API'nin aktif olduğundan emin ol |
| Dashboard API çağrıları başarısız | API_BASE URL'inin analyzer'ın Cloud Run URL'i olduğunu kontrol et |
| "Permission denied" Secret Manager | Service account'a `Secret Manager Secret Accessor` rolü ver |
