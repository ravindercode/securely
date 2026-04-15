package com.securely.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApiKeyService {

    @Value("${api.key:default-api-key}")
    private String configuredApiKey;

    private final Set<String> validApiKeys = ConcurrentHashMap.newKeySet();

    public ApiKeyService() {
    }

    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        return apiKey.equals(configuredApiKey) || validApiKeys.contains(apiKey);
    }

    public void addApiKey(String apiKey) {
        validApiKeys.add(apiKey);
    }

    public void removeApiKey(String apiKey) {
        validApiKeys.remove(apiKey);
    }
}
