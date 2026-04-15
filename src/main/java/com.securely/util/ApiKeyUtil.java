package com.securely.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ApiKeyUtil {

    public static String generateApiKey() {
        return "sk-" + UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isValidApiKeyFormat(String apiKey) {
        return apiKey != null && apiKey.startsWith("sk-") && apiKey.length() > 10;
    }

    public static String extractApiKeyFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public static String extractApiKeyFromQueryParam(String apiKey) {
        return isValidApiKeyFormat(apiKey) ? apiKey : null;
    }
}
