package com.campushub.security.jwt;

import com.campushub.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtUtils(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate Token
     * @param userId
     * @param email
     * @param roles
     * @return Token String
     */
    public String generateToken(UUID userId, String email, String roles){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse Token
     * @param token
     * @return Claims
     */
    public Claims parseToken(String token){
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generate RefreshToken (Stored in Redis)
     * @return UUID'
     *
     *      * JWT 的自验证特性（不查数据库就能验证）是优点也是缺点：
     *      * 一旦签发就无法作废（除非等它过期）。
     *      * Refresh token 用随机字符串 + Redis 存储，
     *      * 可以随时从 Redis 删除来作废它（踢人下线）。
     */
    public String generateRefreshToken(){
        return UUID.randomUUID().toString();
    }

    // -- Claim Extraction --
    public UUID extractUserId(Claims claims){
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims){
        return claims.get("email", String.class);
    }

    public String extractRoles(Claims claims){
        return claims.get("roles", String.class);
    }
}
