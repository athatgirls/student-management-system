package org.example.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    @org.springframework.beans.factory.annotation.Autowired
    private org.example.service.AccountTokenStateService accountTokenStateService;

    @Value("${jwt.absolute-session-ms:604800000}")
    private long absoluteSessionMs = 604800000L;

    private String credentialStamp(String id, String type) {
        String state = accountTokenStateService.state(id, type);
        if (state == null) throw new IllegalArgumentException("Account unavailable");
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(getKey());
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(state.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private long expirationTime;
    
    @Value("${jwt.refresh-threshold}")
    private long refreshThreshold;
    
    private SecretKey key;

    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
    }
    
    // 初始化密钥
    private SecretKey getKey() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        }
        return key;
    }
    
    /**
     * 生成JWT Token
     * @param userId 用户ID
     * @param userType 用户类型 (student/admin)
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(String userId, String userType, String username) {
        return generateToken(userId, userType, username, System.currentTimeMillis());
    }

    private String generateToken(String userId, String userType, String username, long sessionStart) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("credentialStamp", credentialStamp(userId, userType));
        claims.put("sessionStart", sessionStart);
        claims.put("userId", userId);
        claims.put("userType", userType);
        claims.put("username", username);
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(Math.min(System.currentTimeMillis() + expirationTime, sessionStart + absoluteSessionMs)))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
        
        log.info("生成JWT Token成功，用户ID: {}, 用户类型: {}", userId, userType);
        return token;
    }
    
    /**
     * 验证JWT Token
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            log.debug("开始验证token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token).getBody();
            String stamp = claims.get("credentialStamp", String.class);
            Number started = claims.get("sessionStart", Number.class);
            if (stamp == null || started == null || System.currentTimeMillis() - started.longValue() >= absoluteSessionMs) return false;
            if (!java.security.MessageDigest.isEqual(stamp.getBytes(StandardCharsets.UTF_8),
                    credentialStamp(claims.get("userId", String.class), claims.get("userType", String.class)).getBytes(StandardCharsets.UTF_8))) return false;
            log.debug("Token验证成功");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从Token中获取用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", String.class);
    }
    
    /**
     * 从Token中获取用户类型
     * @param token JWT Token
     * @return 用户类型
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userType", String.class);
    }
    
    /**
     * 从Token中获取用户名
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }
    
    /**
     * 从Token中获取过期时间
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }
    
    /**
     * 检查Token是否即将过期
     * @param token JWT Token
     * @return 是否即将过期
     */
    public boolean isTokenNearExpiration(String token) {
        Date expiration = getExpirationFromToken(token);
        long currentTime = System.currentTimeMillis();
        long expirationTime = expiration.getTime();
        boolean isNearExpiration = (expirationTime - currentTime) < refreshThreshold;
        log.debug("Token即将过期检查: 当前时间={}, 过期时间={}, 是否即将过期={}", 
                 currentTime, expirationTime, isNearExpiration);
        return isNearExpiration;
    }
    
    /**
     * 从Token中获取Claims
     * @param token JWT Token
     * @return Claims对象
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 刷新Token（生成新的Token，保持相同的用户信息）
     * @param token 原Token
     * @return 新的Token
     */
    public String refreshToken(String token) {
        if (!validateToken(token)) throw new IllegalArgumentException("Token no longer valid");
        Claims claims = getClaimsFromToken(token);
        String userId = claims.get("userId", String.class);
        String userType = claims.get("userType", String.class);
        String username = claims.get("username", String.class);
        
        log.info("刷新Token，用户ID: {}, 用户类型: {}", userId, userType);
        return generateToken(userId, userType, username, claims.get("sessionStart", Number.class).longValue());
    }
}
