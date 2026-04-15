# Quick Start Guide

Get the application running in 5 minutes with Docker.

## Prerequisites

- Docker & Docker Compose
- Java 21 (Eclipse Temurin) - for local development
- Maven 3.8+

## Option 1: Run with Docker Compose (Recommended)

This starts MySQL, Keycloak, and the Spring Boot app automatically.

```bash
# 1. Build the JAR first
mvn clean package -DskipTests

# 2. Start all services
docker-compose up -d

# 3. Wait 30-60 seconds for services to start
```

**Services will be available at:**
| Service | URL |
|---------|-----|
| Application | http://localhost:8080 |
| Keycloak Admin | http://localhost:8181/admin (admin/admin) |
| MySQL | localhost:3307 |

## Option 2: Run Locally (Development)

```bash
# 1. Start MySQL & Keycloak only
docker-compose up -d mysql keycloak

# 2. Run the Spring Boot app
mvn spring-boot:run
```

## Quick Test

### 1. JWT Mode (Default)
```bash
# Login and get token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'

# Use token
curl http://localhost:8080/api/test \
  -H "Authorization: Bearer <token>"
```

### 2. API Key Mode
Edit `application.yml`: `auth.type: apikey`

```bash
curl http://localhost:8080/api/test \
  -H "X-API-KEY: my-secret-api-key-12345"
```

### 3. Basic Auth Mode
Edit `application.yml`: `auth.type: basic`

```bash
curl http://localhost:8080/api/test \
  -u username:password
```

### 4. OAuth2/Keycloak Mode
Edit `application.yml`: `auth.type: oauth2`

1. Setup Keycloak (see [README.md](README.md) for detailed steps)
2. Get token:
```bash
curl -X POST http://localhost:8181/realms/securely/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=securely-app" \
  -d "username=testuser" \
  -d "password=password"
```

3. Use token:
```bash
curl http://localhost:8080/api/test \
  -H "Authorization: Bearer <token>"
```

## Stop Everything

```bash
docker-compose down -v
```

## Build Docker Image Only

```bash
docker build -t securely:latest .
```
