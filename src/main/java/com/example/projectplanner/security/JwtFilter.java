package com.example.projectplanner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = Logger.getLogger(JwtFilter.class.getName());

    private final JwtProvider jwtProvider;

    @Autowired
    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warning("No Authorization header or it does not start with 'Bearer '");
            chain.doFilter(request, response);
            return;
        }

        // Extract token safely
        String token = authHeader.substring(7).trim();

        try {
            if (!jwtProvider.validateToken(token)) {
                logger.warning("JWT token validation failed");
                sendUnauthorizedResponse(response, "Invalid or expired token");
                return;
            }

            String username = jwtProvider.getUsernameFromToken(token);
            List<String> roles = jwtProvider.getRolesFromToken(token);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Prefix roles with "ROLE_"
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Avoid resetting authentication if already set
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            logger.info("User roles: " + roles);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "JWT Authentication failed", e);
            sendUnauthorizedResponse(response, "Authentication failed: " + e.getMessage());
            //sendJsonErrorResponse(response, "Invalid Token", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    //private void sendJsonErrorResponse(HttpServletResponse response, String message, int status) throws IOException {
    //    response.setStatus(status);
    //    response.setContentType("application/json");
    //    response.setCharacterEncoding("UTF-8");

    //    Map<String, Object> errorResponse = new HashMap<>();
    //    errorResponse.put("message", message);

    //    ObjectMapper objectMapper = new ObjectMapper();
    //    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    //}

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}