# Firebase/Firestore Setup Guide

## 1. Google Cloud Platform'da Firebase Projesi Oluşturma

### Adım 1: Firebase Console'a Giriş
1. [Firebase Console](https://console.firebase.google.com/) adresine gidin
2. Google hesabınızla giriş yapın
3. "Add project" veya "Create a project" butonuna tıklayın

### Adım 2: Proje Oluşturma
1. **Project name**: `SaveYourMoney` yazın
2. **Continue** butonuna tıklayın
3. **Google Analytics**: İsteğe bağlı (kapatabilirsiniz)
4. **Create project** butonuna tıklayın

### Adım 3: Firestore Database Oluşturma
1. Sol menüden **Build > Firestore Database** seçin
2. **Create database** butonuna tıklayın
3. **Location**: En yakın bölgeyi seçin (örn: `europe-west3 (Frankfurt)`)
4. **Security rules**: **Test mode** seçin (geliştirme için)
   - ⚠️ **Production'da mutlaka güvenlik kurallarını güncelleyin!**
5. **Create** butonuna tıklayın

### Adım 4: Web App Oluşturma ve Config Alma
1. Project Overview sayfasında **Web** (</>) ikonuna tıklayın
2. **App nickname**: `SaveYourMoney Dashboard` yazın
3. **Register app** butonuna tıklayın
4. **Firebase SDK snippet** kısmında **Config** seçeneğini seçin
5. Aşağıdaki gibi bir config göreceksiniz:

```javascript
const firebaseConfig = {
  apiKey: "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  authDomain: "saveyourmoney-xxxxx.firebaseapp.com",
  projectId: "saveyourmoney-xxxxx",
  storageBucket: "saveyourmoney-xxxxx.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdefghijklmnop"
};
```

6. Bu config bilgilerini kopyalayın
7. `firebase-setup/firebase-config.js` dosyasına yapıştırın

### Adım 5: Service Account Key Alma (Node.js Admin SDK için)
1. Firebase Console'da **Project Settings** (⚙️) > **Service accounts** sekmesine gidin
2. **Generate new private key** butonuna tıklayın
3. **Generate key** butonuna tıklayın
4. İndirilen JSON dosyasını `firebase-setup/serviceAccountKey.json` olarak kaydedin
5. ⚠️ **Bu dosyayı asla GitHub'a commit etmeyin!** (.gitignore'a eklenmiştir)

---

## 2. Güvenlik Kuralları (Production için)

Test mode'dan production'a geçerken aşağıdaki kuralları uygulayın:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Metrics - Read only
    match /metrics/{document=**} {
      allow read: if request.auth != null;
      allow write: if false; // Only backend can write
    }

    // Recommendations - Read only
    match /recommendations/{document=**} {
      allow read: if request.auth != null;
      allow write: if false;
    }

    // Insights - Read only
    match /insights/{document=**} {
      allow read: if request.auth != null;
      allow write: if false;
    }

    // Deployments - Read only
    match /deployments/{document=**} {
      allow read: if request.auth != null;
      allow write: if false;
    }
  }
}
```

---

## 3. Maliyet Optimizasyonu

### Free Tier Limitleri (Firestore)
- **Stored data**: 1 GB
- **Document reads**: 50,000/day
- **Document writes**: 20,000/day
- **Document deletes**: 20,000/day

### Tavsiyeler
- Gereksiz okuma işlemlerini önlemek için **caching** kullanın
- Büyük koleksiyonlar için **pagination** kullanın
- **Composite indexes** oluşturun (Firebase otomatik önerecektir)

---

## 4. Yedekleme (Backup)

Firestore verilerinizi düzenli olarak yedekleyin:

```bash
# GCloud CLI ile backup
gcloud firestore export gs://[BUCKET_NAME] --project=[PROJECT_ID]
```

---

## Hazır mısınız?

Kurulumu tamamladıktan sonra:

1. `firebase-config.js` dosyasını güncelleyin
2. `serviceAccountKey.json` dosyasını kaydedin
3. Mock data insert script'ini çalıştırın:
   ```bash
   cd firebase-setup
   npm install
   node insert-mock-data.js
   ```
