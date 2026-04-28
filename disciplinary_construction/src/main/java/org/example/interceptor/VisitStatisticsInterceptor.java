package org.example.interceptor;

import org.example.service.VisitStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class VisitStatisticsInterceptor implements HandlerInterceptor {
    
    @Autowired
    private VisitStatisticsService visitStatisticsService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只记录页面级别的访问，不记录每个API调用
        // 这样可以避免刷新页面时访问次数异常增长
        String path = request.getRequestURI();
        
        // 只记录特定的页面访问接口
        boolean isPageVisit = path.contains("/dashboard-stats") || 
                             path.contains("/current-user") ||
                             path.contains("/current-admin") ||
                             path.contains("/profile");
        
        if (isPageVisit && 
            path.startsWith("/SCSE@hbut/msi") && 
            !path.contains("/doc.html") && 
            !path.contains("/swagger") && 
            !path.contains("/webjars") &&
            !path.contains("/v3/api-docs") &&
            !path.contains("/login") &&
            !path.contains("/refresh-token")) {
            // 从请求属性中获取用户信息（JwtAuthenticationFilter已设置）
            String userId = (String) request.getAttribute("userId");
            String userType = (String) request.getAttribute("userType");
            
            // 记录访问（使用用户ID作为去重标识，同一用户同一天只记录一次）
            visitStatisticsService.recordVisit(userId, userType);
        }
        return true;
    }
}
