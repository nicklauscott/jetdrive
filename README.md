# 🚀 JetDrive Backend API

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/nicklauscott/JetDrive-Backend)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![API](https://img.shields.io/badge/API-REST-blue.svg)](https://restfulapi.net/)

**A robust and scalable Spring Boot API server for JetDrive cloud file management** — Provides secure file storage, user authentication, near real-time synchronization, and resumable transfer capabilities. Built with modern Spring Boot practices, this backend supports the JetDrive Android client with enterprise-grade reliability and performance.

**🔗 Companion Project:** [JetDrive Android Client](https://github.com/nicklauscott/JetDrive-Client)

---

## 🏗 Tech Stack

| Layer            | Technology / Framework                                         |
|------------------|----------------------------------------------------------------|
| **Language**     | Java 17+                                            |
| **Framework**    | Spring Boot 3.2+ (Web, Security, Data JPA)                  |
| **Database**     | PostgreSQL (primary) / MySQL (supported)                     |
| **Authentication** | Spring Security + Google Cloud Console OAuth 2.0                        |
| **File Storage** | AWS S3              |
| **Documentation** | OpenAPI 3 / Swagger UI                                      |
| **Build Tool**   |  Gradle                                               |

---

## 🚀 Features

- **🔐 Secure Authentication** - Supports email/password and Google “One Tap” login, with JWT-based session management
- **📁 File Management** - Upload, download, delete, move, and organize files
- **🔄 Resumable Transfers** - Chunked upload/download with pause/resume support
- **👥 User Management** - Multi-user support with isolated storage spaces
- **🔍 Search** - Full-text search
- **📊 Near Real-Time Sync** - Polling-based updates to keep files in sync with minimal delay
- **💾 Cloud Storage Backends** - AWS S3 with full S3 API compatibility for flexible storage management
- **🛡️ Security** - CORS and request validation
- **📚 API Documentation** - Interactive Swagger UI documentation

---

## ⚙️ Prerequisites

- **Java**: 17+ (OpenJDK recommended)
- **Database**: PostgreSQL 13+
- **Gradle**: Gradle 8.14+
- **Google Cloud Console**: Project with OAuth 2.0 credentials

---

## 🔧 Quick Start

### 1. Clone the repository
```bash
git clone https://github.com/nicklauscott/jetdrive.git
cd jetdrive
```

### 2. Database Setup

#### PostgreSQL
```bash
# Install PostgreSQL
sudo apt install postgresql postgresql-contrib

# Create database and user
sudo -u postgres psql
CREATE DATABASE jetdrive;
CREATE USER jetdrive_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE jetdrive TO jetdrive_user;
\q
```

### 3. Configuration

Create `application-dev.yml` in `src/main/resources/`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: org.postgresql.Driver
    
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: true

  flyway:
    baseline-on-migration: true
    enabled: false
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    baseline-description: "init"
    baseline-version: 0

  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
  
app:
  url: ${APP_BASE_URL}

jwt:
  secret: ${JWT_SECRET:your_jwt_secret_key_min_256_bits}

google:
  clientId: ${GOOGLE_CLIENT_ID}

s3:
  endpoint: ${S3_ENDPOINT}
  region: ${S3_REGION}
  access-key: ${S3_ACCESS_KEY}
  secret-key: ${S3_SECRET_KEY}

enc:
  base64Key: ${BASE64_MASTER_KEY}

springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true

logging:
  level:
    com.jetdrive: DEBUG
    org.springframework.security: DEBUG
```

### 4. Environment Variables

Create `.env` file or set system environment variables:
```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/jetdrive
DB_USERNAME=jetdrive_user
DB_PASSWORD=your_password

# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret

# JWT Security
JWT_SECRET=your_super_secret_jwt_key_must_be_at_least_256_bits_long

# File Storage
S3_ENDPOINT=your_s3_endpoint
S3_REGION=your_s3_region
S3_ACCESS_KEY=your_s3_access_key
S3_SECRET_KEY=your_s3_secret_key

# Encryption
BASE64_MASTER_KEY=your_base64_master_key

# App Configuration
APP_BASE_URL=http://localhost:8080
```

### 5. Build and Run

#### Using Gradle
```bash
# Install dependencies
./gradlew build

# Run development server
./gradlew bootRun --args='--spring.profiles.active=dev'

# Build production jar
./gradlew bootJar
```

The API server will be available at: `http://localhost:8080`

---

## 📡 API Documentation

### Interactive Documentation
Once the server is running, visit:
### Interactive Documentation
Once the server is running, visit:
- **Swagger UI (Local)**: `http://localhost:8080/swagger-ui/index.html`
- **Swagger UI (Deployed)**: `https://jetdrive.onrender.com/swagger-ui/index.html`
- **OpenAPI Spec (Local)**: `http://localhost:8080/v3/api-docs`
- **OpenAPI Spec (Deployed)**: `https://jetdrive.onrender.com/v3/api-docs`


### Core Endpoints

#### Authentication
```http
POST /auth/login                # Email/Password login
POST /auth/google/login         # Google one tap login 
POST /auth/refresh              # Refresh JWT token
POST /auth/validate             # Validate JWT token
```

#### File Management
```http
GET    /files                 # List files and directories
GET    /files/{id}            # Get file metadata
GET    /files/search/{query}  # Search files
POST   /files/create          # Create new file
PATCH  /files/rename          # Rename file
PATCH  /files/move            # Move file
PATCH  /files/copy            # Copy file
DELETE /files/delete/{id}     # Delete file
GET    /files/download/{id}   # Download file
```

#### File Sync
```http
GET  /sync/changes           # Get sync changes
```

#### Resumable Uploads
```http
POST  /files/upload/init           # Initialize resumable upload
PUT   /files/upload/{id}           # Upload file chunk
POST  /files/upload/{id}/complete  # Complete upload
GET   /files/upload/status/{id}    # Check upload progress
```

#### User Management
```http
GET     /users   # Get user profile
PATCH   /user    # Update user profile
POST    /user    # Upload profile picture
DELETE  /user    # Delete user account
```

---

## 🛠 Configuration Options

### S3 Storage Configuration
```yaml
s3:
  endpoint: https://s3.amazonaws.com  # or your custom S3-compatible endpoint
  region: us-east-1
  access-key: ${S3_ACCESS_KEY}
  secret-key: ${S3_SECRET_KEY}
  bucket-name: your-jetdrive-bucket
```

### Security Configuration
```yaml
jwt:
  secret: ${JWT_SECRET}

google:
  clientId: ${GOOGLE_CLIENT_ID}

enc:
  base64Key: ${BASE64_MASTER_KEY}  # For file encryption
```

### File Upload Configuration
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

---

## 🚀 Deployment

### Docker Deployment

#### 1. Create Dockerfile
```dockerfile
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY gradle ./gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew clean bootJar --no-daemon

FROM openjdk:21-jdk-slim AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 2. Docker Compose
```yaml
version: '3.8'

services:
  jetdrive-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/jetdrive
      - SPRING_DATASOURCE_USERNAME=jetdrive_user
      - SPRING_DATASOURCE_PASSWORD=secure_password
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=jetdrive
      - POSTGRES_USER=jetdrive_user
      - POSTGRES_PASSWORD=secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

#### 3. Run with Docker Compose
```bash
docker-compose up -d
```

### Production Deployment

#### 1. Build for Production
```bash
./gradlew clean bootJar
```

#### 2. Create systemd service
```bash
sudo vim /etc/systemd/system/jetdrive-api.service
```

```ini
[Unit]
Description=JetDrive API Server
After=network.target

[Service]
Type=simple
User=jetdrive
ExecStart=/usr/bin/java -jar /opt/jetdrive/jetdrive-backend.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

#### 3. Enable and start service
```bash
sudo systemctl daemon-reload
sudo systemctl enable jetdrive-api
sudo systemctl start jetdrive-api
```

---

## 🛠 Development

### Code Quality
```bash
# Run static analysis (if configured)
./gradlew check

# Format code (if ktlint is configured)
./gradlew ktlintFormat

# Build and check everything
./gradlew build
```

### Database Migrations
```bash
# If using Flyway
./gradlew flywayMigrate

# Validate migrations
./gradlew flywayValidate

# Migration info
./gradlew flywayInfo
```

---

## 🛠 Troubleshooting

### Common Issues

**Database Connection Errors:**
```bash
# Check database status
sudo systemctl status postgresql

# Test connection
psql -h localhost -U jetdrive_user -d jetdrive -c "SELECT 1;"

# Check logs
tail -f /var/log/postgresql/postgresql-*.log
```

**File Upload Issues:**
- Check S3 configuration and credentials
- Verify S3 bucket permissions and access
- Validate file size limits in configuration

**Authentication Problems:**
- Verify Google OAuth credentials in Google Cloud Console
- Validate JWT secret is properly set and secure
- Check Google One Tap configuration

**Performance Issues:**
- Enable database query logging to identify slow queries
- Monitor JVM memory usage: `jstat -gc <pid>`
- Check S3 connection latency

### Logging Configuration
```yaml
logging:
  level:
    com.jetdrive: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: logs/jetdrive-api.log
    max-size: 10MB
    max-history: 30
```

---

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guide](CONTRIBUTING.md) before submitting pull requests.

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Set up development environment following the Quick Start guide
4. Run tests: `./gradlew test`
5. Submit a pull request with clear description

### Code Standards
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Write comprehensive tests for new features
- Update API documentation for endpoint changes
- Include database migrations if schema changes are made

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Spring Boot** - for the excellent framework and ecosystem
- **PostgreSQL** - for reliable data storage
- **Google OAuth** - for secure authentication
- **OpenAPI** - for API documentation standards
- **Docker** - for containerization support

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/nicklauscott/jetdrive/issues)

---

<div align="center">

**⭐ Star this repository if you find it useful!**

**🔗 Don't forget to check out the [JetDrive Android Client](https://github.com/nicklauscott/JetDrive-Client)**

Made with ❤️ by [nicklauscott](https://github.com/nicklauscott)

</div>
