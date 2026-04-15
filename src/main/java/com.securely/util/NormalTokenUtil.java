package com.securely.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Component
public class NormalTokenUtil {

    private static final long TOKEN_EXPIRATION_HOURS = 24;

    public String generateToken(String username) {
        // Generate a simple token using username + timestamp + random UUID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        String tokenData = username + ":" + timestamp + ":" + randomPart;
        
        // Encode to Base64 for a simple token format
        return Base64.getEncoder().encodeToString(tokenData.getBytes());
    }

    public String getUsernameFromToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token.getBytes()));
            String[] parts = decoded.split(":");
            return parts.length > 0 ? parts[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token.getBytes()));
            String[] parts = decoded.split(":");
            
            if (parts.length < 3) {
                return false;
            }
            
            // Check if token is expired (24 hours)
            String timestampStr = parts[1];
            LocalDateTime tokenTime = LocalDateTime.parse(timestampStr, 
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            LocalDateTime expiryTime = tokenTime.plusHours(TOKEN_EXPIRATION_HOURS);
            
            return LocalDateTime.now().isBefore(expiryTime);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return !validateToken(token);
    }

    public String getTokenInfo(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token.getBytes()));
            String[] parts = decoded.split(":");
            
            if (parts.length >= 3) {
                String username = parts[0];
                String timestampStr = parts[1];
                LocalDateTime tokenTime = LocalDateTime.parse(timestampStr, 
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                
                return String.format("Token for user: %s, created: %s", username, tokenTime);
            }
            return "Invalid token format";
        } catch (Exception e) {
            return "Invalid token";
        }
    }
}
