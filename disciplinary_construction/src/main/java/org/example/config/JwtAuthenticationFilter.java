package org.example.config;

import org.example.service.OnlineUserService;
import org.example.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private OnlineUserService onlineUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();
        
        log.debug("处理请求: {}, Authorization头: {}", requestURI, authHeader != null ? "存在" : "不存在");
        
        // 清除之前的认证信息
        SecurityContextHolder.clearContext();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // 只有当token不为空时才进行验证
            if (token != null && !token.trim().isEmpty()) {
                log.debug("开始验证token，请求路径: {}", requestURI);
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserIdFromToken(token);
                    String userType = jwtUtil.getUserTypeFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    
                    // 创建认证对象
                    String role = "ROLE_" + userType.toUpperCase();
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                    
                    // 设置认证信息到Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // 将用户信息添加到请求属性中，供后续使用
                    request.setAttribute("userId", userId);
                    request.setAttribute("userType", userType);
                    request.setAttribute("username", username);
                    
                    // 记录用户在线（如果Redis可用）
                    if (userId != null && userType != null) {
                        try {
                            onlineUserService.recordUserOnline(userId, userType);
                        } catch (Exception e) {
                            // Redis连接失败不影响JWT认证
                            log.debug("记录用户在线失败（Redis可能未启动）: {}", e.getMessage());
                        }
                    }
                    
                    log.debug("JWT认证成功，用户ID: {}, 用户类型: {}, 请求路径: {}", userId, userType, requestURI);
                } else {
                    log.warn("JWT token验证失败，请求路径: {}", requestURI);
                    // 确保清除认证信息
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("JWT token为空，请求路径: {}", requestURI);
                // 确保清除认证信息
                SecurityContextHolder.clearContext();
            }
        } else {
            log.debug("请求没有Authorization头或格式不正确，请求路径: {}", requestURI);
            // 确保清除认证信息
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
}
