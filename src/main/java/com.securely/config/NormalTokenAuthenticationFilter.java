package com.securely.config;

import com.securely.service.UserService;
import com.securely.util.NormalTokenUtil;
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
public class NormalTokenAuthenticationFilter extends OncePerRequestFilter {

    private final NormalTokenUtil normalTokenUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String token = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            token = requestTokenHeader.substring(7);
            try {
                if (normalTokenUtil.validateToken(token)) {
                    username = normalTokenUtil.getUsernameFromToken(token);
                }
            } catch (Exception e) {
                log.warn("Unable to validate normal token: {}", e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (normalTokenUtil.validateToken(token)) {
                var userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                    authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user {} via normal token", username);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
