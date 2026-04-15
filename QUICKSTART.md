# Quick Start Guide

Get the application running with H2 database and test all 4 authentication APIs.

## Prerequisites

- Java 21 (Eclipse Temurin)
- Maven 3.8+

## Start Application

```bash
# Run the Spring Boot app with H2 database
mvn spring-boot:run
```

**Access Points:**
- Application: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: (leave blank)

## 4 Core APIs Testing

### 1. Register API
Create a new user with username and password.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}'
```

**Response:**
```json
{
  "message": "User created successfully",
  "userId": 1,
  "username": "testuser"
}
```

### 2. Generate JWT Token
Authenticate user and generate JWT token.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}'
```

**Response:**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "testuser",
  "expiresIn": "24 hours"
}
```

### 3. Generate API Key
Create API key for existing user.

```bash
curl -X POST http://localhost:8080/api/auth/api-key \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}'
```

**Response:**
```json
{
  "message": "API key created successfully",
  "apiKey": "sk-1234567890abcdef...",
  "username": "testuser"
}
```

### 4. Fetch User Data (Property-Based Auth)
Authentication method changes based on `auth.type` property in `application.yml`.

#### Current: JWT Authentication (`auth.type: jwt`)
```bash
# Use JWT token from step 2
curl -X GET http://localhost:8080/api/auth/user \
  -H "Authorization: Bearer <jwt_token>"
```

#### If Changed to API Key (`auth.type: apikey`)
```bash
curl -X GET http://localhost:8080/api/auth/api-key/user \
  -H "X-API-Key: <api_key>"
```

#### If Changed to OAuth2 (`auth.type: oauth2`)
```bash
# OAuth2 login
curl -X POST http://localhost:8080/api/auth/oauth2 \
  -H "Content-Type: application/json" \
  -d '{"provider": "google", "oauth2Id": "1234567890", "username": "oauthuser"}'

# Use returned token
curl -X GET http://localhost:8080/api/auth/user \
  -H "Authorization: Bearer <oauth2_token>"
```

## Authentication Configuration

Change authentication method in `src/main/resources/application.yml`:

```yaml
auth:
  type: jwt  # Options: jwt, apikey, oauth2, basic
```

## Complete Test Sequence

```bash
# 1. Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}'

# 2. Get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}' | jq -r '.token')

# 3. Get API key
API_KEY=$(curl -s -X POST http://localhost:8080/api/auth/api-key \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass"}' | jq -r '.apiKey')

# 4. Test user data with JWT
curl -X GET http://localhost:8080/api/auth/user \
  -H "Authorization: Bearer $TOKEN"

# 5. Test user data with API key
curl -X GET http://localhost:8080/api/auth/api-key/user \
  -H "X-API-Key: $API_KEY"
```

## Database Access

View the database at: http://localhost:8080/h2-console
- **Table**: `USER`
- **Fields**: `id`, `username`, `password`, `api_key`, `oauth2_provider`, `oauth2_id`
