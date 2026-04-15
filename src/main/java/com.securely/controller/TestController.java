package com.securely.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public Map<String, String> test(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentication successful!");
        response.put("user", authentication != null ? authentication.getName() : "anonymous");
        return response;
    }

    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a public endpoint - no auth required!");
        return response;
    }
}
