package com.example.projectplanner.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    private final SecretKey key;
    private final long jwtExpiration;

    public JwtProvider(@Value("${jwt.secret}") String jwtSecret,
                       @Value("${jwt.expiration}") long jwtExpiration) {
        byte[] decodedKey = Decoders.BASE64.decode(jwtSecret);
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtExpiration = jwtExpiration;

        logger.info("JWT Expiration set to {} milliseconds", jwtExpiration);
    }

    /**
     * Generates a JWT token for authentication.
     *
     * @param username The username of the authenticated user.
     * @param roles    The roles assigned to the user.
     * @return A signed JWT token.
     */
    public String generateToken(String username, List<String> roles, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("id", userId)
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates a JWT token.
     *
     * @param token The JWT token to validate.
     * @return true if valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.error("JWT Token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT Token: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the username from the JWT token.
     *
     * @param token The JWT token.
     * @return The username.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    /**
     * Extracts user roles from the JWT token.
     *
     * @param token The JWT token.
     * @return A list of roles.
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = extractClaims(token);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(claims.get("roles"), new TypeReference<List<String>>() {});
    }

    /**
     * Extracts user ID from the JWT token.
     *
     * @param token The JWT token.
     * @return The user ID.
     */
    public Long getIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("id", Long.class);  // Get the ID from the token
    }

    /**
     * Extracts claims from a JWT token.
     *
     * @param token The JWT token.
     * @return Claims object containing token data.
     */
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}