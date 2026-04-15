package com.securely.controller;

import com.securely.entity.User;
import com.securely.service.UserService;
import com.securely.util.JwtUtil;
import com.securely.util.NormalTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final NormalTokenUtil normalTokenUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }
            
            User user = userService.createUser(username, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }
            
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
            }
            
            String token = jwtUtil.generateToken(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("username", username);
            response.put("expiresIn", "24 hours");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api-key")
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required"));
            }
            
            User user = userService.createApiKey(username, password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "API key created successfully");
            response.put("apiKey", user.getApiKey());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable String username) {
        try {
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("hasApiKey", user.getApiKey() != null);
            response.put("oauth2Provider", user.getOauth2Provider());
            response.put("oauth2Id", user.getOauth2Id());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch user details: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = null;
            String username = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    username = jwtUtil.getUsernameFromToken(token);
                }
            }
            
            if (username == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing token"));
            }
            
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("apiKey", user.getApiKey());
            response.put("oauth2Provider", user.getOauth2Provider());
            response.put("oauth2Id", user.getOauth2Id());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch user details: " + e.getMessage()));
        }
    }

    @PostMapping("/oauth2")
    public ResponseEntity<Map<String, Object>> oauth2Login(@RequestBody Map<String, String> request) {
        try {
            String provider = request.get("provider");
            String oauth2Id = request.get("oauth2Id");
            String username = request.get("username");
            
            if (provider == null || oauth2Id == null || username == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Provider, oauth2Id, and username are required"));
            }
            
            User user = userService.createOrUpdateOAuth2User(provider, oauth2Id, username);
            String token = normalTokenUtil.generateToken(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "OAuth2 login successful");
            response.put("token", token);
            response.put("username", username);
            response.put("provider", provider);
            response.put("tokenType", "normal");
            response.put("expiresIn", "24 hours");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "OAuth2 login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            users.forEach(user -> {
                user.setPassword(null); // Don't expose passwords
            });
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api-key/user")
    public ResponseEntity<Map<String, Object>> getUserByApiKey(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                              @RequestParam(value = "apiKey", required = false) String paramApiKey) {
        try {
            String effectiveApiKey = apiKey != null ? apiKey : paramApiKey;
            
            if (effectiveApiKey == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "API key is required"));
            }
            
            Optional<User> userOpt = userService.findByApiKey(effectiveApiKey);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("oauth2Provider", user.getOauth2Provider());
            response.put("oauth2Id", user.getOauth2Id());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch user details: " + e.getMessage()));
        }
    }
}
