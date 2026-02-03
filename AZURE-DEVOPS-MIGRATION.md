# 🔄 GitHub'dan Azure DevOps'a Proje Taşıma Rehberi

**SaveYourMoney** projesini GitHub'dan Azure DevOps Azure Repos'a taşıma rehberi.

---

## 📋 İçindekiler

1. [Azure DevOps Projesi Oluşturma](#1-azure-devops-projesi-oluşturma)
2. [Azure Repos'a Kod Aktarma](#2-azure-reposa-kod-aktarma)
3. [Azure Pipelines Kurulumu](#3-azure-pipelines-kurulumu)
4. [Azure DevOps için GCP Deployment](#4-azure-devops-için-gcp-deployment)
5. [GitHub Repository'yi Temizleme](#5-github-repositoryyi-temizleme)

---

## 1. Azure DevOps Projesi Oluşturma

### 1.1 Azure DevOps'a Giriş

1. Tarayıcınızda bu adresi açın: https://dev.azure.com
2. Microsoft hesabınızla **Sign in** yapın
3. İlk kez kullanıyorsanız **"Create new organization"** tıklayın
4. Organization adı girin (örn: `saveyourmoney-org`)
5. **Continue** tıklayın

### 1.2 Yeni Proje Oluşturma

1. **"+ New project"** butonuna tıklayın
2. Proje bilgilerini doldurun:
   - **Project name**: `SaveYourMoney`
   - **Description**: `AI-powered resource optimization platform`
   - **Visibility**: `Private` (önerilir) veya `Public`
   - **Version control**: `Git` (**ÖNEMLİ: Git seçin!**)
   - **Work item process**: `Agile`
3. **Create** butonuna tıklayın

✅ Projeniz oluşturuldu! Şimdi kod aktarımına geçelim.

---

## 2. Azure Repos'a Kod Aktarma

### Yöntem A: Azure DevOps UI ile Import (ÖNERİLEN - En Kolay)

#### Adım 1: Azure Repos'da Import Başlatma

1. Azure DevOps projenizde sol menüden **Repos** tıklayın
2. Üst tarafta repository dropdown'dan **"Import repository"** seçin
3. Import ekranı açılacak

#### Adım 2: GitHub Repository Bilgilerini Girme

**Clone URL**: `https://github.com/mertkiziloglu/SaveYourMoney.git`

**Requires authentication**: GitHub repo private ise ✅ işaretleyin, public ise boş bırakın

**Eğer private ise:**
- **Username**: GitHub kullanıcı adınız
- **Password/PAT**: GitHub Personal Access Token

  **GitHub PAT oluşturma:**
  1. GitHub'da: Settings → Developer settings → Personal access tokens → Tokens (classic)
  2. **Generate new token (classic)** tıklayın
  3. **Note**: `Azure DevOps Import`
  4. **Expiration**: `90 days` veya istediğiniz süre
  5. **Scopes**: ✅ `repo` (Full control of private repositories)
  6. **Generate token** tıklayın
  7. Token'ı kopyalayın (bir daha göremezsiniz!)

**Repository name**: `SaveYourMoney` (default bırakabilirsiniz)

4. **Import** butonuna tıklayın

⏳ Import işlemi başladı (1-3 dakika sürer)

✅ İşlem tamamlandığında tüm kodunuz, commit history'si ve branch'ler Azure Repos'ta olacak!

---

### Yöntem B: Git Command Line ile Taşıma (Alternatif)

Eğer terminal kullanmak isterseniz:

```bash
# 1. Azure DevOps'tan remote URL'i kopyalayın
# Repos → Files → Clone → Copy URL
# Örnek: https://saveyourmoney-org@dev.azure.com/saveyourmoney-org/SaveYourMoney/_git/SaveYourMoney

# 2. Proje dizininizde:
cd /Users/mertkiziloglu/Desktop/SaveYourMoney

# 3. Azure DevOps remote ekleyin
git remote add azure https://saveyourmoney-org@dev.azure.com/saveyourmoney-org/SaveYourMoney/_git/SaveYourMoney

# 4. Azure DevOps'a push edin
git push azure main --all

# 5. Tag'leri de push edin (varsa)
git push azure --tags

# 6. GitHub remote'u kaldırın (opsiyonel)
git remote remove origin

# 7. Azure'u default remote yapın
git remote rename azure origin
```

---

## 3. Azure Pipelines Kurulumu

Azure Pipelines, GitHub Actions'a benzer CI/CD sistemidir.

### 3.1 Azure Pipeline Dosyası Oluşturma

Azure DevOps'ta **Pipelines** → **Create Pipeline** yerine, repository'nizde `azure-pipelines.yml` oluşturacağız.

**Proje ana dizininde `azure-pipelines.yml` oluşturun:**

```yaml
# Azure DevOps Pipeline for SaveYourMoney
trigger:
  branches:
    include:
      - main
      - develop
  paths:
    exclude:
      - README.md
      - docs/*

pool:
  vmImage: 'ubuntu-latest'

variables:
  MAVEN_CACHE_FOLDER: $(Pipeline.Workspace)/.m2/repository
  MAVEN_OPTS: '-Dmaven.repo.local=$(MAVEN_CACHE_FOLDER)'

stages:
  - stage: Build
    displayName: 'Build All Services'
    jobs:
      - job: BuildAnalyzerService
        displayName: 'Build Analyzer Service'
        steps:
          - task: Maven@3
            inputs:
              mavenPomFile: 'analyzer-service/pom.xml'
              goals: 'clean package'
              options: '-DskipTests'
              publishJUnitResults: false
              javaHomeOption: 'JDKVersion'
              jdkVersionOption: '1.17'
              mavenVersionOption: 'Default'

          - task: Docker@2
            displayName: 'Build Docker Image'
            inputs:
              containerRegistry: 'GCP Container Registry'
              repository: '$(GCP_PROJECT_ID)/analyzer-service'
              command: 'build'
              Dockerfile: 'analyzer-service/Dockerfile'
              tags: |
                $(Build.BuildId)
                latest

      - job: BuildCodeGeneratorService
        displayName: 'Build Code Generator Service'
        steps:
          - task: Maven@3
            inputs:
              mavenPomFile: 'code-generator-service/pom.xml'
              goals: 'clean package'
              options: '-DskipTests'
              publishJUnitResults: false
              javaHomeOption: 'JDKVersion'
              jdkVersionOption: '1.17'
              mavenVersionOption: 'Default'

          - task: Docker@2
            displayName: 'Build Docker Image'
            inputs:
              containerRegistry: 'GCP Container Registry'
              repository: '$(GCP_PROJECT_ID)/code-generator-service'
              command: 'build'
              Dockerfile: 'code-generator-service/Dockerfile'
              tags: |
                $(Build.BuildId)
                latest

      - job: BuildDemoServices
        displayName: 'Build Demo Services'
        steps:
          - script: |
              cd demo-services/cpu-hungry-service
              mvn clean package -DskipTests
              cd ../memory-leaker-service
              mvn clean package -DskipTests
              cd ../db-connection-service
              mvn clean package -DskipTests
            displayName: 'Maven Build All Demo Services'

  - stage: Test
    displayName: 'Run Tests'
    dependsOn: Build
    jobs:
      - job: UnitTests
        displayName: 'Run Unit Tests'
        steps:
          - task: Maven@3
            inputs:
              mavenPomFile: 'analyzer-service/pom.xml'
              goals: 'test'
              publishJUnitResults: true
              testResultsFiles: '**/surefire-reports/TEST-*.xml'
              javaHomeOption: 'JDKVersion'
              jdkVersionOption: '1.17'

  - stage: Deploy
    displayName: 'Deploy to GCP'
    dependsOn: Test
    condition: and(succeeded(), eq(variables['Build.SourceBranch'], 'refs/heads/main'))
    jobs:
      - deployment: DeployToGCP
        displayName: 'Deploy to GCP Cloud Run'
        environment: 'production'
        strategy:
          runOnce:
            deploy:
              steps:
                - script: echo "Deploy to GCP Cloud Run"
                  displayName: 'Deploy Services'
```

### 3.2 Azure Pipeline'ı Etkinleştirme (UI)

1. **Pipelines** → **Create Pipeline** tıklayın
2. **Where is your code?** → **Azure Repos Git** seçin
3. **Select a repository** → `SaveYourMoney` seçin
4. **Configure your pipeline** → **Existing Azure Pipelines YAML file** seçin
5. **Path**: `/azure-pipelines.yml` seçin
6. **Continue** tıklayın
7. **Run** tıklayın

✅ Pipeline çalışmaya başlayacak!

---

## 4. Azure DevOps için GCP Deployment

### 4.1 GCP Service Account Oluşturma

**GCP Console'da:**

1. **IAM & Admin** → **Service Accounts** → **CREATE SERVICE ACCOUNT**
2. **Service account name**: `azure-devops-deployer`
3. **Service account ID**: `azure-devops-deployer`
4. **CREATE AND CONTINUE**
5. **Grant this service account access to project**:
   - ✅ `Cloud Run Admin`
   - ✅ `Cloud Build Editor`
   - ✅ `Storage Admin`
6. **CONTINUE** → **DONE**
7. Oluşturulan service account'a tıklayın
8. **KEYS** tab → **ADD KEY** → **Create new key**
9. **JSON** seçin → **CREATE**
10. JSON dosyası indirilecek (örn: `azure-devops-deployer-xxxxx.json`)

### 4.2 Azure DevOps'ta Service Connection Oluşturma

1. **Project Settings** (sol altta ⚙️) → **Service connections**
2. **New service connection** → **Docker Registry** seçin
3. **Registry type**: **Others**
4. **Docker Registry**: `https://gcr.io`
5. **Docker ID**: `_json_key`
6. **Docker Password**: Service account JSON dosyasının **tüm içeriğini** buraya yapıştırın
7. **Service connection name**: `GCP Container Registry`
8. **Save**

### 4.3 Azure Pipelines Deployment Script

**`scripts/azure-deploy-to-gcp.sh`** oluşturun:

```bash
#!/bin/bash
set -e

echo "🚀 Azure DevOps -> GCP Cloud Run Deployment"

PROJECT_ID="${GCP_PROJECT_ID}"
REGION="us-central1"

echo "Project: $PROJECT_ID"
echo "Region: $REGION"

# Authenticate with GCP
echo $GCP_SERVICE_ACCOUNT_KEY | base64 -d > ${HOME}/gcp-key.json
gcloud auth activate-service-account --key-file ${HOME}/gcp-key.json
gcloud config set project $PROJECT_ID

# Deploy services
SERVICES=(
  "analyzer-service:8084:1Gi:1"
  "code-generator-service:8085:1Gi:1"
  "cpu-hungry-service:8081:512Mi:1"
  "memory-leaker-service:8082:512Mi:1"
  "db-connection-service:8083:512Mi:1"
)

for service_info in "${SERVICES[@]}"; do
  IFS=':' read -r service port memory cpu <<< "$service_info"

  echo "Deploying $service..."

  gcloud run deploy $service \
    --image gcr.io/$PROJECT_ID/$service:latest \
    --region $REGION \
    --platform managed \
    --allow-unauthenticated \
    --port $port \
    --memory $memory \
    --cpu $cpu \
    --min-instances 0 \
    --max-instances 10 \
    --set-env-vars "SERVER_PORT=$port,SPRING_PROFILES_ACTIVE=prod,GOOGLE_CLOUD_PROJECT=$PROJECT_ID"
done

echo "✅ Deployment completed!"
```

### 4.4 Azure DevOps Variables (Secrets)

1. **Pipelines** → Pipeline'ınızı seçin → **Edit**
2. **Variables** → **New variable**

**Variable 1:**
- **Name**: `GCP_PROJECT_ID`
- **Value**: `saveyourmoney-prod-123456` (kendi project ID'niz)
- **Keep this value secret**: Hayır

**Variable 2:**
- **Name**: `GCP_SERVICE_ACCOUNT_KEY`
- **Value**: Service account JSON dosyasının base64 encoded hali
- **Keep this value secret**: ✅ Evet

```bash
# Base64 encode etmek için (macOS/Linux):
base64 -i azure-devops-deployer-xxxxx.json | pbcopy
```

3. **Save**

---

## 5. GitHub Repository'yi Temizleme

### Seçenek A: GitHub Repo'yu Silme

1. GitHub'da repository sayfasına gidin: https://github.com/mertkiziloglu/SaveYourMoney
2. **Settings** tab'ına tıklayın
3. En alta scroll edin → **Danger Zone** bölümü
4. **Delete this repository** tıklayın
5. Repository adını yazın: `mertkiziloglu/SaveYourMoney`
6. **I understand the consequences, delete this repository** tıklayın

### Seçenek B: GitHub Repo'yu Archive Etme (Önerilir)

1. GitHub'da repository sayfasına gidin
2. **Settings** → **Danger Zone**
3. **Archive this repository** tıklayın
4. **I understand, archive this repository** onaylayın

✅ Repository artık read-only ve archive durumda

### Seçenek C: README'ye Azure DevOps Linki Ekleme

GitHub repo'yu tutmak ama yönlendirmek isterseniz:

**README.md'yi şu şekilde güncelleyin:**

```markdown
# SaveYourMoney

> ⚠️ **This project has moved to Azure DevOps!**
>
> New repository: https://dev.azure.com/saveyourmoney-org/SaveYourMoney/_git/SaveYourMoney
>
> Please use Azure DevOps repository for latest updates and contributions.

---

[View project on Azure DevOps →](https://dev.azure.com/saveyourmoney-org/SaveYourMoney)
```

---

## 6. Yerel Git Remote Güncelleme

Yerel bilgisayarınızda:

```bash
# Mevcut remote'ları kontrol edin
git remote -v

# GitHub remote'unu kaldırın
git remote remove origin

# Azure DevOps remote'unu ekleyin
git remote add origin https://saveyourmoney-org@dev.azure.com/saveyourmoney-org/SaveYourMoney/_git/SaveYourMoney

# Test edin
git fetch origin
git pull origin main

# Yeni değişiklikleri artık Azure DevOps'a push edebilirsiniz
git push origin main
```

---

## 📋 Taşıma Checklist

- [ ] Azure DevOps organization oluşturuldu
- [ ] Azure DevOps projesi oluşturuldu
- [ ] GitHub'dan Azure Repos'a kod import edildi
- [ ] azure-pipelines.yml oluşturuldu
- [ ] GCP Service Account oluşturuldu
- [ ] Azure DevOps Service Connection ayarlandı
- [ ] Azure Pipeline variables (secrets) eklendi
- [ ] Pipeline çalıştırıldı ve başarılı oldu
- [ ] Yerel git remote güncellendi
- [ ] GitHub repository archive/silindi

---

## 🎯 Azure DevOps vs GitHub Karşılaştırması

| Özellik | GitHub | Azure DevOps |
|---------|--------|--------------|
| **Version Control** | Git | Git (Azure Repos) |
| **CI/CD** | GitHub Actions | Azure Pipelines |
| **Issue Tracking** | Issues | Azure Boards |
| **Container Registry** | GitHub Packages | Azure Container Registry |
| **Code Review** | Pull Requests | Pull Requests |
| **Wiki** | GitHub Wiki | Azure Wiki |
| **Free Tier** | Public repos unlimited | 5 users free (private repos) |

---

## 💡 Azure DevOps Avantajları

1. **Azure Integration**: Azure Cloud ile native entegrasyon
2. **Azure Boards**: Gelişmiş proje yönetimi (Scrum/Kanban)
3. **Azure Test Plans**: Test case yönetimi
4. **Azure Artifacts**: Package management (Maven, npm, NuGet)
5. **Enterprise Features**: Daha gelişmiş güvenlik ve compliance
6. **Unlimited Private Repos**: 5 kullanıcıya kadar ücretsiz

---

## 🆘 Troubleshooting

### Problem: Import başarısız

**Çözüm:**
- GitHub Personal Access Token'ın `repo` scope'una sahip olduğundan emin olun
- Token'ın expire olmadığını kontrol edin
- Public repo ise authentication'ı devre dışı bırakın

### Problem: Azure Pipeline build başarısız

**Çözüm:**
- **Pipelines → Build** → Hata loglarını kontrol edin
- Java 17 ve Maven version'larını kontrol edin
- pom.xml dosyalarını kontrol edin

### Problem: GCP deployment başarısız

**Çözüm:**
- Service Account JSON'ın doğru olduğundan emin olun
- GCP Project ID'nin doğru olduğunu kontrol edin
- Cloud Run API'nin enabled olduğunu kontrol edin

---

## 📞 Destek

- **Azure DevOps Docs**: https://docs.microsoft.com/azure/devops
- **Azure Repos**: https://docs.microsoft.com/azure/devops/repos
- **Azure Pipelines**: https://docs.microsoft.com/azure/devops/pipelines

---

**Hazırlayan**: Claude Sonnet 4.5
**Tarih**: 2026-02-03
**Versiyon**: 1.0
