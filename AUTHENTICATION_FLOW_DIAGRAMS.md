# 4 Authentication Flow Diagrams with SecurityConfig

## Overview

Complete flow diagrams including SecurityConfig implementation and filter chain configuration for all authentication methods.

## SecurityConfig Implementation

### SecurityConfig Class Structure
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final NormalTokenAuthenticationFilter normalTokenAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf().disable()
            .sessionManagement().sessionCreationPolicy(STATELESS)
            .authorizeHttpRequests()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/oauth2").permitAll()
                .requestMatchers("/api/auth/api-key", "/api/auth/user/{username}", "/api/auth/users").permitAll()
                .requestMatchers("/h2-console/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(normalTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## 1. JWT Authentication (Default)

### Complete Flow with SecurityConfig
```mermaid
sequenceDiagram
    participant Client
    participant SecurityConfig
    participant JwtAuthenticationFilter
    participant API
    participant UserService
    participant Database
    participant JwtUtil

    Client->>API: POST /api/auth/register
    Note over Client,API: {username, password}
    SecurityConfig->>SecurityConfig: permitAll() - No auth required
    API->>UserService: createUser(username, password)
    UserService->>Database: Check username exists
    Database-->>UserService: Username available
    UserService->>Database: Save user with BCrypt password
    Database-->>UserService: User created
    API-->>Client: {message, userId, username}

    Client->>API: POST /api/auth/login
    Note over Client,API: {username, password}
    SecurityConfig->>SecurityConfig: permitAll() - No auth required
    API->>UserService: authenticateUser(username, password)
    UserService->>Database: Get user by username
    Database-->>UserService: User found
    UserService->>UserService: Validate password with BCrypt
    UserService-->>API: Authentication successful
    API->>JwtUtil: generateToken(username)
    JwtUtil-->>API: JWT token (24 hours)
    API-->>Client: {token, username, expiresIn}

    Client->>API: GET /api/auth/user
    Note over Client,API: Authorization: Bearer <jwt_token>
    SecurityConfig->>JwtAuthenticationFilter: Filter #3 in chain
    JwtAuthenticationFilter->>JwtUtil: validateToken(token)
    JwtUtil->>JwtUtil: Verify signature & expiration
    JwtUtil-->>JwtAuthenticationFilter: Token valid
    JwtAuthenticationFilter->>JwtUtil: getUsernameFromToken(token)
    JwtUtil-->>JwtAuthenticationFilter: username
    JwtAuthenticationFilter->>UserService: findByUsername(username)
    UserService->>Database: Get user details
    Database-->>UserService: User data
    UserService-->>JwtAuthenticationFilter: User found
    JwtAuthenticationFilter->>SecurityConfig: Set Security Context
    SecurityConfig->>API: Authentication successful
    API-->>Client: {user details}
```

### SecurityConfig Integration
- **Filter Position**: 3rd in filter chain (after API Key and Normal Token)
- **Endpoints**: `/api/auth/user` requires authentication
- **Validation**: JWT signature verification with JwtUtil
- **Security Context**: UsernamePasswordAuthenticationToken set

---

## 2. API Key Authentication

### Complete Flow with SecurityConfig
```mermaid
sequenceDiagram
    participant Client
    participant SecurityConfig
    participant ApiKeyAuthenticationFilter
    participant API
    participant UserService
    participant Database
    participant ApiKeyUtil

    Client->>API: POST /api/auth/api-key
    Note over Client,API: {username, password}
    SecurityConfig->>SecurityConfig: permitAll() - No auth required
    API->>UserService: authenticateUser(username, password)
    UserService-->>API: Authentication successful
    API->>ApiKeyUtil: generateApiKey()
    ApiKeyUtil->>ApiKeyUtil: UUID + "sk-" prefix
    ApiKeyUtil-->>API: API key (permanent)
    API->>UserService: Update user with API key
    UserService->>Database: Save API key
    Database-->>UserService: User updated
    API-->>Client: {apiKey, username}

    Client->>API: GET /api/auth/api-key/user
    Note over Client,API: X-API-Key: <api_key>
    SecurityConfig->>ApiKeyAuthenticationFilter: Filter #1 in chain
    ApiKeyAuthenticationFilter->>ApiKeyAuthenticationFilter: Extract API key from header
    ApiKeyAuthenticationFilter->>UserService: findByApiKey(apiKey)
    UserService->>Database: Query by API key
    Database-->>UserService: User found
    UserService-->>ApiKeyAuthenticationFilter: User data
    ApiKeyAuthenticationFilter->>SecurityConfig: Set Security Context
    SecurityConfig->>API: Authentication successful
    API-->>Client: {user details}

    Note over Client,API: Alternative methods:
    Client->>API: GET /api/auth/api-key/user?apiKey=<api_key>
    Note over Client,API: ApiKeyAuthenticationFilter extracts from query param
    Client->>API: GET /api/auth/api-key/user
    Note over Client,API: Authorization: Bearer <api_key>
    Note over Client,API: ApiKeyAuthenticationFilter extracts from Authorization header
```

### SecurityConfig Integration
- **Filter Position**: 1st in filter chain (highest priority)
- **Endpoints**: `/api/auth/api-key/user` requires authentication
- **Validation**: Database lookup with UserService
- **Security Context**: UsernamePasswordAuthenticationToken set with username

---

## 3. OAuth2 Authentication (JWT Version)

### Complete Flow with SecurityConfig
```mermaid
sequenceDiagram
    participant Client
    participant SecurityConfig
    participant OAuth2Provider
    participant API
    participant UserService
    participant Database
    participant JwtUtil

    Client->>OAuth2Provider: OAuth2 Login (Google, GitHub, etc.)
    OAuth2Provider-->>Client: OAuth2 token + user info
    Client->>API: POST /api/auth/oauth2
    Note over Client,API: {provider, oauth2Id, username}
    SecurityConfig->>SecurityConfig: permitAll() - No auth required
    API->>UserService: createOrUpdateOAuth2User(provider, oauth2Id, username)
    UserService->>Database: Find existing user by provider+oauth2Id
    alt User exists
        Database-->>UserService: User found
        UserService-->>API: Return existing user
    else New user
        UserService->>Database: Create OAuth2 user
        Database-->>UserService: User created
        UserService-->>API: New user
    end
    API->>JwtUtil: generateToken(username)
    JwtUtil-->>API: JWT token
    API-->>Client: {token, username, provider, expiresIn}

    Client->>API: GET /api/auth/user
    Note over Client,API: Authorization: Bearer <oauth2_jwt_token>
    SecurityConfig->>JwtAuthenticationFilter: Filter #3 in chain
    JwtAuthenticationFilter->>JwtUtil: validateToken(token)
    JwtUtil-->>JwtAuthenticationFilter: Token valid
    JwtAuthenticationFilter->>JwtUtil: getUsernameFromToken(token)
    JwtUtil-->>JwtAuthenticationFilter: username
    JwtAuthenticationFilter->>UserService: findByUsername(username)
    UserService->>Database: Get user details
    Database-->>UserService: User data with OAuth2 info
    UserService-->>JwtAuthenticationFilter: User found
    JwtAuthenticationFilter->>SecurityConfig: Set Security Context
    SecurityConfig->>API: Authentication successful
    API-->>Client: {user details with oauth2Provider, oauth2Id}
```

### SecurityConfig Integration
- **Filter Position**: Uses JwtAuthenticationFilter (3rd in chain)
- **Endpoints**: `/api/auth/oauth2` is public, `/api/auth/user` requires auth
- **Validation**: JWT token generated after OAuth2 user creation
- **Security Context**: Standard JWT authentication flow

---

## 4. OAuth2 Authentication (Normal Token Version)

### Complete Flow with SecurityConfig
```mermaid
sequenceDiagram
    participant Client
    participant SecurityConfig
    participant NormalTokenAuthenticationFilter
    participant OAuth2Provider
    participant API
    participant UserService
    participant Database
    participant NormalTokenUtil

    Client->>OAuth2Provider: OAuth2 Login
    OAuth2Provider-->>Client: OAuth2 token + user info
    Client->>API: POST /api/auth/oauth2
    Note over Client,API: {provider, oauth2Id, username}
    SecurityConfig->>SecurityConfig: permitAll() - No auth required
    API->>UserService: createOrUpdateOAuth2User(provider, oauth2Id, username)
    UserService->>Database: Find/create OAuth2 user
    Database-->>UserService: User ready
    UserService-->>API: User data
    API->>NormalTokenUtil: generateToken(username)
    NormalTokenUtil->>NormalTokenUtil: Base64(username:timestamp:uuid)
    NormalTokenUtil-->>API: Normal token
    API-->>Client: {token, username, provider, tokenType: "normal"}

    Client->>API: GET /api/auth/user
    Note over Client,API: Authorization: Bearer <normal_token>
    SecurityConfig->>NormalTokenAuthenticationFilter: Filter #2 in chain
    NormalTokenAuthenticationFilter->>NormalTokenUtil: validateToken(token)
    NormalTokenUtil->>NormalTokenUtil: Decode Base64, check format & expiration
    NormalTokenUtil-->>NormalTokenAuthenticationFilter: Token valid
    NormalTokenAuthenticationFilter->>NormalTokenUtil: getUsernameFromToken(token)
    NormalTokenUtil-->>NormalTokenAuthenticationFilter: username
    NormalTokenAuthenticationFilter->>UserService: findByUsername(username)
    UserService->>Database: Get user details
    Database-->>UserService: User data
    UserService-->>NormalTokenAuthenticationFilter: User found
    NormalTokenAuthenticationFilter->>SecurityConfig: Set Security Context
    SecurityConfig->>API: Authentication successful
    API-->>Client: {user details}
```

### SecurityConfig Integration
- **Filter Position**: 2nd in filter chain (after API Key, before JWT)
- **Endpoints**: `/api/auth/oauth2` is public, `/api/auth/user` requires auth
- **Validation**: NormalTokenUtil validates Base64 format and timestamp
- **Security Context**: UsernamePasswordAuthenticationToken set with extracted username

---

## SecurityConfig Filter Chain Configuration

### Filter Chain Order
```mermaid
flowchart TD
    A[HTTP Request] --> B[SecurityConfig Filter Chain]
    B --> C[1. ApiKeyAuthenticationFilter]
    C --> D{API Key found?}
    D -->|Yes| E[Set Security Context]
    D -->|No| F[2. NormalTokenAuthenticationFilter]
    F --> G{Normal Token found?}
    G -->|Yes| H[Set Security Context]
    G -->|No| I[3. JwtAuthenticationFilter]
    I --> J{JWT Token found?}
    J -->|Yes| K[Set Security Context]
    J -->|No| L[401 Unauthorized]
    
    E --> M[Controller]
    H --> M
    K --> M
    L --> N[Error Response]
```

### SecurityConfig URL Rules
```yaml
# Public endpoints (no authentication required)
/api/auth/register
/api/auth/login  
/api/auth/oauth2
/api/auth/api-key
/api/auth/user/{username}
/api/auth/users
/h2-console/**
/actuator/**

# Protected endpoints (authentication required)
/api/auth/user
/api/secure/**
```

---

## SecurityConfig Beans Configuration

### Essential Beans
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // For password hashing
}

@Bean  
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    return config.getAuthenticationManager();  // For authentication
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    // Filter chain configuration with all 3 authentication filters
}
```

---

## Comparison Summary with SecurityConfig

| Authentication Method | Filter in SecurityConfig | Filter Position | Validation Method | Security Context |
|----------------------|--------------------------|----------------|------------------|-----------------|
| **API Key** | ApiKeyAuthenticationFilter | 1st (highest priority) | Database lookup | Username from API key |
| **Normal Token** | NormalTokenAuthenticationFilter | 2nd | Base64 + timestamp | Username from token |
| **JWT** | JwtAuthenticationFilter | 3rd (lowest priority) | JWT signature | Username from JWT |
| **OAuth2** | Uses JWT or Normal Token filter | Depends on token type | OAuth2 + token validation | OAuth2 user data |

### Database Schema
```sql
USER {
  id: Long (Primary Key)
  username: String (Unique)
  password: String (BCrypt encrypted)
  api_key: String (Unique, nullable)
  oauth2_provider: String (nullable)
  oauth2_id: String (nullable)
}
```

All authentication methods are integrated through SecurityConfig with proper filter chain ordering and Spring Security context management.
