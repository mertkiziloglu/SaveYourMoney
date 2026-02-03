# 🔐 Security Policy

**SaveYourMoney Project**

---

## 🛡️ Security Features

### 1. API Security

#### Production Environment
- ✅ **API Key Authentication:** All protected endpoints require X-API-Key header
- ✅ **CORS Protection:** Restricted to known origins (Cloud Run, Firebase)
- ✅ **Rate Limiting:** Prevents abuse (configurable per endpoint)
- ✅ **HTTPS Only:** All production traffic encrypted with TLS 1.3

#### Authentication Methods

**Option 1: API Key (Simple)**
```bash
curl -H "X-API-Key: your-api-key" \
  https://analyzer-service.run.app/api/analyze/cpu-hungry-service
```

**Option 2: HTTP Basic Auth (Development)**
```bash
curl -u username:password \
  http://localhost:8084/api/dashboard
```

**Option 3: JWT Bearer Token (Future)**
```bash
curl -H "Authorization: Bearer eyJhbGc..." \
  https://analyzer-service.run.app/api/metrics/cpu-hungry-service
```

---

### 2. Data Security

#### Data at Rest
- ✅ **Firestore Encryption:** All data encrypted at rest (AES-256)
- ✅ **H2 Database:** In-memory only, no persistent storage in dev
- ✅ **No PII Storage:** We don't collect personally identifiable information

#### Data in Transit
- ✅ **HTTPS/TLS:** All API calls use HTTPS in production
- ✅ **GCP Internal Networking:** Service-to-service calls use private networking
- ✅ **Certificate Validation:** All external calls validate SSL certificates

#### Data Retention
- ✅ **30-Day Retention:** Metrics automatically deleted after 30 days
- ✅ **GDPR Compliant:** Right to deletion supported
- ✅ **Minimal Data Collection:** Only technical metrics, no user data

---

### 3. Dependency Security

#### Automated Scanning
- ✅ **GitHub Dependabot:** Automatic vulnerability scanning
- ✅ **OWASP Dependency Check:** Maven plugin checks for CVEs
- ✅ **CI/CD Security Scan:** Every build scans dependencies

#### Dependency Management
```xml
<!-- Spring Boot 3.2.2 - Latest stable -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.2</version>
</parent>

<!-- Security dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT (latest) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

---

### 4. Infrastructure Security

#### GCP Security
- ✅ **IAM Roles:** Principle of least privilege
- ✅ **Service Accounts:** Separate SA for each service
- ✅ **VPC Security:** Private networking between services
- ✅ **Cloud Armor:** DDoS protection (optional)

#### Kubernetes Security (GKE)
- ✅ **Pod Security Standards:** Restricted mode
- ✅ **Network Policies:** Ingress/egress rules
- ✅ **Secret Management:** GCP Secret Manager
- ✅ **Image Scanning:** Container vulnerability scanning

---

### 5. Application Security

#### Input Validation
```java
@PostMapping("/analyze/{serviceName}")
public ResponseEntity<ResourceRecommendation> analyzeService(
    @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") String serviceName) {
    // Only allows lowercase alphanumeric and hyphens
}
```

#### SQL Injection Prevention
- ✅ **JPA/Hibernate:** Parameterized queries only
- ✅ **No Raw SQL:** All queries use ORM

#### XSS Protection
- ✅ **Content Security Policy:** CSP headers enabled
- ✅ **JSON Responses:** All APIs return JSON (not HTML)
- ✅ **Output Encoding:** Automatic escaping

---

## 🚨 Reported Vulnerabilities

None reported yet. See "Reporting a Vulnerability" below.

---

## 📋 Security Checklist

### Development
- [x] HTTPS enforced in production
- [x] API authentication implemented
- [x] CORS properly configured
- [x] Dependencies scanned for CVEs
- [x] Security headers enabled
- [x] Input validation implemented
- [x] Error messages don't leak sensitive info
- [x] Logging doesn't include secrets

### Deployment
- [x] Secrets in environment variables (not code)
- [x] Service accounts have minimal permissions
- [x] Firestore security rules configured
- [x] HTTPS certificates valid
- [x] Rate limiting enabled
- [x] Monitoring and alerting configured

---

## 🔒 Secrets Management

### Environment Variables

**Development:**
```bash
# .env (local only - never commit)
API_KEY=dev-api-key-12345
AZURE_DEVOPS_TOKEN=your-token
GOOGLE_CLOUD_PROJECT=your-project-id
```

**Production:**
```bash
# Set via GCP Secret Manager
gcloud secrets create api-key --data-file=- <<< "production-key"

# Access in Cloud Run
gcloud run services update analyzer-service \
  --update-secrets=API_KEY=api-key:latest
```

### Never Commit
- ❌ API keys
- ❌ Database passwords
- ❌ OAuth tokens
- ❌ Private keys
- ❌ Service account keys (*.json)

### .gitignore
```
# Secrets
.env
.env.local
*.pem
*.key
serviceAccountKey.json
firebase-setup/serviceAccountKey.json
```

---

## 🐛 Reporting a Vulnerability

### How to Report

**Email:** security@saveyourmoney.ai

**What to Include:**
1. Description of the vulnerability
2. Steps to reproduce
3. Potential impact
4. Suggested fix (if any)

### Response Timeline

- **24 hours:** Acknowledgment
- **7 days:** Initial assessment
- **30 days:** Fix or mitigation plan
- **Public disclosure:** After fix is deployed

### Responsible Disclosure

We follow responsible disclosure practices:
1. Report privately first
2. We fix the issue
3. Public disclosure after 90 days (or sooner if mutually agreed)

---

## 🏆 Security Best Practices

### For Developers

1. **Never hardcode secrets**
   ```java
   // ❌ Bad
   String apiKey = "abc123";

   // ✅ Good
   @Value("${app.api-key}")
   private String apiKey;
   ```

2. **Validate all inputs**
   ```java
   // ✅ Always validate
   @PathVariable @Pattern(regexp = "^[a-z0-9-]+$") String serviceName
   ```

3. **Use parameterized queries**
   ```java
   // ✅ JPA/Hibernate does this automatically
   repository.findByServiceName(serviceName);
   ```

4. **Log carefully**
   ```java
   // ❌ Don't log sensitive data
   log.info("User password: {}", password);

   // ✅ Log safely
   log.info("User authenticated: {}", username);
   ```

---

## 📊 Security Monitoring

### Alerts Configured

- ✅ Failed authentication attempts > 10/min
- ✅ Unusual traffic patterns
- ✅ New vulnerabilities in dependencies
- ✅ SSL certificate expiration (30 days warning)

### Logging

All security events logged to GCP Cloud Logging:
- Authentication attempts
- Authorization failures
- API key validation
- Unusual request patterns

---

## 🔄 Security Updates

### Automated Updates
- **Dependabot:** Auto-updates dependencies weekly
- **Renovate:** Alternative dependency updater
- **GitHub Actions:** CI/CD scans on every commit

### Manual Reviews
- **Quarterly Security Audit**
- **Penetration Testing** (annual)
- **Dependency Review** (monthly)

---

## 📞 Security Contacts

- **Security Team:** security@saveyourmoney.ai
- **General Issues:** https://github.com/saveyourmoney/issues
- **Emergency:** security-emergency@saveyourmoney.ai (24/7)

---

## 📚 References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [GCP Security Best Practices](https://cloud.google.com/security/best-practices)
- [CWE Top 25](https://cwe.mitre.org/top25/)

---

**Last Updated:** 2026-02-03
**Version:** 1.0

🔐 Security is everyone's responsibility.
