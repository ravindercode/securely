# Securely - Multi-Auth Spring Boot Application

A Spring Boot application demonstrating multiple authentication types with `@ConditionalOnProperty` for easy switching.

## Supported Authentication Types

- **JWT** (default) - JSON Web Token authentication
- **Basic Auth** - HTTP Basic authentication
- **API Key** - X-API-KEY header authentication
- **OAuth2** - OAuth2 Resource Server with JWT

## Configuration

Switch authentication types in `application.yml`:

```yaml
auth:
  type: jwt  # Options: basic, apikey, jwt, oauth2
```

## Auth Type Details

### 1. JWT Authentication (Default)
Configuration: `auth.type: jwt`

**Login:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'
```

**Access protected endpoint:**
```bash
curl http://localhost:8080/api/test \
  -H "Authorization: Bearer <token>"
```

### 2. Basic Authentication
Configuration: `auth.type: basic`

**Access protected endpoint:**
```bash
curl http://localhost:8080/api/test \
  -u username:password
```

### 3. API Key Authentication
Configuration: `auth.type: apikey`

Configure API key in `application.yml`:
```yaml
api:
  key: my-secret-api-key-12345
```

**Access protected endpoint:**
```bash
curl http://localhost:8080/api/test \
  -H "X-API-KEY: my-secret-api-key-12345"
```

### 4. OAuth2 Resource Server (Keycloak)
Configuration: `auth.type: oauth2`

**Setup Keycloak (included in docker-compose):**

1. Start all services:
   ```bash
   docker-compose up
   ```

2. Access Keycloak Admin Console: http://localhost:8181/admin
   - Username: `admin`
   - Password: `admin`

3. Create a new realm:
   - Click realm dropdown → "Add realm"
   - Name: `securely`

4. Create a client:
   - Go to Clients → Create
   - Client ID: `securely-app`
   - Client Protocol: `openid-connect`
   - Access Type: `public`
   - Enable: `Standard Flow Enabled`
   - Valid Redirect URIs: `http://localhost:8080/*`
   - Web Origins: `http://localhost:8080`

5. Create a user:
   - Go to Users → Add User
   - Username: `testuser`
   - Email: `test@example.com`
   - Save → Credentials tab → Set Password

6. Get an access token:
   ```bash
   curl -X POST http://localhost:8181/realms/securely/protocol/openid-connect/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=password" \
     -d "client_id=securely-app" \
     -d "username=testuser" \
     -d "password=your-password"
   ```

7. Access protected endpoint:
   ```bash
   curl http://localhost:8080/api/test \
     -H "Authorization: Bearer <access_token>"
   ```

## Endpoints

| Endpoint | Auth Required | Description |
|----------|--------------|-------------|
| `POST /auth/login` | No | Get JWT token (JWT mode only) |
| `GET /api/test` | Yes | Test protected endpoint |
| `GET /api/public` | No | Public test endpoint |
| `POST /realms/securely/protocol/openid-connect/token` | No | Get OAuth2 token from Keycloak |

## Running the Application

```bash
./mvnw spring-boot:run
```

Or with Docker:
```bash
docker-compose up
```

## Docker Services

The `docker-compose.yml` includes:

| Service | Port | Description |
|---------|------|-------------|
| MySQL | 3307 | Application database & Keycloak storage |
| Keycloak | 8181 | OAuth2 provider (admin: admin/admin) |
| Spring App | 8080 | The secured application |

## Architecture

The application uses Spring Boot's `@ConditionalOnProperty` annotation to conditionally enable security configurations based on the `auth.type` property:

- `@ConditionalOnProperty(name = "auth.type", havingValue = "jwt")` → `JwtSecurityConfig`
- `@ConditionalOnProperty(name = "auth.type", havingValue = "basic")` → `BasicAuthSecurityConfig`
- `@ConditionalOnProperty(name = "auth.type", havingValue = "apikey")` → `ApiKeyAuthSecurityConfig`
- `@ConditionalOnProperty(name = "auth.type", havingValue = "oauth2")` → `OAuth2SecurityConfig` (with Keycloak)

Only the matching configuration is loaded at runtime based on your settings.