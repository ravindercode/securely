package com.securely.config;

import com.securely.service.UserService;
import com.securely.util.ApiKeyUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String apiKey = null;
        
        // Try to get API key from header
        String headerApiKey = request.getHeader("X-API-Key");
        if (headerApiKey != null && ApiKeyUtil.isValidApiKeyFormat(headerApiKey)) {
            apiKey = headerApiKey;
        }
        
        // Try to get API key from query parameter
        if (apiKey == null) {
            String paramApiKey = request.getParameter("apiKey");
            if (paramApiKey != null && ApiKeyUtil.isValidApiKeyFormat(paramApiKey)) {
                apiKey = paramApiKey;
            }
        }
        
        // Try to get API key from Authorization header (Bearer sk-...)
        if (apiKey == null) {
            String authHeader = request.getHeader("Authorization");
            String bearerApiKey = ApiKeyUtil.extractApiKeyFromHeader(authHeader);
            if (bearerApiKey != null && ApiKeyUtil.isValidApiKeyFormat(bearerApiKey)) {
                apiKey = bearerApiKey;
            }
        }

        if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userOpt = userService.findByApiKey(apiKey);
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(user.getUsername(), null, new ArrayList<>());
                authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user {} via API key", user.getUsername());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
